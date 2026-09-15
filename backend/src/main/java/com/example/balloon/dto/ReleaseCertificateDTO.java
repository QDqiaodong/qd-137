package com.example.balloon.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 台账中的一张放飞证。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReleaseCertificateDTO {

    private Long id;
    private String certificateNo;
    private String operatorCode;
    private String operatorName;
    private LocalDate issueDate;
    private LocalDate expireDate;
    private String batchNo;
    private String createdOperatorCode;
    private LocalDateTime createdAt;
}
