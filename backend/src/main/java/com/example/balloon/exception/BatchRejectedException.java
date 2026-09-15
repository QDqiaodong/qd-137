package com.example.balloon.exception;

import com.example.balloon.dto.BatchRejectedDTO;
import lombok.Getter;

/**
 * 一批证照里某一条不合格：整批回滚、一条不落。
 * 由 GlobalExceptionHandler 映射为 422，并把卡住行号/原因/撞号占用人带回前端。
 */
@Getter
public class BatchRejectedException extends RuntimeException {

    private final BatchRejectedDTO detail;

    public BatchRejectedException(BatchRejectedDTO detail) {
        super(detail.getReason());
        this.detail = detail;
    }
}
