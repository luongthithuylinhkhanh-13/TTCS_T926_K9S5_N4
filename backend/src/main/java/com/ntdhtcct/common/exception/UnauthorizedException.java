package com.ntdhtcct.common.exception;

/**
 * Ngoại lệ ném ra khi không xác thực được danh tính người dùng (HTTP 401 Unauthorized).
 */
public class UnauthorizedException extends RuntimeException {

    private final String code;

    public UnauthorizedException(String message) {
        super(message);
        this.code = "AUTH_UNAUTHORIZED";
    }

    public UnauthorizedException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
