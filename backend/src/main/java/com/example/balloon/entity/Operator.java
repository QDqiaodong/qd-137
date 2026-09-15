package com.example.balloon.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "operator")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Operator {

    /** 调度：可改挂任意航线的支架 */
    public static final String ROLE_DISPATCHER = "DISPATCHER";
    /** 放飞员：只读，且仅限自己当班航线 */
    public static final String ROLE_LAUNCH_OPERATOR = "LAUNCH_OPERATOR";
    /** 地勤：负责地面固定支架的归位交接 */
    public static final String ROLE_GROUND_CREW = "GROUND_CREW";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "operator_code", unique = true, nullable = false, length = 50)
    private String operatorCode;

    @Column(name = "operator_name", nullable = false, length = 100)
    private String operatorName;

    @Column(name = "role", nullable = false, length = 20)
    private String role;

    @Column(name = "password_hash", nullable = false, length = 64)
    private String passwordHash;

    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private String status = "ACTIVE";

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
