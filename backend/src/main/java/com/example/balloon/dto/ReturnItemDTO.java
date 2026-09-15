package com.example.balloon.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 归位清单条目：支架编号 + 收回状态（RETURNED=已收回停放区 / ON_SITE=还停在场地）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReturnItemDTO {

    private Long id;
    private Long shiftId;
    private Long bracketId;
    private String bracketCode;
    private String bracketName;
    private String returnStatus;
    private String createdOperatorCode;
    private String returnedOperatorCode;
    private LocalDateTime createdAt;
    private LocalDateTime returnedAt;
}
