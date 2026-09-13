package com.example.balloon.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "wind_match_log")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WindMatchLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "route_id", nullable = false)
    private Long routeId;

    @Column(name = "route_code", length = 50)
    private String routeCode;

    @Column(name = "bracket_id")
    private Long bracketId;

    @Column(name = "bracket_code", length = 50)
    private String bracketCode;

    @Column(name = "old_min_wind", nullable = false)
    private Double oldMinWind;

    @Column(name = "old_max_wind", nullable = false)
    private Double oldMaxWind;

    @Column(name = "new_min_wind", nullable = false)
    private Double newMinWind;

    @Column(name = "new_max_wind", nullable = false)
    private Double newMaxWind;

    @Column(name = "action_type", nullable = false, length = 50)
    private String actionType;

    @Column(name = "match_result", length = 100)
    private String matchResult;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
