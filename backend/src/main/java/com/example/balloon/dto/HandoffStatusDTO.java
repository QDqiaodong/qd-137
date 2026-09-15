package com.example.balloon.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 归位页打开时第一眼看到的交接状态：
 * 有进行中的班组 -> 展示本班清单；
 * 没有进行中的班组 -> 展示上一班（最近一个已交接班组）遗留的未收回支架，
 * allReturned 为真才允许开始本班。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HandoffStatusDTO {

    /** 当前进行中的班组；交接间隙为空 */
    private DutyShiftDTO activeShift;

    /** 交接间隙待收回的上一班班组；本班进行中或没有历史班组时为空 */
    private DutyShiftDTO pendingShift;

    /** 交接间隙需要先收回的清单（上一班 ON_SITE 条目）；本班进行中为空 */
    private java.util.List<ReturnItemDTO> pendingItems;

    /** 是否可以开始本班：交接间隙且上一班没有遗留 ON_SITE */
    private boolean canStartShift;

    /** 给页面的提示语 */
    private String message;
}
