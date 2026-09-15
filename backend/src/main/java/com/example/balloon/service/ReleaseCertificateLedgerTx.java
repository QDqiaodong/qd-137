package com.example.balloon.service;

import com.example.balloon.dto.BatchRejectedDTO;
import com.example.balloon.dto.CertificateBatchResultDTO;
import com.example.balloon.dto.CertificateRowDTO;
import com.example.balloon.entity.Operator;
import com.example.balloon.entity.ReleaseCertificate;
import com.example.balloon.exception.BatchRejectedException;
import com.example.balloon.repository.OperatorRepository;
import com.example.balloon.repository.ReleaseCertificateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 放飞证照台账的事务内落库逻辑：整批校验 + 整批落库必须在同一个数据库事务里，
 * 任一条不合格即抛 {@link BatchRejectedException}，事务回滚，一条都不落库。
 *
 * 写台账全程由 {@link ReleaseCertificateService} 持全局 Redis 写锁串行化，
 * 因此这里看到的「库里现状 + 本批前序行」在写入期间不会被别人改动。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReleaseCertificateLedgerTx {

    /** 严格 yyyy-MM-dd，拒绝 2026-1-9、20260230 这类填反/非法日期 */
    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("uuuu-MM-dd").withResolverStyle(ResolverStyle.STRICT);

    private final ReleaseCertificateRepository certificateRepository;
    private final OperatorRepository operatorRepository;

    @Transactional(propagation = Propagation.REQUIRED)
    public CertificateBatchResultDTO saveBatch(List<CertificateRowDTO> rows, Operator dispatcher) {
        List<ParsedRow> parsed = new ArrayList<>(rows.size());
        for (int i = 0; i < rows.size(); i++) {
            parsed.add(new ParsedRow(i + 1, rows.get(i)));
        }

        // 每个人「库里现有最晚到期日」；换证次序的基线，随本批前序行不断后延
        Map<String, LocalDate> latestExpireByOperator = loadLatestExpireBaselines();
        // 证号 -> 本批第一次出现的行号
        Map<String, Integer> certNoRowInBatch = new HashMap<>();

        List<ReleaseCertificate> toSave = new ArrayList<>(parsed.size());
        String batchNo = nextBatchNo();

        for (ParsedRow p : parsed) {
            validate(p, latestExpireByOperator, certNoRowInBatch, toSave, batchNo, dispatcher);
        }

        certificateRepository.saveAll(toSave);
        log.info("Certificate batch {} saved {} row(s) by dispatcher {}",
                batchNo, toSave.size(), dispatcher.getOperatorCode());
        return CertificateBatchResultDTO.builder()
                .batchNo(batchNo)
                .savedCount(toSave.size())
                .message("本批 " + toSave.size() + " 条放飞证照已全部登记入台账（批次 " + batchNo + "）")
                .build();
    }

    private void validate(ParsedRow p,
                          Map<String, LocalDate> latestExpireByOperator,
                          Map<String, Integer> certNoRowInBatch,
                          List<ReleaseCertificate> toSave,
                          String batchNo,
                          Operator dispatcher) {
        CertificateRowDTO row = p.row;
        int n = p.rowNumber;
        String operatorCode = trim(row.getOperatorCode());
        String certificateNo = trim(row.getCertificateNo());
        String issueText = trim(row.getIssueDate());
        String expireText = trim(row.getExpireDate());

        if (!StringUtils.hasText(operatorCode)) {
            reject(p, "OPERATOR_NOT_FOUND", "第 " + n + " 条未填写工号");
        }
        if (!StringUtils.hasText(certificateNo)) {
            reject(p, "CERT_NO_EMPTY", "第 " + n + " 条未填写证号");
        }

        // 日期：先保证能按 yyyy-MM-dd 严格解析，再比较先后，否则「填反」无从判断
        LocalDate issueDate = parseDate(issueText, p, "发证日期");
        LocalDate expireDate = parseDate(expireText, p, "到期日期");
        if (!issueDate.isBefore(expireDate)) {
            reject(p, "DATE_ORDER",
                    "第 " + n + " 条发证日期（" + issueText + "）不早于到期日期（" + expireText
                            + "），日期填反或相同");
        }

        // 工号必须对得上在册且在职的人
        Operator operator = operatorRepository.findByOperatorCode(operatorCode).orElse(null);
        if (operator == null || !"ACTIVE".equals(operator.getStatus())) {
            reject(p, "OPERATOR_NOT_FOUND", "第 " + n + " 条工号 " + operatorCode + " 对不上在册人员");
        }

        // 证号不能与库里已有记录撞号；撞了要写出目前是谁、工号多少在用
        certificateRepository.findByCertificateNo(certificateNo).ifPresent(existing -> reject(p,
                "CERT_NO_DUPLICATE_IN_DB",
                "第 " + n + " 条证号 " + certificateNo + " 与台账已有记录撞号，目前由 "
                        + existing.getOperatorName() + "（工号 " + existing.getOperatorCode() + "）在用",
                existing.getOperatorName(), existing.getOperatorCode()));

        // 证号也不能与本批另一条撞号
        Integer earlierRow = certNoRowInBatch.get(certificateNo);
        if (earlierRow != null) {
            reject(p, "CERT_NO_DUPLICATE_IN_BATCH",
                    "第 " + n + " 条证号 " + certificateNo + " 与本批第 " + earlierRow + " 条撞号");
        }
        certNoRowInBatch.put(certificateNo, n);

        // 换证次序：新证到期日必须严格晚于这个人现在手里到期日最晚的那张
        // （库里基线；同一人本批前序换证行已把基线顺延）
        LocalDate baseline = latestExpireByOperator.get(operatorCode);
        if (baseline != null && !expireDate.isAfter(baseline)) {
            reject(p, "RENEWAL_ORDER",
                    "第 " + n + " 条是 " + operator.getOperatorName() + "（" + operatorCode
                            + "）的换证，新证到期日 " + expireText + " 没有严格晚于其现有最晚到期日 "
                            + baseline + "，不能换证");
        }
        latestExpireByOperator.put(operatorCode, expireDate);

        toSave.add(ReleaseCertificate.builder()
                .certificateNo(certificateNo)
                .operatorCode(operator.getOperatorCode())
                .operatorName(operator.getOperatorName())
                .issueDate(issueDate)
                .expireDate(expireDate)
                .batchNo(batchNo)
                .createdOperatorCode(dispatcher.getOperatorCode())
                .build());
    }

    private LocalDate parseDate(String text, ParsedRow p, String fieldLabel) {
        if (!StringUtils.hasText(text)) {
            reject(p, "DATE_FORMAT", "第 " + p.rowNumber + " 条" + fieldLabel + "未填写，应为 yyyy-MM-dd");
        }
        try {
            return LocalDate.parse(text, DATE_FORMATTER);
        } catch (DateTimeParseException e) {
            reject(p, "DATE_FORMAT",
                    "第 " + p.rowNumber + " 条" + fieldLabel + "「" + text + "」不是合法的 yyyy-MM-dd 日期");
            return null; // 不可达，reject 必抛异常
        }
    }

    private void reject(ParsedRow p, String conflictType, String reason) {
        reject(p, conflictType, reason, null, null);
    }

    private void reject(ParsedRow p, String conflictType, String reason,
                        String currentHolderName, String currentHolderCode) {
        log.warn("Certificate batch rejected at row {} ({}): {}", p.rowNumber, conflictType, reason);
        throw new BatchRejectedException(BatchRejectedDTO.builder()
                .rowNumber(p.rowNumber)
                .row(p.row)
                .reason(reason)
                .conflictType(conflictType)
                .currentHolderName(currentHolderName)
                .currentHolderCode(currentHolderCode)
                .build());
    }

    /** 每个在册持证人现有证里最晚的到期日；无证人员不在 Map 中（首证不受换证次序限制） */
    private Map<String, LocalDate> loadLatestExpireBaselines() {
        Map<String, LocalDate> baselines = new HashMap<>();
        for (ReleaseCertificate c : certificateRepository.findAll()) {
            baselines.merge(c.getOperatorCode(), c.getExpireDate(),
                    (a, b) -> a.isAfter(b) ? a : b);
        }
        return baselines;
    }

    /**
     * 批次号：CERT-BATCH-yyyyMMdd-当日序号（三位）。
     */
    private String nextBatchNo() {
        String prefix = "CERT-BATCH-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + "-";
        long seq = certificateRepository.findAll().stream()
                .map(ReleaseCertificate::getBatchNo)
                .filter(code -> code != null && code.startsWith(prefix))
                .mapToLong(code -> {
                    try {
                        return Long.parseLong(code.substring(prefix.length()));
                    } catch (NumberFormatException e) {
                        return 0;
                    }
                })
                .max().orElse(0) + 1;
        return prefix + String.format("%03d", seq);
    }

    private static String trim(String s) {
        return s == null ? "" : s.trim();
    }

    /** 解析阶段的行包装：行号（从 1 开始）+ 原始行 */
    private static final class ParsedRow {
        final int rowNumber;
        final CertificateRowDTO row;

        ParsedRow(int rowNumber, CertificateRowDTO row) {
            this.rowNumber = rowNumber;
            this.row = row == null ? new CertificateRowDTO("", "", "", "") : row;
        }
    }
}
