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
}
