package com.example.balloon.controller;

import com.example.balloon.dto.BracketDTO;
import com.example.balloon.service.BracketService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/brackets")
@RequiredArgsConstructor
public class BracketController {

    private final BracketService bracketService;

    @PostMapping
    public ResponseEntity<BracketDTO> createBracket(@Valid @RequestBody BracketDTO dto) {
        return ResponseEntity.ok(bracketService.createBracket(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<BracketDTO> updateBracket(@PathVariable Long id, @Valid @RequestBody BracketDTO dto) {
        return ResponseEntity.ok(bracketService.updateBracket(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBracket(@PathVariable Long id) {
        bracketService.deleteBracket(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<BracketDTO> getBracketById(@PathVariable Long id) {
        return ResponseEntity.ok(bracketService.getBracketById(id));
    }

    @GetMapping("/code/{code}")
    public ResponseEntity<BracketDTO> getBracketByCode(@PathVariable String code) {
        return ResponseEntity.ok(bracketService.getBracketByCode(code));
    }

    @GetMapping
    public ResponseEntity<List<BracketDTO>> getAllBrackets() {
        return ResponseEntity.ok(bracketService.getAllBrackets());
    }

    @GetMapping("/type/{type}")
    public ResponseEntity<List<BracketDTO>> getBracketsByType(@PathVariable String type) {
        return ResponseEntity.ok(bracketService.getBracketsByType(type));
    }

    @GetMapping("/types")
    public ResponseEntity<List<String>> getAllTypes() {
        return ResponseEntity.ok(bracketService.getAllTypes());
    }

    @GetMapping("/search")
    public ResponseEntity<List<BracketDTO>> searchBrackets(
            @RequestParam(required = false) Double minWindSpeed,
            @RequestParam(required = false) Double maxWindSpeed) {
        if (minWindSpeed != null && maxWindSpeed != null) {
            return ResponseEntity.ok(bracketService.findByWindRange(minWindSpeed, maxWindSpeed));
        }
        return ResponseEntity.ok(bracketService.getAllBrackets());
    }

    @GetMapping("/suitable")
    public ResponseEntity<List<BracketDTO>> getSuitableBrackets(
            @RequestParam Double minWindSpeed,
            @RequestParam Double maxWindSpeed) {
        return ResponseEntity.ok(bracketService.findSuitableBrackets(minWindSpeed, maxWindSpeed));
    }
}
