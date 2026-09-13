package com.example.balloon.controller;

import com.example.balloon.dto.RouteDTO;
import com.example.balloon.dto.WindMatchResult;
import com.example.balloon.service.RouteBracketBindingService;
import com.example.balloon.service.RouteService;
import com.example.balloon.service.WindMatchService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/routes")
@RequiredArgsConstructor
public class RouteController {

    private final RouteService routeService;
    private final WindMatchService windMatchService;
    private final RouteBracketBindingService bindingService;

    @PostMapping
    public ResponseEntity<RouteDTO> createRoute(@Valid @RequestBody RouteDTO dto) {
        return ResponseEntity.ok(routeService.createRoute(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<RouteDTO> updateRoute(@PathVariable Long id, @Valid @RequestBody RouteDTO dto) {
        RouteDTO updated = routeService.updateRoute(id, dto);
        bindingService.rematchBindingsForRoute(id);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRoute(@PathVariable Long id) {
        routeService.deleteRoute(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<RouteDTO> getRouteById(@PathVariable Long id) {
        return ResponseEntity.ok(routeService.getRouteById(id));
    }

    @GetMapping("/code/{code}")
    public ResponseEntity<RouteDTO> getRouteByCode(@PathVariable String code) {
        return ResponseEntity.ok(routeService.getRouteByCode(code));
    }

    @GetMapping
    public ResponseEntity<List<RouteDTO>> getAllRoutes() {
        return ResponseEntity.ok(routeService.getAllRoutes());
    }

    @GetMapping("/group/{groupName}")
    public ResponseEntity<List<RouteDTO>> getRoutesByGroup(@PathVariable String groupName) {
        return ResponseEntity.ok(routeService.getRoutesByGroup(groupName));
    }

    @GetMapping("/groups")
    public ResponseEntity<List<String>> getAllGroups() {
        return ResponseEntity.ok(routeService.getAllGroups());
    }

    @GetMapping("/{id}/suitable-brackets")
    public ResponseEntity<List<com.example.balloon.dto.BracketDTO>> getSuitableBrackets(@PathVariable Long id) {
        return ResponseEntity.ok(windMatchService.findSuitableBracketsForRoute(id));
    }

    @GetMapping("/{id}/match-result")
    public ResponseEntity<WindMatchResult> rematchBrackets(@PathVariable Long id) {
        return ResponseEntity.ok(windMatchService.rematchBracketsForRoute(id));
    }
}
