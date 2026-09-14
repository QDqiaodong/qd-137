package com.example.balloon.service;

import com.example.balloon.dto.BracketDTO;
import com.example.balloon.dto.RouteDTO;
import com.example.balloon.dto.WindMatchResult;
import com.example.balloon.entity.Bracket;
import com.example.balloon.entity.Route;
import com.example.balloon.entity.WindMatchLog;
import com.example.balloon.repository.BracketRepository;
import com.example.balloon.repository.RouteRepository;
import com.example.balloon.repository.WindMatchLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class WindMatchService {

    private final BracketRepository bracketRepository;
    private final RouteRepository routeRepository;
    private final WindMatchLogRepository windMatchLogRepository;

    public WindMatchResult checkWindMatch(Long routeId, Long bracketId) {
        return checkWindMatch(routeId, bracketId, null);
    }

    public WindMatchResult checkWindMatch(Long routeId, Long bracketId, Double launchWindSpeed) {
        Route route = routeRepository.findById(routeId)
                .orElseThrow(() -> new IllegalArgumentException("航线不存在: " + routeId));
        Bracket bracket = bracketRepository.findById(bracketId)
                .orElseThrow(() -> new IllegalArgumentException("支架不存在: " + bracketId));

        WindMatchResult result = checkWindMatch(route.getMinWindSpeed(), route.getMaxWindSpeed(), bracket);
        String windStatus = evaluateWindStatus(launchWindSpeed, route, bracket);
        result.setWindStatus(windStatus);
        result.setLaunchWindSpeed(launchWindSpeed);
        result.setMessage(buildWindCheckMessage(launchWindSpeed, route, bracket));
        return result;
    }

    public WindMatchResult checkWindMatch(Double routeMinWind, Double routeMaxWind, Bracket bracket) {
        String matchLevel = calculateMatchLevel(routeMinWind, routeMaxWind, bracket);
        boolean matched = !"NONE".equals(matchLevel);
        
        String message;
        if ("PERFECT".equals(matchLevel)) {
            message = "完全匹配：支架风力区间完全覆盖航线风力区间";
        } else if ("PARTIAL".equals(matchLevel)) {
            message = "部分匹配：支架风力区间部分覆盖航线风力区间";
        } else {
            message = "不匹配：支架风力区间无法覆盖航线风力区间";
        }

        return WindMatchResult.builder()
                .matched(matched)
                .matchLevel(matchLevel)
                .message(message)
                .build();
    }

    public List<BracketDTO> findSuitableBracketsForRoute(Long routeId) {
        Route route = routeRepository.findById(routeId)
                .orElseThrow(() -> new IllegalArgumentException("航线不存在: " + routeId));
        
        return bracketRepository.findSuitableBrackets(route.getMinWindSpeed(), route.getMaxWindSpeed()).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<BracketDTO> findSuitableBracketsByWindRange(Double minWind, Double maxWind) {
        return bracketRepository.findSuitableBrackets(minWind, maxWind).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public WindMatchResult rematchBracketsForRoute(Long routeId) {
        Route route = routeRepository.findById(routeId)
                .orElseThrow(() -> new IllegalArgumentException("航线不存在: " + routeId));

        Double oldMinWind = route.getMinWindSpeed();
        Double oldMaxWind = route.getMaxWindSpeed();

        List<Bracket> allActiveBrackets = bracketRepository.findByStatus("ACTIVE");
        List<BracketDTO> suitableBrackets = new ArrayList<>();
        List<BracketDTO> unsuitableBrackets = new ArrayList<>();

        for (Bracket bracket : allActiveBrackets) {
            String matchLevel = calculateMatchLevel(route.getMinWindSpeed(), route.getMaxWindSpeed(), bracket);
            if (!"NONE".equals(matchLevel)) {
                suitableBrackets.add(convertToDTO(bracket));
            } else {
                unsuitableBrackets.add(convertToDTO(bracket));
            }
        }

        saveMatchLog(routeId, route.getRouteCode(), null, null, 
                oldMinWind, oldMaxWind, route.getMinWindSpeed(), route.getMaxWindSpeed(),
                "REMATCH", suitableBrackets.isEmpty() ? "NO_MATCH" : "MATCHED",
                "航线风力参数更新后重新匹配，适配支架数: " + suitableBrackets.size());

        return WindMatchResult.builder()
                .matched(!suitableBrackets.isEmpty())
                .matchLevel(suitableBrackets.isEmpty() ? "NONE" : "PARTIAL")
                .message("重新匹配完成，共找到 " + suitableBrackets.size() + " 个适配支架")
                .suitableBrackets(suitableBrackets)
                .unsuitableBrackets(unsuitableBrackets)
                .build();
    }

    public String calculateMatchLevel(Double routeMinWind, Double routeMaxWind, Bracket bracket) {
        Double bracketMinWind = bracket.getMinWindSpeed();
        Double bracketMaxWind = bracket.getMaxWindSpeed();

        if (bracketMinWind <= routeMinWind && bracketMaxWind >= routeMaxWind) {
            return "PERFECT";
        }

        if (bracketMaxWind >= routeMinWind && bracketMinWind <= routeMaxWind) {
            return "PARTIAL";
        }

        return "NONE";
    }

    /**
     * 核对当日放飞风速是否同时落在航线与支架各自的适飞窗口内。
     * 未填写风速或任一窗口不覆盖该风速时，判定为区间错位(MISMATCH)。
     */
    public String evaluateWindStatus(Double launchWindSpeed, Route route, Bracket bracket) {
        if (launchWindSpeed == null) {
            return "MISMATCH";
        }
        boolean inRouteWindow = launchWindSpeed >= route.getMinWindSpeed()
                && launchWindSpeed <= route.getMaxWindSpeed();
        boolean inBracketWindow = launchWindSpeed >= bracket.getMinWindSpeed()
                && launchWindSpeed <= bracket.getMaxWindSpeed();
        return (inRouteWindow && inBracketWindow) ? "MATCHED" : "MISMATCH";
    }

    /**
     * 生成当日放飞风速对照结果说明，用于绑定响应与日志。
     */
    public String buildWindCheckMessage(Double launchWindSpeed, Route route, Bracket bracket) {
        String routeWindow = String.format("[%.1f-%.1f]", route.getMinWindSpeed(), route.getMaxWindSpeed());
        String bracketWindow = String.format("[%.1f-%.1f]", bracket.getMinWindSpeed(), bracket.getMaxWindSpeed());

        if (launchWindSpeed == null) {
            return "区间错位：未填写当日放飞风速，不能完成绑定";
        }

        boolean inRouteWindow = launchWindSpeed >= route.getMinWindSpeed()
                && launchWindSpeed <= route.getMaxWindSpeed();
        boolean inBracketWindow = launchWindSpeed >= bracket.getMinWindSpeed()
                && launchWindSpeed <= bracket.getMaxWindSpeed();

        if (inRouteWindow && inBracketWindow) {
            return String.format("区间匹配：当日放飞风速 %.1f m/s 同时在航线适飞窗口%s与支架适飞窗口%s内",
                    launchWindSpeed, routeWindow, bracketWindow);
        }
        if (!inRouteWindow && !inBracketWindow) {
            return String.format("区间错位：当日放飞风速 %.1f m/s 既不在航线适飞窗口%s内，也不在支架适飞窗口%s内，不能完成绑定",
                    launchWindSpeed, routeWindow, bracketWindow);
        }
        if (!inRouteWindow) {
            return String.format("区间错位：当日放飞风速 %.1f m/s 不在航线适飞窗口%s内，不能完成绑定",
                    launchWindSpeed, routeWindow);
        }
        return String.format("区间错位：当日放飞风速 %.1f m/s 不在支架适飞窗口%s内，不能完成绑定",
                launchWindSpeed, bracketWindow);
    }

    @Transactional
    public void saveMatchLog(Long routeId, String routeCode, Long bracketId, String bracketCode,
                             Double oldMinWind, Double oldMaxWind, Double newMinWind, Double newMaxWind,
                             String actionType, String matchResult, String description) {
        WindMatchLog log = WindMatchLog.builder()
                .routeId(routeId)
                .routeCode(routeCode)
                .bracketId(bracketId)
                .bracketCode(bracketCode)
                .oldMinWind(oldMinWind)
                .oldMaxWind(oldMaxWind)
                .newMinWind(newMinWind)
                .newMaxWind(newMaxWind)
                .actionType(actionType)
                .matchResult(matchResult)
                .description(description)
                .build();
        windMatchLogRepository.save(log);
    }

    public List<WindMatchLog> getMatchLogsByRoute(Long routeId) {
        return windMatchLogRepository.findByRouteIdOrderByCreatedAtDesc(routeId);
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
