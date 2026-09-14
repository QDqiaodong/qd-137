package com.example.balloon.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BindingDTO {

    @NotNull(message = "航线ID不能为空")
    private Long routeId;

    @NotNull(message = "支架ID不能为空")
    private Long bracketId;

    /**
     * 当日放飞风速(m/s)，放飞员当场填写；可为空，为空时按区间错位处理
     */
    private Double launchWindSpeed;
}
