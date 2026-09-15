package com.example.balloon.service;

import com.example.balloon.dto.DailyFlightSummaryDTO;
import com.example.balloon.dto.FlightRecordDTO;
import com.example.balloon.entity.Route;
import com.example.balloon.entity.RouteDailyFlight;
import com.example.balloon.repository.RouteDailyFlightRepository;
import com.example.balloon.repository.RouteRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DailyFlightService {

    private final RouteDailyFlightRepository dailyFlightRepository;
    private final RouteRepository routeRepository;

    /**
     * 登记一笔飞行：同一天同一条航线在原合计行上累加趟次与时长，不另开新行。
     */
    @Transactional
    public DailyFlightSummaryDTO recordFlight(FlightRecordDTO dto) {
        Route route = routeRepository.findById(dto.getRouteId())
                .filter(r -> "ACTIVE".equals(r.getStatus()))
                .orElseThrow(() -> new EntityNotFoundException("航线不存在: " + dto.getRouteId()));

        dailyFlightRepository.upsertIncrement(
                route.getId(),
                dto.getFlightDate(),
                dto.getFlightCount(),
                dto.getDurationMinutes());

        log.info("Route {} flight record on {} added: +{} trips, +{} minutes",
                route.getRouteCode(), dto.getFlightDate(),
                dto.getFlightCount(), dto.getDurationMinutes());

        RouteDailyFlight updated = dailyFlightRepository
                .findByRouteIdAndFlightDate(route.getId(), dto.getFlightDate())
                .orElseThrow(() -> new IllegalStateException("合计行写入失败"));
        return toSummary(route, updated);
    }

    /**
     * 查询某条航线某天的合计；当天没记过也返回一行，趟次与时长为0、recorded=false。
     */
    @Transactional(readOnly = true)
    public DailyFlightSummaryDTO getSummary(Long routeId, LocalDate flightDate) {
        Route route = routeRepository.findById(routeId)
                .orElseThrow(() -> new EntityNotFoundException("航线不存在: " + routeId));
        return dailyFlightRepository.findByRouteIdAndFlightDate(routeId, flightDate)
                .map(stat -> toSummary(route, stat))
                .orElseGet(() -> emptySummary(route, flightDate));
    }

    /**
     * 查询全部启用航线在指定日期的合计；没记过的航线同样给出0趟次0时长的空合计。
     */
    @Transactional(readOnly = true)
    public List<DailyFlightSummaryDTO> listSummaries(LocalDate flightDate) {
        List<Route> activeRoutes = routeRepository.findByStatus("ACTIVE");
        Map<Long, RouteDailyFlight> statByRoute = dailyFlightRepository.findByFlightDate(flightDate).stream()
                .collect(Collectors.toMap(RouteDailyFlight::getRouteId, Function.identity(), (a, b) -> a));

        List<DailyFlightSummaryDTO> summaries = new ArrayList<>();
        for (Route route : activeRoutes) {
            RouteDailyFlight stat = statByRoute.get(route.getId());
            summaries.add(stat != null ? toSummary(route, stat) : emptySummary(route, flightDate));
        }
        return summaries;
    }

    private DailyFlightSummaryDTO toSummary(Route route, RouteDailyFlight stat) {
        return DailyFlightSummaryDTO.builder()
                .routeId(route.getId())
                .routeCode(route.getRouteCode())
                .routeName(route.getRouteName())
                .flightDate(stat.getFlightDate())
                .flightCount(stat.getFlightCount())
                .totalDuration(stat.getTotalDuration())
                .recorded(true)
                .build();
    }

    private DailyFlightSummaryDTO emptySummary(Route route, LocalDate flightDate) {
        return DailyFlightSummaryDTO.builder()
                .routeId(route.getId())
                .routeCode(route.getRouteCode())
                .routeName(route.getRouteName())
                .flightDate(flightDate)
                .flightCount(0)
                .totalDuration(0)
                .recorded(false)
                .build();
    }
}
