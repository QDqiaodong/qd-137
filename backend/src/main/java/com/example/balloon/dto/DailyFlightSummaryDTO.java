package com.example.balloon.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 某条航线在某一天的合计：飞了几趟、一共飞了多久。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailyFlightSummaryDTO {

    private Long routeId;
    private String routeCode;
    private String routeName;
    private LocalDate flightDate;
    /** 当天累计趟次；没记过为0 */
    private Integer flightCount;
    /** 当天累计时长（分钟）；没记过为0 */
    private Integer totalDuration;
    /** 当天是否有过登记 */
    private Boolean recorded;
}
