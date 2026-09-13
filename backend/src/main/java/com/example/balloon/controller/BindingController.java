package com.example.balloon.controller;

import com.example.balloon.dto.BindingDTO;
import com.example.balloon.dto.WindMatchResult;
import com.example.balloon.service.RouteBracketBindingService;
import com.example.balloon.service.WindMatchService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/bindings")
@RequiredArgsConstructor
public class BindingController {

    private final RouteBracketBindingService bindingService;
    private final WindMatchService windMatchService;

    @PostMapping
    public ResponseEntity<WindMatchResult> createBinding(@Valid @RequestBody BindingDTO dto) {
        return ResponseEntity.ok(bindingService.bindBracketToRoute(dto));
    }

    @DeleteMapping("/route/{routeId}/bracket/{bracketId}")
    public ResponseEntity<Void> deleteBinding(@PathVariable Long routeId, @PathVariable Long bracketId) {
        bindingService.unbindBracketFromRoute(routeId, bracketId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/route/{routeId}")
    public ResponseEntity<List<Map<String, Object>>> getBindingsByRoute(@PathVariable Long routeId) {
        return ResponseEntity.ok(bindingService.getBindingsByRoute(routeId));
    }

    @GetMapping("/bracket/{bracketId}")
    public ResponseEntity<List<Map<String, Object>>> getBindingsByBracket(@PathVariable Long bracketId) {
        return ResponseEntity.ok(bindingService.getBindingsByBracket(bracketId));
    }

    @GetMapping("/route/{routeId}/count")
    public ResponseEntity<Map<String, Long>> countBindings(@PathVariable Long routeId) {
        return ResponseEntity.ok(Map.of("count", bindingService.countBindingsByRoute(routeId)));
    }

    @GetMapping("/check-match")
    public ResponseEntity<WindMatchResult> checkMatch(
            @RequestParam Long routeId,
            @RequestParam Long bracketId) {
        return ResponseEntity.ok(windMatchService.checkWindMatch(routeId, bracketId));
    }
}
