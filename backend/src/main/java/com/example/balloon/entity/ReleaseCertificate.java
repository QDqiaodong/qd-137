package com.example.balloon.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 放飞证照台账记录：一名放飞员的一张放飞证。
 *
 * 同一证号全库唯一（{@link #certificateNo} 唯一约束兜底）。
 * 换证只能让到期日严格后延：新证到期日必须严格晚于该人库里到期日最晚的那张证。
 * 批量补录/换证整批一个事务：任一条不合格，整批回滚，一条都不落库。
 */
@Entity
@Table(name = "release_certificate",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_certificate_no",
                columnNames = "certificate_no"))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReleaseCertificate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 证号，全库唯一 */
    @Column(name = "certificate_no", nullable = false, length = 64)
    private String certificateNo;

    /** 持人工号 */
    @Column(name = "operator_code", nullable = false, length = 50)
    private String operatorCode;

    /** 持人姓名（冗余，便于台账展示与撞号定位） */
    @Column(name = "operator_name", nullable = false, length = 100)
    private String operatorName;

    /** 发证日期 */
    @Column(name = "issue_date", nullable = false)
    private LocalDate issueDate;

    /** 到期日期；换证新证的到期日必须严格晚于该人现有最晚到期日 */
    @Column(name = "expire_date", nullable = false)
    private LocalDate expireDate;

    /** 批次号，形如 CERT-BATCH-yyyyMMdd-三位序号；整批同号 */
    @Column(name = "batch_no", nullable = false, length = 64)
    private String batchNo;

    /** 经办调度工号 */
    @Column(name = "created_operator_code", nullable = false, length = 50)
    private String createdOperatorCode;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
