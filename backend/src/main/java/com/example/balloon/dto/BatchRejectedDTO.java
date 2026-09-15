package com.example.balloon.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 整批被拦下时的说明：卡在第几条（从 1 开始）、因为什么。
 * 证号撞号时还要写出目前是谁、工号多少在用这号。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchRejectedDTO {

    /** 卡住的行号，从 1 开始 */
    private int rowNumber;

    /** 卡住的那一行的原始内容，便于页面定位 */
    private CertificateRowDTO row;

    /** 拒绝原因（人能看懂的中文说明） */
    private String reason;

    /** 冲突类型：OPERATOR_NOT_FOUND / DATE_FORMAT / DATE_ORDER /
     *  CERT_NO_DUPLICATE_IN_DB / CERT_NO_DUPLICATE_IN_BATCH / RENEWAL_ORDER */
    private String conflictType;

    /** 证号撞库时，目前占用该证号的持人姓名 */
    private String currentHolderName;

    /** 证号撞库时，目前占用该证号的持人工号 */
    private String currentHolderCode;
}
