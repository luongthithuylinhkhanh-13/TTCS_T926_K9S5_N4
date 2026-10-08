package com.ntdhtcct.common.response;

import java.time.OffsetDateTime;

/**
 * T-04.10: Đối tượng phản hồi lỗi chuẩn cho toàn bộ API hệ thống.
 */
public class ErrorResponse {

    private OffsetDateTime timestamp;
    private int status;
    private String error;
    private String code;
    private String message;
    private String path;

    public ErrorResponse() {
        this.timestamp = OffsetDateTime.now();
    }

    public ErrorResponse(int status, String error, String code, String message, String path) {
        this.timestamp = OffsetDateTime.now();
        this.status = status;
        this.error = error;
        this.code = code;
        this.message = message;
        this.path = path;
    }

    // Getters and Setters
    public OffsetDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(OffsetDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }
}
