package com.example.balloon.controller;

import com.example.balloon.dto.DailyFlightSummaryDTO;
import com.example.balloon.dto.FlightRecordDTO;
import com.example.balloon.entity.Operator;
import com.example.balloon.service.DailyFlightService;
import com.example.balloon.service.OperatorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * 调度按日登记航线飞行趟次与时长。
 * 每条航线每天只有一行合计，重复登记在原行上累加。
 * 所有接口逐请求核对身份：仅调度可登记；放飞员只能查看自己当班航线的合计。
 */
@RestController
@RequestMapping("/api/daily-flights")
@RequiredArgsConstructor
public class DailyFlightController {

    private final DailyFlightService dailyFlightService;
    private final OperatorService operatorService;

    /**
     * 登记一笔飞行记录（趟次、时长叠加到对应日期与航线的合计行）。
     */
    @PostMapping
    public ResponseEntity<DailyFlightSummaryDTO> recordFlight(
            @RequestHeader(value = "X-Operator-Code", required = false) String operatorCode,
            @RequestHeader(value = "X-Operator-Password", required = false) String operatorPassword,
            @Valid @RequestBody FlightRecordDTO dto) {
        Operator operator = operatorService.verifyIdentity(operatorCode, operatorPassword);
        operatorService.requireDispatcher(operator);
        return ResponseEntity.ok(dailyFlightService.recordFlight(dto));
    }

    /**
     * 按日期查询各航线合计。调度可见全部航线；放飞员仅可见当班航线。
     * 当天没记过的航线也会返回，flightCount/totalDuration 为 0 且 recorded=false。
     */
    @GetMapping
    public ResponseEntity<List<DailyFlightSummaryDTO>> listByDate(
            @RequestHeader(value = "X-Operator-Code", required = false) String operatorCode,
            @RequestHeader(value = "X-Operator-Password", required = false) String operatorPassword,
            @RequestParam("date") LocalDate date) {
        Operator operator = operatorService.verifyIdentity(operatorCode, operatorPassword);
        List<DailyFlightSummaryDTO> summaries = dailyFlightService.listSummaries(date);
        if (!operatorService.isDispatcher(operator)) {
            List<Long> dutyRouteIds = operatorService.getDutyRouteIds(operator.getId());
            summaries = summaries.stream()
                    .filter(s -> dutyRouteIds.contains(s.getRouteId()))
                    .toList();
        }
        return ResponseEntity.ok(summaries);
    }

    /**
     * 查询单条航线某天的合计；没记过也返回0趟次0时长。
     */
    @GetMapping("/route/{routeId}")
    public ResponseEntity<DailyFlightSummaryDTO> getByRouteAndDate(
            @RequestHeader(value = "X-Operator-Code", required = false) String operatorCode,
            @RequestHeader(value = "X-Operator-Password", required = false) String operatorPassword,
            @PathVariable Long routeId,
            @RequestParam("date") LocalDate date) {
        Operator operator = operatorService.verifyIdentity(operatorCode, operatorPassword);
        operatorService.requireRouteVisible(operator, routeId);
        return ResponseEntity.ok(dailyFlightService.getSummary(routeId, date));
    }
}
