package com.ntdhtcct.common.exception;

import com.ntdhtcct.common.response.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.stream.Collectors;

/**
 * T-04.10: Xử lý tập trung các ngoại lệ toàn ứng dụng, đặc biệt là lỗi 403 Forbidden
 * khi không đủ quyền hoặc không phải thành viên dự án.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ErrorResponse> handleForbiddenException(ForbiddenException ex, HttpServletRequest request) {
        log.warn("Truy cập bị từ chối [403 Forbidden] tại {}: {}", request.getRequestURI(), ex.getMessage());
        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.FORBIDDEN.value(),
                "Forbidden",
                ex.getCode() != null ? ex.getCode() : "AUTH_FORBIDDEN",
                ex.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorizedException(UnauthorizedException ex, HttpServletRequest request) {
        log.warn("Chưa xác thực [401 Unauthorized] tại {}: {}", request.getRequestURI(), ex.getMessage());
        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.UNAUTHORIZED.value(),
                "Unauthorized",
                ex.getCode() != null ? ex.getCode() : "AUTH_UNAUTHORIZED",
                ex.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFoundException(ResourceNotFoundException ex, HttpServletRequest request) {
        log.warn("Không tìm thấy tài nguyên [404 Not Found] tại {}: {}", request.getRequestURI(), ex.getMessage());
        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.NOT_FOUND.value(),
                "Not Found",
                ex.getCode() != null ? ex.getCode() : "RESOURCE_NOT_FOUND",
                ex.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    @ExceptionHandler(DiaryDateLockedException.class)
    public ResponseEntity<ErrorResponse> handleDiaryDateLockedException(DiaryDateLockedException ex, HttpServletRequest request) {
        log.warn("Ngày nhật ký đã bị khóa sổ [409 Conflict] tại {}: {}", request.getRequestURI(), ex.getMessage());
        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.CONFLICT.value(),
                "Conflict",
                ex.getCode() != null ? ex.getCode() : "DIARY_DATE_LOCKED",
                ex.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ErrorResponse> handleBadRequestException(BadRequestException ex, HttpServletRequest request) {
        log.warn("Yêu cầu không hợp lệ [400 Bad Request] tại {}: {}", request.getRequestURI(), ex.getMessage());
        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Bad Request",
                ex.getCode() != null ? ex.getCode() : "BAD_REQUEST",
                ex.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException ex, HttpServletRequest request) {
        String details = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Bad Request",
                "VALIDATION_FAILED",
                details.isEmpty() ? "Dữ liệu không hợp lệ" : details,
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResourceFoundException(
            NoResourceFoundException ex,
            HttpServletRequest request
    ) {
        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.NOT_FOUND.value(),
                "Not Found",
                "RESOURCE_NOT_FOUND",
                "Không tìm thấy đường dẫn yêu cầu",
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadableException(
            HttpMessageNotReadableException ex,
            HttpServletRequest request
    ) {
        log.warn("Dữ liệu yêu cầu không hợp lệ hoặc sai định dạng JSON [400 Bad Request] tại {}: {}", request.getRequestURI(), ex.getMessage());
        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Bad Request",
                "INVALID_REQUEST_BODY",
                "Dữ liệu yêu cầu không hợp lệ hoặc sai định dạng JSON",
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentTypeMismatchException(
            MethodArgumentTypeMismatchException ex,
            HttpServletRequest request
    ) {
        log.warn("Tham số yêu cầu sai kiểu dữ liệu [400 Bad Request] tại {}: {}", request.getRequestURI(), ex.getMessage());
        String name = ex.getName();
        String message = String.format("Tham số '%s' có giá trị không hợp lệ", name);
        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Bad Request",
                "INVALID_PARAMETER_TYPE",
                message,
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParameterException(
            MissingServletRequestParameterException ex,
            HttpServletRequest request
    ) {
        log.warn("Thiếu tham số bắt buộc [400 Bad Request] tại {}: {}", request.getRequestURI(), ex.getMessage());
        String message = String.format("Thiếu tham số bắt buộc: %s", ex.getParameterName());
        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Bad Request",
                "MISSING_REQUIRED_PARAMETER",
                message,
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(
            IllegalArgumentException ex,
            HttpServletRequest request
    ) {
        log.warn("Tham số không hợp lệ [400 Bad Request] tại {}: {}", request.getRequestURI(), ex.getMessage());
        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Bad Request",
                "ILLEGAL_ARGUMENT",
                ex.getMessage() != null ? ex.getMessage() : "Tham số không hợp lệ",
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneralException(Exception ex, HttpServletRequest request) {
        log.error("Lỗi hệ thống [500 Internal Server Error] tại {}: {}", request.getRequestURI(), ex.getMessage(), ex);
        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Internal Server Error",
                "INTERNAL_SERVER_ERROR",
                "Đã xảy ra lỗi nội bộ trên máy chủ: " + ex.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
}
