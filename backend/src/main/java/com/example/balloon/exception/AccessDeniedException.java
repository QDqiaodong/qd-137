package com.example.balloon.exception;

/**
 * 已核对身份，但当前角色无权执行该操作
 */
public class AccessDeniedException extends RuntimeException {

    public AccessDeniedException(String message) {
        super(message);
    }
}
