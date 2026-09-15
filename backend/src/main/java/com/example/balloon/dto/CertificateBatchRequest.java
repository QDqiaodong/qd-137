package com.example.balloon.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 一批放飞证照（历年散落证照补录 + 在册人员换证，可混在同一批）。
 * 整批要么全部落库，要么一条不落。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CertificateBatchRequest {

    @NotEmpty(message = "批次不能为空，至少填写一条证照")
    @Valid
    private List<CertificateRowDTO> rows;
}
