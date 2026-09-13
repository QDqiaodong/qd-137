package com.example.balloon.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "bracket")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Bracket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "bracket_code", unique = true, nullable = false, length = 50)
    private String bracketCode;

    @Column(name = "bracket_name", nullable = false, length = 100)
    private String bracketName;

    @Column(name = "max_load", nullable = false)
    private Double maxLoad;

    @Column(name = "min_wind_speed", nullable = false)
    private Double minWindSpeed;

    @Column(name = "max_wind_speed", nullable = false)
    private Double maxWindSpeed;

    @Column(name = "bracket_type", nullable = false, length = 20)
    private String bracketType;

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
