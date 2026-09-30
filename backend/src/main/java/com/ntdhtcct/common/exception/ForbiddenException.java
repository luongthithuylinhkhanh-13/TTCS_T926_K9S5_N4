package com.ntdhtcct.common.exception;

/**
 * T-04.10: Ngoại lệ ném ra khi người dùng không đủ quyền truy cập (HTTP 403 Forbidden).
 */
public class ForbiddenException extends RuntimeException {

    private final String code;

    public ForbiddenException(String message) {
        super(message);
        this.code = "AUTH_FORBIDDEN";
    }

    public ForbiddenException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
