package com.example.balloon.exception;

/**
 * 已经有调度在写台账（全局写锁被占用）。
 * 两人同时交同一批时只许一个成功，另一个收到 409 并被告知稍后重试、本批未写入。
 */
public class LedgerLockedException extends RuntimeException {

    public LedgerLockedException(String message) {
        super(message);
    }
}
