package com.example.balloon.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 地勤下班登记：当天动过的支架。同一班组内同一支架只能登记一次。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddReturnItemRequest {

    @NotNull(message = "支架不能为空")
    private Long bracketId;
}
