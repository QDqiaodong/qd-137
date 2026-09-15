package com.example.balloon.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 一个班组（一班）及其归位清单。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DutyShiftDTO {

    private Long id;
    private String shiftCode;
    /** ACTIVE=本班进行中；CLOSED=已交接下班 */
    private String shiftStatus;
    private String startOperatorCode;
    private String closeOperatorCode;
    private LocalDateTime startedAt;
    private LocalDateTime closedAt;
    /** 清单条目数 */
    private long totalItems;
    /** 还停在场地的条目数；大于 0 即未全部收回 */
    private long onSiteItems;
}
