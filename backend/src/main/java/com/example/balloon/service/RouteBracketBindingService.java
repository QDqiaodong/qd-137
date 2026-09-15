package com.example.balloon.service;

import com.example.balloon.dto.BindingDTO;
import com.example.balloon.dto.WindMatchResult;
import com.example.balloon.entity.Bracket;
import com.example.balloon.entity.Route;
import com.example.balloon.entity.RouteBracketBinding;
import com.example.balloon.repository.BracketRepository;
import com.example.balloon.repository.RouteBracketBindingRepository;
import com.example.balloon.repository.RouteRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RouteBracketBindingService {

    private final RouteBracketBindingRepository bindingRepository;
    private final RouteRepository routeRepository;
    private final BracketRepository bracketRepository;
    private final WindMatchService windMatchService;

    @Transactional
    public WindMatchResult bindBracketToRoute(BindingDTO dto) {
        Route route = routeRepository.findById(dto.getRouteId())
                .orElseThrow(() -> new EntityNotFoundException("航线不存在: " + dto.getRouteId()));
        if (!"ACTIVE".equals(route.getStatus())) {
            throw new IllegalArgumentException("航线已删除，不能绑定支架: " + route.getRouteCode());
        }
        Bracket bracket = bracketRepository.findById(dto.getBracketId())
                .orElseThrow(() -> new EntityNotFoundException("支架不存在: " + dto.getBracketId()));

        Double launchWindSpeed = dto.getLaunchWindSpeed();
        String matchLevel = windMatchService.calculateMatchLevel(
                route.getMinWindSpeed(), route.getMaxWindSpeed(), bracket);
        String windStatus = windMatchService.evaluateWindStatus(launchWindSpeed, route, bracket);
        boolean completed = "MATCHED".equals(windStatus);
        String message = windMatchService.buildWindCheckMessage(launchWindSpeed, route, bracket);

        Optional<RouteBracketBinding> existing =
                bindingRepository.findByRouteIdAndBracketId(dto.getRouteId(), dto.getBracketId());

        RouteBracketBinding binding;
        if (existing.isPresent()) {
            binding = existing.get();
            if ("ACTIVE".equals(binding.getStatus())) {
                throw new IllegalArgumentException("该航线与支架已绑定");
            }
            binding.setMatchLevel(matchLevel);
            binding.setLaunchWindSpeed(launchWindSpeed);
            binding.setWindStatus(windStatus);
            binding.setStatus(completed ? "ACTIVE" : "MISMATCH");
            binding.setUpdatedAt(LocalDateTime.now());
        } else {
            binding = RouteBracketBinding.builder()
                    .route(route)
                    .bracket(bracket)
                    .matchLevel(matchLevel)
                    .launchWindSpeed(launchWindSpeed)
                    .windStatus(windStatus)
                    .status(completed ? "ACTIVE" : "MISMATCH")
                    .build();
        }
        bindingRepository.save(binding);

        windMatchService.saveMatchLog(route.getId(), route.getRouteCode(),
                bracket.getId(), bracket.getBracketCode(),
                route.getMinWindSpeed(), route.getMaxWindSpeed(),
                route.getMinWindSpeed(), route.getMaxWindSpeed(),
                "BIND", windStatus,
                "绑定支架: " + bracket.getBracketCode() + " 到航线: " + route.getRouteCode()
                        + "，" + message);

        log.info("Bind bracket {} to route {}: windStatus={}, launchWindSpeed={}",
                bracket.getBracketCode(), route.getRouteCode(), windStatus, launchWindSpeed);

        return WindMatchResult.builder()
                .matched(completed)
                .matchLevel(matchLevel)
                .windStatus(windStatus)
                .launchWindSpeed(launchWindSpeed)
                .message(message)
                .build();
    }

    @Transactional
    public void unbindBracketFromRoute(Long routeId, Long bracketId) {
        RouteBracketBinding binding = bindingRepository.findByRouteIdAndBracketId(routeId, bracketId)
                .orElseThrow(() -> new EntityNotFoundException("绑定关系不存在"));
        
        binding.setStatus("DELETED");
        binding.setUpdatedAt(LocalDateTime.now());
        bindingRepository.save(binding);
        
        log.info("Unbound bracket {} from route {}", bracketId, routeId);
    }

    @Transactional
    public void rematchBindingsForRoute(Long routeId) {
        Route route = routeRepository.findById(routeId)
                .orElseThrow(() -> new EntityNotFoundException("航线不存在: " + routeId));

        List<RouteBracketBinding> activeBindings = bindingRepository.findActiveByRouteId(routeId);
        
        for (RouteBracketBinding binding : activeBindings) {
            String matchLevel = windMatchService.calculateMatchLevel(
                    route.getMinWindSpeed(), route.getMaxWindSpeed(), binding.getBracket());
            
            if ("NONE".equals(matchLevel)) {
                binding.setStatus("DELETED");
                binding.setUpdatedAt(LocalDateTime.now());
                bindingRepository.save(binding);
                
                windMatchService.saveMatchLog(route.getId(), route.getRouteCode(),
                        binding.getBracket().getId(), binding.getBracket().getBracketCode(),
                        route.getMinWindSpeed(), route.getMaxWindSpeed(),
                        route.getMinWindSpeed(), route.getMaxWindSpeed(),
                        "UNBIND", "NO_MATCH",
                        "航线风力参数更新后，支架: " + binding.getBracket().getBracketCode() + " 不再适配，已解绑");
            } else {
                String windStatus = windMatchService.evaluateWindStatus(
                        binding.getLaunchWindSpeed(), route, binding.getBracket());
                binding.setMatchLevel(matchLevel);
                binding.setWindStatus(windStatus);
                binding.setStatus("MATCHED".equals(windStatus) ? "ACTIVE" : "MISMATCH");
                binding.setUpdatedAt(LocalDateTime.now());
                bindingRepository.save(binding);
            }
        }
        
        log.info("Rematched {} bindings for route {}", activeBindings.size(), route.getRouteCode());
    }

    public List<Map<String, Object>> getBindingsByRoute(Long routeId) {
        List<RouteBracketBinding> bindings = bindingRepository.findVisibleByRouteId(routeId);
        return bindings.stream()
                .map(b -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", b.getId());
                    map.put("routeId", b.getRoute().getId());
                    map.put("routeCode", b.getRoute().getRouteCode());
                    map.put("routeName", b.getRoute().getRouteName());
                    map.put("bracketId", b.getBracket().getId());
                    map.put("bracketCode", b.getBracket().getBracketCode());
                    map.put("bracketName", b.getBracket().getBracketName());
                    map.put("maxLoad", b.getBracket().getMaxLoad());
                    map.put("bracketMinWind", b.getBracket().getMinWindSpeed());
                    map.put("bracketMaxWind", b.getBracket().getMaxWindSpeed());
                    map.put("matchLevel", b.getMatchLevel());
                    map.put("launchWindSpeed", b.getLaunchWindSpeed());
                    map.put("windStatus", b.getWindStatus() != null ? b.getWindStatus() : "MATCHED");
                    map.put("status", b.getStatus());
                    return map;
                })
                .collect(Collectors.toList());
    }

    public List<Map<String, Object>> getBindingsByBracket(Long bracketId) {
        List<RouteBracketBinding> bindings = bindingRepository.findActiveByBracketId(bracketId);
        return bindings.stream()
                .map(b -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", b.getId());
                    map.put("routeId", b.getRoute().getId());
                    map.put("routeCode", b.getRoute().getRouteCode());
                    map.put("routeName", b.getRoute().getRouteName());
                    map.put("bracketId", b.getBracket().getId());
                    map.put("bracketCode", b.getBracket().getBracketCode());
                    map.put("bracketName", b.getBracket().getBracketName());
                    map.put("matchLevel", b.getMatchLevel());
                    map.put("launchWindSpeed", b.getLaunchWindSpeed());
                    map.put("windStatus", b.getWindStatus() != null ? b.getWindStatus() : "MATCHED");
                    map.put("status", b.getStatus());
                    return map;
                })
                .collect(Collectors.toList());
    }

    public long countBindingsByRoute(Long routeId) {
        return bindingRepository.countByRouteIdAndStatus(routeId, "ACTIVE");
    }
}
