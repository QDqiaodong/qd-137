package com.example.balloon.service;

import com.example.balloon.dto.OperatorInfoDTO;
import com.example.balloon.entity.Operator;
import com.example.balloon.exception.AccessDeniedException;
import com.example.balloon.exception.UnauthorizedException;
import com.example.balloon.repository.DutyAssignmentRepository;
import com.example.balloon.repository.OperatorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OperatorService {

    private final OperatorRepository operatorRepository;
    private final DutyAssignmentRepository dutyAssignmentRepository;

    /**
     * 身份核对：工号 + 口令。核对失败一律抛 401，不区分工号不存在还是口令错误。
     */
    public Operator verifyIdentity(String operatorCode, String password) {
        if (!StringUtils.hasText(operatorCode) || !StringUtils.hasText(password)) {
            throw new UnauthorizedException("请先完成身份核对");
        }
        return operatorRepository.findByOperatorCode(operatorCode.trim())
                .filter(o -> "ACTIVE".equals(o.getStatus()))
                .filter(o -> o.getPasswordHash().equals(sha256(password)))
                .orElseThrow(() -> new UnauthorizedException("身份核对失败：工号或口令不正确"));
    }

    /**
     * 改挂（绑定/解绑/试配）仅调度可执行。
     */
    public void requireDispatcher(Operator operator) {
        if (!Operator.ROLE_DISPATCHER.equals(operator.getRole())) {
            log.warn("Operator {} ({}) attempted a dispatch-only operation",
                    operator.getOperatorCode(), operator.getRole());
            throw new AccessDeniedException("仅调度可改挂航线支架，放飞员为只读权限");
        }
    }

    /**
     * 放飞证照台账的补录/换证仅调度可执行；放飞员只读。
     */
    public void requireCertificateWriter(Operator operator) {
        if (!Operator.ROLE_DISPATCHER.equals(operator.getRole())) {
            log.warn("Operator {} ({}) attempted to write the release-certificate ledger",
                    operator.getOperatorCode(), operator.getRole());
            throw new AccessDeniedException("放飞证照台账仅调度可补录/换证，放飞员为只读权限");
        }
    }

    /**
     * 放飞员仅限查看自己当班航线；调度不限航线。
     */
    public void requireRouteVisible(Operator operator, Long routeId) {
        if (Operator.ROLE_DISPATCHER.equals(operator.getRole())) {
            return;
        }
        if (!dutyAssignmentRepository.existsByOperatorIdAndRouteId(operator.getId(), routeId)) {
            log.warn("Operator {} attempted to access route {} outside duty assignment",
                    operator.getOperatorCode(), routeId);
            throw new AccessDeniedException("放飞员只能查看自己当班航线的挂接，不能访问其他航线");
        }
    }

    public boolean isDispatcher(Operator operator) {
        return Operator.ROLE_DISPATCHER.equals(operator.getRole());
    }

    public List<Long> getDutyRouteIds(Long operatorId) {
        return dutyAssignmentRepository.findRouteIdsByOperatorId(operatorId);
    }

    public OperatorInfoDTO toOperatorInfo(Operator operator) {
        return OperatorInfoDTO.builder()
                .operatorCode(operator.getOperatorCode())
                .operatorName(operator.getOperatorName())
                .role(operator.getRole())
                .dutyRouteIds(isDispatcher(operator) ? List.of() : getDutyRouteIds(operator.getId()))
                .build();
    }

    public static String sha256(String raw) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 不可用", e);
        }
    }
}
