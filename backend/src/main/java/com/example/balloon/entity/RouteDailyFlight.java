package com.example.balloon.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 调度按日记录的航线飞行趟次汇总。
 * 同一天同一条航线只有一行：再记一笔时趟次与时长累加，不另开新行。
 */
@Entity
@Table(
        name = "route_daily_flight",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_route_date",
                columnNames = {"route_id", "flight_date"}
        )
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RouteDailyFlight {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "route_id", nullable = false)
    private Long routeId;

    @Column(name = "flight_date", nullable = false)
    private LocalDate flightDate;

    /** 当天该航线累计飞行趟次 */
    @Column(name = "flight_count", nullable = false)
    @Builder.Default
    private Integer flightCount = 0;

    /** 当天该航线累计飞行时长（分钟） */
    @Column(name = "total_duration", nullable = false)
    @Builder.Default
    private Integer totalDuration = 0;

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();
}
