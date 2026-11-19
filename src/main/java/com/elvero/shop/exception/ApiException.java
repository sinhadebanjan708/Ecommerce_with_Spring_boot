package com.elvero.shop.exception;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;

/**
 * Domain/business exception that maps cleanly to the Node-style
 * { success: false, message, code?, errors? } envelope.
 */
public class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final String code;
    private final List<Map<String, String>> fieldErrors;

    public ApiException(HttpStatus status, String message) {
        this(status, message, null, null);
    }

    public ApiException(HttpStatus status, String message, String code) {
        this(status, message, code, null);
    }

    public ApiException(HttpStatus status, String message, String code, List<Map<String, String>> fieldErrors) {
        super(message);
        this.status = status;
        this.code = code;
        this.fieldErrors = fieldErrors == null ? Collections.emptyList() : fieldErrors;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }

    public List<Map<String, String>> getFieldErrors() {
        return fieldErrors;
    }

    // ---- factories ----

    public static ApiException badRequest(String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, message, "BAD_REQUEST");
    }

    public static ApiException badRequest(String message, String code) {
        return new ApiException(HttpStatus.BAD_REQUEST, message, code);
    }

    public static ApiException validation(String message, List<Map<String, String>> fieldErrors) {
        return new ApiException(HttpStatus.BAD_REQUEST, message, "VALIDATION_ERROR", fieldErrors);
    }

    public static ApiException unauthorized(String message) {
        return new ApiException(HttpStatus.UNAUTHORIZED, message, "UNAUTHORIZED");
    }

    public static ApiException forbidden(String message) {
        return new ApiException(HttpStatus.FORBIDDEN, message, "FORBIDDEN");
    }

    public static ApiException notFound(String message) {
        return new ApiException(HttpStatus.NOT_FOUND, message, "NOT_FOUND");
    }

    public static ApiException notFound(String resource, Object id) {
        return new ApiException(
                HttpStatus.NOT_FOUND,
                resource + " not found" + (id != null ? " (id=" + id + ")" : ""),
                "NOT_FOUND");
    }

    public static ApiException conflict(String message) {
        return new ApiException(HttpStatus.CONFLICT, message, "CONFLICT");
    }

    public static ApiException unprocessable(String message) {
        return new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, message, "UNPROCESSABLE_ENTITY");
    }

    public static ApiException payloadTooLarge(String message) {
        return new ApiException(HttpStatus.PAYLOAD_TOO_LARGE, message, "PAYLOAD_TOO_LARGE");
    }

    public static ApiException internal(String message) {
        return new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, message, "INTERNAL_ERROR");
    }
}
