package com.example.balloon.controller;

import com.example.balloon.dto.OperatorInfoDTO;
import com.example.balloon.dto.VerifyIdentityDTO;
import com.example.balloon.entity.Operator;
import com.example.balloon.service.OperatorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final OperatorService operatorService;

    /**
     * 身份核对：工号 + 口令，核对通过后返回角色与当班航线范围
     */
    @PostMapping("/verify")
    public ResponseEntity<OperatorInfoDTO> verify(@Valid @RequestBody VerifyIdentityDTO dto) {
        Operator operator = operatorService.verifyIdentity(dto.getOperatorCode(), dto.getPassword());
        return ResponseEntity.ok(operatorService.toOperatorInfo(operator));
    }
}
