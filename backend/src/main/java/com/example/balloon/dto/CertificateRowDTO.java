package com.example.balloon.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 批量补录/换证中的一行。字段以字符串接收，
 * 由后端逐条校验（工号、日期、证号、换证次序），不合格要能定位到第几条。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CertificateRowDTO {

    /** 持人工号 */
    private String operatorCode;

    /** 证号 */
    private String certificateNo;

    /** 发证日期，yyyy-MM-dd */
    private String issueDate;

    /** 到期日期，yyyy-MM-dd */
    private String expireDate;
}
