package com.example.balloon.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 调度登记一笔飞行记录：指定航线与日期，趟次与时长叠加到当天该航线的合计行。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FlightRecordDTO {

    @NotNull(message = "航线不能为空")
    private Long routeId;

    @NotNull(message = "日期不能为空")
    private LocalDate flightDate;

    @NotNull(message = "趟次不能为空")
    @Min(value = 1, message = "趟次至少为1")
    private Integer flightCount;

    @NotNull(message = "飞行时长不能为空")
    @Min(value = 1, message = "飞行时长至少为1分钟")
    private Integer durationMinutes;
}
