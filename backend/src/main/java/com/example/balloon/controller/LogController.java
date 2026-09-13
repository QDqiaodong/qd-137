package com.example.balloon.controller;

import com.example.balloon.entity.WindMatchLog;
import com.example.balloon.service.WindMatchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/logs")
@RequiredArgsConstructor
public class LogController {

    private final WindMatchService windMatchService;

    @GetMapping("/route/{routeId}")
    public ResponseEntity<List<WindMatchLog>> getLogsByRoute(@PathVariable Long routeId) {
        return ResponseEntity.ok(windMatchService.getMatchLogsByRoute(routeId));
    }
}
