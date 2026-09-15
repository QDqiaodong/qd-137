package com.example.balloon.controller;

import com.example.balloon.dto.CertificateBatchRequest;
import com.example.balloon.dto.CertificateBatchResultDTO;
import com.example.balloon.dto.CertificateRowDTO;
import com.example.balloon.dto.OperatorInfoDTO;
import com.example.balloon.dto.ReleaseCertificateDTO;
import com.example.balloon.entity.Operator;
import com.example.balloon.service.OperatorService;
import com.example.balloon.service.ReleaseCertificateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 放飞证照台账。
 *
 * 身份逐请求核对（请求头 X-Operator-Code / X-Operator-Password）：
 * - 查询：调度、放飞员均可（放飞员只读，仅用于查看证照台账）。
 * - 补录批量 / 单条换证：仅调度，放飞员调用返回 403。
 *
 * 写入整批原子（任一条不合格整批回滚，422 带卡住行号与原因），
 * 并用全局写锁保证两人同时交批只成功一批（另一批 409）。
 */
@RestController
@RequestMapping("/api/certificates")
@RequiredArgsConstructor
public class ReleaseCertificateController {

    private final ReleaseCertificateService certificateService;
    private final OperatorService operatorService;

    /** 台账全量（放飞员只读也能看） */
    @GetMapping
    public ResponseEntity<List<ReleaseCertificateDTO>> list(
            @RequestHeader(value = "X-Operator-Code", required = false) String operatorCode,
            @RequestHeader(value = "X-Operator-Password", required = false) String operatorPassword) {
        operatorService.verifyIdentity(operatorCode, operatorPassword);
        return ResponseEntity.ok(certificateService.listAll());
    }

    /** 在册在职人员清单（单条换证选人、补录核对工号用） */
    @GetMapping("/operators")
    public ResponseEntity<List<OperatorInfoDTO>> listOperators(
            @RequestHeader(value = "X-Operator-Code", required = false) String operatorCode,
            @RequestHeader(value = "X-Operator-Password", required = false) String operatorPassword) {
        operatorService.verifyIdentity(operatorCode, operatorPassword);
        return ResponseEntity.ok(certificateService.listActiveOperators());
    }

    /** 批量补录历年散落证照 + 在册人员换证：整批要么全落，要么一条不落 */
    @PostMapping("/batch")
    public ResponseEntity<CertificateBatchResultDTO> submitBatch(
            @RequestHeader(value = "X-Operator-Code", required = false) String operatorCode,
            @RequestHeader(value = "X-Operator-Password", required = false) String operatorPassword,
            @Valid @RequestBody CertificateBatchRequest request) {
        Operator dispatcher = operatorService.verifyIdentity(operatorCode, operatorPassword);
        operatorService.requireCertificateWriter(dispatcher);
        return ResponseEntity.ok(certificateService.submitBatch(request, dispatcher));
    }

    /** 单条换证：与批量同一套到期日次序与整批回滚校验 */
    @PostMapping("/renew")
    public ResponseEntity<CertificateBatchResultDTO> renewOne(
            @RequestHeader(value = "X-Operator-Code", required = false) String operatorCode,
            @RequestHeader(value = "X-Operator-Password", required = false) String operatorPassword,
            @Valid @RequestBody CertificateRowDTO row) {
        Operator dispatcher = operatorService.verifyIdentity(operatorCode, operatorPassword);
        operatorService.requireCertificateWriter(dispatcher);
        return ResponseEntity.ok(certificateService.renewOne(row, dispatcher));
    }
}
