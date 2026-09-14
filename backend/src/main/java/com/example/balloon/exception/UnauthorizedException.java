package com.example.balloon.exception;

/**
 * 身份核对失败：未提供身份或工号/口令不正确
 */
public class UnauthorizedException extends RuntimeException {

    public UnauthorizedException(String message) {
        super(message);
    }
}
