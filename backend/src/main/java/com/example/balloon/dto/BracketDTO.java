package com.example.balloon.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BracketDTO {

    private Long id;

    @NotBlank(message = "支架编号不能为空")
    @Size(max = 50, message = "支架编号长度不能超过50")
    private String bracketCode;

    @NotBlank(message = "支架名称不能为空")
    @Size(max = 100, message = "支架名称长度不能超过100")
    private String bracketName;

    @NotNull(message = "最大承重不能为空")
    @Positive(message = "最大承重必须为正数")
    private Double maxLoad;

    @NotNull(message = "最小风力不能为空")
    private Double minWindSpeed;

    @NotNull(message = "最大风力不能为空")
    private Double maxWindSpeed;

    @NotBlank(message = "支架类型不能为空")
    private String bracketType;

    private String status;

    private String description;
}
