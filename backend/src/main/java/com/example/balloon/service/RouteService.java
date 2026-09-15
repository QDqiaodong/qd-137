package com.example.balloon.service;

import com.example.balloon.dto.RouteDTO;
import com.example.balloon.entity.Route;
import com.example.balloon.repository.RouteRepository;
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
public class RouteService {

    private final RouteRepository routeRepository;

    @Transactional
    public RouteDTO createRoute(RouteDTO dto) {
        validateWindRange(dto.getMinWindSpeed(), dto.getMaxWindSpeed());
        
        Route route = Route.builder()
                .routeCode(dto.getRouteCode())
                .routeName(dto.getRouteName())
                .groupName(dto.getGroupName())
                .minWindSpeed(dto.getMinWindSpeed())
                .maxWindSpeed(dto.getMaxWindSpeed())
                .distance(dto.getDistance())
                .duration(dto.getDuration())
                .difficultyLevel(dto.getDifficultyLevel())
                .status(dto.getStatus() != null ? dto.getStatus() : "ACTIVE")
                .description(dto.getDescription())
                .build();
        
        Route saved = routeRepository.save(route);
        log.info("Created route: {}", saved.getRouteCode());
        return convertToDTO(saved);
    }

    @Transactional
    public RouteDTO updateRoute(Long id, RouteDTO dto) {
        Route route = routeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("航线不存在: " + id));
        
        if (dto.getMinWindSpeed() != null && dto.getMaxWindSpeed() != null) {
            validateWindRange(dto.getMinWindSpeed(), dto.getMaxWindSpeed());
        }
        
        if (dto.getRouteCode() != null) route.setRouteCode(dto.getRouteCode());
        if (dto.getRouteName() != null) route.setRouteName(dto.getRouteName());
        if (dto.getGroupName() != null) route.setGroupName(dto.getGroupName());
        if (dto.getMinWindSpeed() != null) route.setMinWindSpeed(dto.getMinWindSpeed());
        if (dto.getMaxWindSpeed() != null) route.setMaxWindSpeed(dto.getMaxWindSpeed());
        if (dto.getDistance() != null) route.setDistance(dto.getDistance());
        if (dto.getDuration() != null) route.setDuration(dto.getDuration());
        if (dto.getDifficultyLevel() != null) route.setDifficultyLevel(dto.getDifficultyLevel());
        if (dto.getStatus() != null) route.setStatus(dto.getStatus());
        if (dto.getDescription() != null) route.setDescription(dto.getDescription());
        route.setUpdatedAt(LocalDateTime.now());
        
        Route saved = routeRepository.save(route);
        log.info("Updated route: {}", saved.getRouteCode());
        return convertToDTO(saved);
    }

    @Transactional
    public void deleteRoute(Long id) {
        Route route = routeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("航线不存在: " + id));
        route.setStatus("DELETED");
        route.setUpdatedAt(LocalDateTime.now());
        routeRepository.save(route);
        log.info("Deleted route: {}", route.getRouteCode());
    }

    public RouteDTO getRouteById(Long id) {
        Route route = routeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("航线不存在: " + id));
        return convertToDTO(route);
    }

    public RouteDTO getRouteByCode(String code) {
        Route route = routeRepository.findByRouteCode(code)
                .orElseThrow(() -> new EntityNotFoundException("航线不存在: " + code));
        return convertToDTO(route);
    }

    public List<RouteDTO> getAllRoutes() {
        return routeRepository.findByStatus("ACTIVE").stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<RouteDTO> getRoutesByGroup(String groupName) {
        return routeRepository.findByGroupNameAndStatus(groupName, "ACTIVE").stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<String> getAllGroups() {
        return routeRepository.findDistinctGroupNames();
    }

    private void validateWindRange(Double minWind, Double maxWind) {
        if (minWind > maxWind) {
            throw new IllegalArgumentException("最小风力不能大于最大风力");
        }
        if (minWind < 0) {
            throw new IllegalArgumentException("最小风力不能为负数");
        }
    }

    private RouteDTO convertToDTO(Route entity) {
        return RouteDTO.builder()
                .id(entity.getId())
                .routeCode(entity.getRouteCode())
                .routeName(entity.getRouteName())
                .groupName(entity.getGroupName())
                .minWindSpeed(entity.getMinWindSpeed())
                .maxWindSpeed(entity.getMaxWindSpeed())
                .distance(entity.getDistance())
                .duration(entity.getDuration())
                .difficultyLevel(entity.getDifficultyLevel())
                .status(entity.getStatus())
                .description(entity.getDescription())
                .build();
    }
}
