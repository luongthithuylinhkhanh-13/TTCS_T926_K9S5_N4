package com.ntdhtcct.common.exception;

import com.ntdhtcct.common.response.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;
    private HttpServletRequest request;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
        request = new MockHttpServletRequest("GET", "/api/test-endpoint");
    }

    @Test
    @DisplayName("HttpMessageNotReadableException returns 400 Bad Request with code INVALID_REQUEST_BODY")
    void handleHttpMessageNotReadableException() {
        HttpMessageNotReadableException ex = new HttpMessageNotReadableException(
                "Malformed JSON", new MockHttpInputMessage(new byte[0])
        );

        ResponseEntity<ErrorResponse> response = handler.handleHttpMessageNotReadableException(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(400);
        assertThat(response.getBody().getCode()).isEqualTo("INVALID_REQUEST_BODY");
        assertThat(response.getBody().getMessage()).isEqualTo("Dữ liệu yêu cầu không hợp lệ hoặc sai định dạng JSON");
    }

    @Test
    @DisplayName("MethodArgumentTypeMismatchException returns 400 Bad Request with code INVALID_PARAMETER_TYPE")
    void handleMethodArgumentTypeMismatchException() {
        MethodArgumentTypeMismatchException ex = new MethodArgumentTypeMismatchException(
                "invalid-uuid", String.class, "projectId", null, new IllegalArgumentException("bad uuid")
        );

        ResponseEntity<ErrorResponse> response = handler.handleMethodArgumentTypeMismatchException(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(400);
        assertThat(response.getBody().getCode()).isEqualTo("INVALID_PARAMETER_TYPE");
        assertThat(response.getBody().getMessage()).contains("projectId");
    }

    @Test
    @DisplayName("MissingServletRequestParameterException returns 400 Bad Request with code MISSING_REQUIRED_PARAMETER")
    void handleMissingServletRequestParameterException() {
        MissingServletRequestParameterException ex = new MissingServletRequestParameterException("date", "String");

        ResponseEntity<ErrorResponse> response = handler.handleMissingParameterException(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(400);
        assertThat(response.getBody().getCode()).isEqualTo("MISSING_REQUIRED_PARAMETER");
        assertThat(response.getBody().getMessage()).contains("date");
    }

    @Test
    @DisplayName("IllegalArgumentException returns 400 Bad Request with code ILLEGAL_ARGUMENT")
    void handleIllegalArgumentException() {
        IllegalArgumentException ex = new IllegalArgumentException("Dữ liệu ngày không được lớn hơn hiện tại");

        ResponseEntity<ErrorResponse> response = handler.handleIllegalArgumentException(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(400);
        assertThat(response.getBody().getCode()).isEqualTo("ILLEGAL_ARGUMENT");
        assertThat(response.getBody().getMessage()).isEqualTo("Dữ liệu ngày không được lớn hơn hiện tại");
    }
}
