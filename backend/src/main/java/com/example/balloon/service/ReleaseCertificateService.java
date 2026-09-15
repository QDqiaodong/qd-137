package com.example.balloon.service;

import com.example.balloon.dto.CertificateBatchRequest;
import com.example.balloon.dto.CertificateBatchResultDTO;
import com.example.balloon.dto.CertificateRowDTO;
import com.example.balloon.dto.OperatorInfoDTO;
import com.example.balloon.dto.ReleaseCertificateDTO;
import com.example.balloon.entity.Operator;
import com.example.balloon.exception.LedgerLockedException;
import com.example.balloon.repository.OperatorRepository;
import com.example.balloon.repository.ReleaseCertificateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

/**
 * 放飞证照台账。
 *
 * 入口规则：
 * 1. 只有调度（DISPATCHER）能写；放飞员打开页面只读，写接口在后端逐请求拦截。
 * 2. 全局一把写锁（Redis SET NX）：两人同时交批，只许一个成功，另一个收到 409，
 *    并被告知已经有调度在写台账；拿不到锁不做任何落库。
 * 3. 整批校验 + 整批落库在同一个数据库事务里（{@link ReleaseCertificateLedgerTx}），
 *    任一条不合格整批回滚。单条换证也走同一套校验（包装成一条的批次）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReleaseCertificateService {

    /** 台账全局写锁键 */
    private static final String LOCK_KEY = "lock:release-certificate-ledger:write";
    /** 写锁租约：只在事务期间持有，30s 足以兜底进程崩溃，避免死锁 */
    private static final Duration LOCK_TTL = Duration.ofSeconds(30);

    /** 安全释放 Lua：只删自己这把锁（值=本次 token），不替别人解锁 */
    private static final String UNLOCK_SCRIPT =
            "if redis.call('get', KEYS[1]) == ARGV[1] then "
                    + "return redis.call('del', KEYS[1]) else return 0 end";

    private final ReleaseCertificateLedgerTx ledgerTx;
    private final ReleaseCertificateRepository certificateRepository;
    private final OperatorRepository operatorRepository;
    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 批量补录/换证。整批原子：全部成功才落库，否则一条不落（422 带卡住行号与原因）。
     */
    public CertificateBatchResultDTO submitBatch(CertificateBatchRequest request, Operator dispatcher) {
        List<CertificateRowDTO> rows = request.getRows();
        String token = acquireLock(dispatcher);
        try {
            return ledgerTx.saveBatch(rows, dispatcher);
        } finally {
            releaseLock(token);
        }
    }

    /**
     * 单条换证：与批量走完全相同的到期日次序与整批回滚逻辑，
     * 只是把一条包装成单元素批次，不能因为是单条就放行。
     */
    public CertificateBatchResultDTO renewOne(CertificateRowDTO row, Operator dispatcher) {
        CertificateBatchRequest request = CertificateBatchRequest.builder()
                .rows(List.of(row))
                .build();
        return submitBatch(request, dispatcher);
    }

    @Transactional(readOnly = true)
    public List<ReleaseCertificateDTO> listAll() {
        return certificateRepository.findAllByOrderByIdAsc().stream()
                .map(c -> ReleaseCertificateDTO.builder()
                        .id(c.getId())
                        .certificateNo(c.getCertificateNo())
                        .operatorCode(c.getOperatorCode())
                        .operatorName(c.getOperatorName())
                        .issueDate(c.getIssueDate())
                        .expireDate(c.getExpireDate())
                        .batchNo(c.getBatchNo())
                        .createdOperatorCode(c.getCreatedOperatorCode())
                        .createdAt(c.getCreatedAt())
                        .build())
                .toList();
    }

    /** 在册在职人员清单，供单条换证选择，以及补录时核对工号 */
    @Transactional(readOnly = true)
    public List<OperatorInfoDTO> listActiveOperators() {
        return operatorRepository.findAll().stream()
                .filter(o -> "ACTIVE".equals(o.getStatus()))
                .map(o -> OperatorInfoDTO.builder()
                        .operatorCode(o.getOperatorCode())
                        .operatorName(o.getOperatorName())
                        .role(o.getRole())
                        .dutyRouteIds(List.of())
                        .build())
                .toList();
    }

    private String acquireLock(Operator dispatcher) {
        String token = dispatcher.getOperatorCode() + ":" + UUID.randomUUID();
        Boolean acquired = stringRedisTemplate.opsForValue()
                .setIfAbsent(LOCK_KEY, token, LOCK_TTL);
        if (!Boolean.TRUE.equals(acquired)) {
            String holder = stringRedisTemplate.opsForValue().get(LOCK_KEY);
            String holderCode = holder == null ? "其他调度" : holder.split(":")[0];
            log.warn("Certificate ledger write lock busy, dispatcher {} rejected; holder={}",
                    dispatcher.getOperatorCode(), holder);
            throw new LedgerLockedException(
                    "已经有调度在写台账（当前持锁：" + holderCode + "），本批未写入，请稍后重试");
        }
        return token;
    }

    private void releaseLock(String token) {
        try {
            stringRedisTemplate.execute(
                    new DefaultRedisScript<>(UNLOCK_SCRIPT, Long.class),
                    List.of(LOCK_KEY), token);
        } catch (Exception e) {
            // 锁有 TTL 兜底；释放失败仅告警，不影响已提交的事务结果
            log.warn("Failed to release certificate ledger lock, will expire by TTL: {}", e.getMessage());
        }
    }
}
