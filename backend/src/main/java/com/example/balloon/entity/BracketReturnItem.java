package com.example.balloon.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 归位清单条目：地勤当天动过的地面固定支架逐一登记。
 * RETURNED=已收回停放区；ON_SITE=还停在场地。
 * 下一班必须先把上一班遗留的 ON_SITE 全部收回，才能开始本班。
 */
@Entity
@Table(name = "bracket_return_item",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_shift_bracket",
                columnNames = {"shift_id", "bracket_id"}))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BracketReturnItem {

    public static final String STATUS_RETURNED = "RETURNED";
    public static final String STATUS_ON_SITE = "ON_SITE";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "shift_id", nullable = false)
    private Long shiftId;

    @Column(name = "bracket_id", nullable = false)
    private Long bracketId;

    /** 冗余支架编号/名称，防止支架日后被停用或删除后清单无法展示 */
    @Column(name = "bracket_code", nullable = false, length = 50)
    private String bracketCode;

    @Column(name = "bracket_name", nullable = false, length = 100)
    private String bracketName;

    @Column(name = "return_status", nullable = false, length = 20)
    @Builder.Default
    private String returnStatus = STATUS_ON_SITE;

    /** 登记该支架的地勤（工号） */
    @Column(name = "created_operator_code", nullable = false, length = 50)
    private String createdOperatorCode;

    /** 标记收回的地勤（工号）；仍在场地时为空 */
    @Column(name = "returned_operator_code", length = 50)
    private String returnedOperatorCode;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "returned_at")
    private LocalDateTime returnedAt;
}
