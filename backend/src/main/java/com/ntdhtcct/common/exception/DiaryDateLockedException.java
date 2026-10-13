package com.ntdhtcct.common.exception;

import org.springframework.http.HttpStatus;

/**
 * T-65: Ngoại lệ ném ra khi cố gắng tạo, sửa hoặc đồng bộ nhật ký vào ngày đã bị khóa sổ.
 * Ánh xạ với HTTP 409 Conflict.
 */
public class DiaryDateLockedException extends RuntimeException {

    private final String code;
    private final int status;

    public DiaryDateLockedException(String message) {
        super(message);
        this.code = "DIARY_DATE_LOCKED";
        this.status = HttpStatus.CONFLICT.value();
    }

    public DiaryDateLockedException(String code, String message) {
        super(message);
        this.code = code;
        this.status = HttpStatus.CONFLICT.value();
    }

    public String getCode() {
        return code;
    }

    public int getStatus() {
        return status;
    }
}
