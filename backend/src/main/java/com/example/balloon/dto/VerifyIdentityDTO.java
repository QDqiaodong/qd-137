package com.example.balloon.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VerifyIdentityDTO {

    @NotBlank(message = "工号不能为空")
    private String operatorCode;

    @NotBlank(message = "口令不能为空")
    private String password;
}
