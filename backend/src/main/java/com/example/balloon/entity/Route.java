package com.example.balloon.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "route")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Route {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "route_code", unique = true, nullable = false, length = 50)
    private String routeCode;

    @Column(name = "route_name", nullable = false, length = 100)
    private String routeName;

    @Column(name = "group_name", length = 50)
    private String groupName;

    @Column(name = "min_wind_speed", nullable = false)
    private Double minWindSpeed;

    @Column(name = "max_wind_speed", nullable = false)
    private Double maxWindSpeed;

    @Column(name = "distance")
    private Double distance;

    @Column(name = "duration")
    private Integer duration;

    @Column(name = "difficulty_level", length = 20)
    private String difficultyLevel;

    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private String status = "ACTIVE";

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
