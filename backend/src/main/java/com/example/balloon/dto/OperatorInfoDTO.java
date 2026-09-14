package com.example.balloon.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OperatorInfoDTO {

    private String operatorCode;

    private String operatorName;

    /** DISPATCHER=调度；LAUNCH_OPERATOR=放飞员 */
    private String role;

    /** 放飞员当班航线ID列表；调度为空列表（表示不限航线） */
    private List<Long> dutyRouteIds;
}
