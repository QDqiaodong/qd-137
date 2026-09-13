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
public class RouteDTO {

    private Long id;

    @NotBlank(message = "航线编号不能为空")
    @Size(max = 50, message = "航线编号长度不能超过50")
    private String routeCode;

    @NotBlank(message = "航线名称不能为空")
    @Size(max = 100, message = "航线名称长度不能超过100")
    private String routeName;

    private String groupName;

    @NotNull(message = "最小风力不能为空")
    private Double minWindSpeed;

    @NotNull(message = "最大风力不能为空")
    private Double maxWindSpeed;

    private Double distance;

    private Integer duration;

    private String difficultyLevel;

    private String status;

    private String description;
}
