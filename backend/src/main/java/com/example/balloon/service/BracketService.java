package com.example.balloon.service;

import com.example.balloon.cache.BracketCacheService;
import com.example.balloon.dto.BracketDTO;
import com.example.balloon.entity.Bracket;
import com.example.balloon.repository.BracketRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BracketService {

    private final BracketRepository bracketRepository;
    private final BracketCacheService bracketCacheService;

    @Transactional
    public BracketDTO createBracket(BracketDTO dto) {
        validateWindRange(dto.getMinWindSpeed(), dto.getMaxWindSpeed());
        
        Bracket bracket = Bracket.builder()
                .bracketCode(dto.getBracketCode())
                .bracketName(dto.getBracketName())
                .maxLoad(dto.getMaxLoad())
                .minWindSpeed(dto.getMinWindSpeed())
                .maxWindSpeed(dto.getMaxWindSpeed())
                .bracketType(dto.getBracketType())
                .status(dto.getStatus() != null ? dto.getStatus() : "ACTIVE")
                .description(dto.getDescription())
                .build();
        
        Bracket saved = bracketRepository.save(bracket);
        bracketCacheService.cacheBracket(saved);
        log.info("Created bracket: {}", saved.getBracketCode());
        return convertToDTO(saved);
    }

    @Transactional
    public BracketDTO updateBracket(Long id, BracketDTO dto) {
        Bracket bracket = bracketRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("支架不存在: " + id));
        
        if (dto.getMinWindSpeed() != null && dto.getMaxWindSpeed() != null) {
            validateWindRange(dto.getMinWindSpeed(), dto.getMaxWindSpeed());
        }
        
        if (dto.getBracketCode() != null) bracket.setBracketCode(dto.getBracketCode());
        if (dto.getBracketName() != null) bracket.setBracketName(dto.getBracketName());
        if (dto.getMaxLoad() != null) bracket.setMaxLoad(dto.getMaxLoad());
        if (dto.getMinWindSpeed() != null) bracket.setMinWindSpeed(dto.getMinWindSpeed());
        if (dto.getMaxWindSpeed() != null) bracket.setMaxWindSpeed(dto.getMaxWindSpeed());
        if (dto.getBracketType() != null) bracket.setBracketType(dto.getBracketType());
        if (dto.getStatus() != null) bracket.setStatus(dto.getStatus());
        if (dto.getDescription() != null) bracket.setDescription(dto.getDescription());
        bracket.setUpdatedAt(LocalDateTime.now());
        
        Bracket saved = bracketRepository.save(bracket);
        bracketCacheService.cacheBracket(saved);
        log.info("Updated bracket: {}", saved.getBracketCode());
        return convertToDTO(saved);
    }

    @Transactional
    public void deleteBracket(Long id) {
        Bracket bracket = bracketRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("支架不存在: " + id));
        bracket.setStatus("DELETED");
        bracket.setUpdatedAt(LocalDateTime.now());
        bracketRepository.save(bracket);
        bracketCacheService.removeBracketFromCache(id);
        log.info("Deleted bracket: {}", bracket.getBracketCode());
    }

    public BracketDTO getBracketById(Long id) {
        Bracket bracket = bracketRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("支架不存在: " + id));
        return convertToDTO(bracket);
    }

    public BracketDTO getBracketByCode(String code) {
        Bracket bracket = bracketRepository.findByBracketCode(code)
                .orElseThrow(() -> new EntityNotFoundException("支架不存在: " + code));
        return convertToDTO(bracket);
    }

    public List<BracketDTO> getAllBrackets() {
        return bracketRepository.findByStatus("ACTIVE").stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<BracketDTO> getBracketsByType(String type) {
        return bracketRepository.findByBracketType(type).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<BracketDTO> findSuitableBrackets(Double minWindSpeed, Double maxWindSpeed) {
        return bracketRepository.findSuitableBrackets(minWindSpeed, maxWindSpeed).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<BracketDTO> findByWindRange(Double minWind, Double maxWind) {
        return bracketRepository.findByWindRange(minWind, maxWind).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<String> getAllTypes() {
        return bracketRepository.findDistinctBracketTypes();
    }

    private void validateWindRange(Double minWind, Double maxWind) {
        if (minWind > maxWind) {
            throw new IllegalArgumentException("最小风力不能大于最大风力");
        }
        if (minWind < 0) {
            throw new IllegalArgumentException("最小风力不能为负数");
        }
    }

    private BracketDTO convertToDTO(Bracket entity) {
        return BracketDTO.builder()
                .id(entity.getId())
                .bracketCode(entity.getBracketCode())
                .bracketName(entity.getBracketName())
                .maxLoad(entity.getMaxLoad())
                .minWindSpeed(entity.getMinWindSpeed())
                .maxWindSpeed(entity.getMaxWindSpeed())
                .bracketType(entity.getBracketType())
                .status(entity.getStatus())
                .description(entity.getDescription())
                .build();
    }
}
