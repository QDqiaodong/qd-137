package com.example.balloon.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 放飞员当班航线：一名放飞员只对排入当班的航线有查看权限
 */
@Entity
@Table(name = "duty_assignment", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"operator_id", "route_id"})
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DutyAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "operator_id", nullable = false)
    private Operator operator;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "route_id", nullable = false)
    private Route route;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
