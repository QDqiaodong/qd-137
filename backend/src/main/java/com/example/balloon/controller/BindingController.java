package com.example.balloon.controller;

import com.example.balloon.dto.BindingDTO;
import com.example.balloon.dto.WindMatchResult;
import com.example.balloon.entity.Operator;
import com.example.balloon.service.OperatorService;
import com.example.balloon.service.RouteBracketBindingService;
import com.example.balloon.service.WindMatchService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 航线-支架挂接。所有接口先做身份核对（请求头携带工号与口令）：
 * 调度可改挂任意航线；放飞员只读，且仅限自己当班航线。
 */
@RestController
@RequestMapping("/api/bindings")
@RequiredArgsConstructor
public class BindingController {

    private final RouteBracketBindingService bindingService;
    private final WindMatchService windMatchService;
    private final OperatorService operatorService;

    @PostMapping
    public ResponseEntity<WindMatchResult> createBinding(
            @RequestHeader(value = "X-Operator-Code", required = false) String operatorCode,
            @RequestHeader(value = "X-Operator-Password", required = false) String operatorPassword,
            @Valid @RequestBody BindingDTO dto) {
        Operator operator = operatorService.verifyIdentity(operatorCode, operatorPassword);
        operatorService.requireDispatcher(operator);
        return ResponseEntity.ok(bindingService.bindBracketToRoute(dto));
    }

    @DeleteMapping("/route/{routeId}/bracket/{bracketId}")
    public ResponseEntity<Void> deleteBinding(
            @RequestHeader(value = "X-Operator-Code", required = false) String operatorCode,
            @RequestHeader(value = "X-Operator-Password", required = false) String operatorPassword,
            @PathVariable Long routeId, @PathVariable Long bracketId) {
        Operator operator = operatorService.verifyIdentity(operatorCode, operatorPassword);
        operatorService.requireDispatcher(operator);
        bindingService.unbindBracketFromRoute(routeId, bracketId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/route/{routeId}")
    public ResponseEntity<List<Map<String, Object>>> getBindingsByRoute(
            @RequestHeader(value = "X-Operator-Code", required = false) String operatorCode,
            @RequestHeader(value = "X-Operator-Password", required = false) String operatorPassword,
            @PathVariable Long routeId) {
        Operator operator = operatorService.verifyIdentity(operatorCode, operatorPassword);
        operatorService.requireRouteVisible(operator, routeId);
        return ResponseEntity.ok(bindingService.getBindingsByRoute(routeId));
    }

    @GetMapping("/bracket/{bracketId}")
    public ResponseEntity<List<Map<String, Object>>> getBindingsByBracket(
            @RequestHeader(value = "X-Operator-Code", required = false) String operatorCode,
            @RequestHeader(value = "X-Operator-Password", required = false) String operatorPassword,
            @PathVariable Long bracketId) {
        Operator operator = operatorService.verifyIdentity(operatorCode, operatorPassword);
        List<Map<String, Object>> bindings = bindingService.getBindingsByBracket(bracketId);
        if (!operatorService.isDispatcher(operator)) {
            List<Long> dutyRouteIds = operatorService.getDutyRouteIds(operator.getId());
            bindings = bindings.stream()
                    .filter(b -> dutyRouteIds.contains(((Number) b.get("routeId")).longValue()))
                    .toList();
        }
        return ResponseEntity.ok(bindings);
    }

    @GetMapping("/route/{routeId}/count")
    public ResponseEntity<Map<String, Long>> countBindings(
            @RequestHeader(value = "X-Operator-Code", required = false) String operatorCode,
            @RequestHeader(value = "X-Operator-Password", required = false) String operatorPassword,
            @PathVariable Long routeId) {
        Operator operator = operatorService.verifyIdentity(operatorCode, operatorPassword);
        operatorService.requireRouteVisible(operator, routeId);
        return ResponseEntity.ok(Map.of("count", bindingService.countBindingsByRoute(routeId)));
    }

    @GetMapping("/check-match")
    public ResponseEntity<WindMatchResult> checkMatch(
            @RequestHeader(value = "X-Operator-Code", required = false) String operatorCode,
            @RequestHeader(value = "X-Operator-Password", required = false) String operatorPassword,
            @RequestParam Long routeId,
            @RequestParam Long bracketId,
            @RequestParam(required = false) Double launchWindSpeed) {
        Operator operator = operatorService.verifyIdentity(operatorCode, operatorPassword);
        operatorService.requireDispatcher(operator);
        return ResponseEntity.ok(windMatchService.checkWindMatch(routeId, bracketId, launchWindSpeed));
    }
}
