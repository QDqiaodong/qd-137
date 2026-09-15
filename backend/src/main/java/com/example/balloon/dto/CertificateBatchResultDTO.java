package com.example.balloon.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 批量落库结果。成功时所有行均已落库；
 * 任一条不合格时后端抛 422，返回 {@link BatchRejectedDTO}，整批已回滚。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CertificateBatchResultDTO {

    /** 批次号，形如 CERT-BATCH-yyyyMMdd-001 */
    private String batchNo;

    /** 本批实际落库条数 */
    private int savedCount;

    private String message;
}
