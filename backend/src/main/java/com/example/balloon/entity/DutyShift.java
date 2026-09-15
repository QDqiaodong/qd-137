package com.example.balloon.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 地勤班组：一次「上班 → 作业 → 交接」为一个班组。
 * 全班组同时只能有一个处于 ACTIVE 状态的班组；上一班交接后状态置为 CLOSED。
 */
@Entity
@Table(name = "duty_shift",
        uniqueConstraints = @UniqueConstraint(name = "uk_shift_code", columnNames = "shift_code"))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DutyShift {

    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_CLOSED = "CLOSED";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 班组编号，形如 SHIFT-20260915-001 */
    @Column(name = "shift_code", nullable = false, length = 50)
    private String shiftCode;

    @Column(name = "shift_status", nullable = false, length = 20)
    @Builder.Default
    private String shiftStatus = STATUS_ACTIVE;

    /** 开始本班的地勤（工号） */
    @Column(name = "start_operator_code", nullable = false, length = 50)
    private String startOperatorCode;

    /** 交接下班的地勤（工号）；未交接前为空 */
    @Column(name = "close_operator_code", length = 50)
    private String closeOperatorCode;

    @Column(name = "started_at", nullable = false)
    @Builder.Default
    private LocalDateTime startedAt = LocalDateTime.now();

    @Column(name = "closed_at")
    private LocalDateTime closedAt;
}
