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
public class WindMatchResult {

    private boolean matched;

    private String matchLevel;

    private String message;

    private List<BracketDTO> suitableBrackets;

    private List<BracketDTO> unsuitableBrackets;
}
