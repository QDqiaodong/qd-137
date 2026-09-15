package com.example.balloon.controller;

import com.example.balloon.dto.AddReturnItemRequest;
import com.example.balloon.dto.DutyShiftDTO;
import com.example.balloon.dto.HandoffStatusDTO;
import com.example.balloon.dto.ReturnItemDTO;
import com.example.balloon.entity.Operator;
import com.example.balloon.service.OperatorService;
import com.example.balloon.service.ReturnHandoffService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 地勤支架归位交接。所有接口逐请求核对身份（请求头 X-Operator-Code / X-Operator-Password）。
 *
 * 交接纪律：打开归位页先看到上一班还停在场地的支架；只要还有未收回的，
 * 「开始本班」由后端拦截；全部收回后返回 canStartShift=true，页面提示本班可以开始作业。
 */
@RestController
@RequestMapping("/api/returns")
@RequiredArgsConstructor
public class ReturnHandoffController {

    private final ReturnHandoffService handoffService;
    private final OperatorService operatorService;

    /** 归位页状态：进行中本班，或交接间隙的上一班遗留清单 */
    @GetMapping("/handoff")
    public ResponseEntity<HandoffStatusDTO> getHandoff(
            @RequestHeader(value = "X-Operator-Code", required = false) String operatorCode,
            @RequestHeader(value = "X-Operator-Password", required = false) String operatorPassword) {
        operatorService.verifyIdentity(operatorCode, operatorPassword);
        return ResponseEntity.ok(handoffService.getHandoffStatus());
    }

    /** 开始本班：上一班仍有未收回支架时返回 403 */
    @PostMapping("/shifts/start")
    public ResponseEntity<DutyShiftDTO> startShift(
            @RequestHeader(value = "X-Operator-Code", required = false) String operatorCode,
            @RequestHeader(value = "X-Operator-Password", required = false) String operatorPassword) {
        Operator operator = operatorService.verifyIdentity(operatorCode, operatorPassword);
        return ResponseEntity.ok(handoffService.startShift(operator));
    }

    /** 下班交接：本班置为 CLOSED，未收回支架留给下一班先处理 */
    @PostMapping("/shifts/close")
    public ResponseEntity<DutyShiftDTO> closeShift(
            @RequestHeader(value = "X-Operator-Code", required = false) String operatorCode,
            @RequestHeader(value = "X-Operator-Password", required = false) String operatorPassword) {
        Operator operator = operatorService.verifyIdentity(operatorCode, operatorPassword);
        return ResponseEntity.ok(handoffService.closeShift(operator));
    }

    /** 查看某个班组的完整归位清单 */
    @GetMapping("/shifts/{shiftId}/items")
    public ResponseEntity<List<ReturnItemDTO>> listItems(
            @RequestHeader(value = "X-Operator-Code", required = false) String operatorCode,
            @RequestHeader(value = "X-Operator-Password", required = false) String operatorPassword,
            @PathVariable Long shiftId) {
        operatorService.verifyIdentity(operatorCode, operatorPassword);
        return ResponseEntity.ok(handoffService.listItems(shiftId));
    }

    /** 本班进行中：登记一个当天动过的支架（初始为还停在场地） */
    @PostMapping("/items")
    public ResponseEntity<ReturnItemDTO> addItem(
            @RequestHeader(value = "X-Operator-Code", required = false) String operatorCode,
            @RequestHeader(value = "X-Operator-Password", required = false) String operatorPassword,
            @Valid @RequestBody AddReturnItemRequest request) {
        Operator operator = operatorService.verifyIdentity(operatorCode, operatorPassword);
        return ResponseEntity.ok(handoffService.addItem(operator, request));
    }

    /** 标记一条清单的归位状态：RETURNED=已收回停放区 / ON_SITE=还停在场地 */
    @PutMapping("/items/{itemId}/status")
    public ResponseEntity<ReturnItemDTO> markStatus(
            @RequestHeader(value = "X-Operator-Code", required = false) String operatorCode,
            @RequestHeader(value = "X-Operator-Password", required = false) String operatorPassword,
            @PathVariable Long itemId,
            @RequestBody Map<String, String> body) {
        Operator operator = operatorService.verifyIdentity(operatorCode, operatorPassword);
        return ResponseEntity.ok(handoffService.markStatus(operator, itemId, body.get("returnStatus")));
    }
}
