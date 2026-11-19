package com.elvero.shop.exception;

import com.elvero.shop.common.ApiResponse;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ConstraintViolationException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Replaces Express errorHandler middleware.
 * Every error body is { success: false, message, code?, errors? }.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // ---------- Domain / business ----------

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiResponse<Void>> handleApi(ApiException ex) {
        ApiResponse<Void> body = ApiResponse.error(ex.getMessage(), ex.getCode(),
                ex.getFieldErrors().isEmpty() ? null : ex.getFieldErrors());
        return ResponseEntity.status(ex.getStatus()).body(body);
    }

    // ---------- Bean validation (@Valid / @RequestBody) ----------

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException ex) {
        List<Map<String, String>> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> {
                    Map<String, String> m = new LinkedHashMap<>();
                    m.put("field", fe.getField());
                    m.put("message", fe.getDefaultMessage() != null ? fe.getDefaultMessage() : "Invalid value");
                    if (fe.getRejectedValue() != null) {
                        m.put("rejectedValue", String.valueOf(fe.getRejectedValue()));
                    }
                    return m;
                })
                .collect(Collectors.toList());

        String summary = fieldErrors.stream()
                .map(e -> e.get("field") + ": " + e.get("message"))
                .collect(Collectors.joining("; "));

        return ResponseEntity.badRequest()
                .body(ApiResponse.error(
                        summary.isBlank() ? "Validation failed" : summary,
                        "VALIDATION_ERROR",
                        fieldErrors));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraint(ConstraintViolationException ex) {
        List<Map<String, String>> fieldErrors = ex.getConstraintViolations().stream()
                .map(cv -> {
                    Map<String, String> m = new LinkedHashMap<>();
                    m.put("field", cv.getPropertyPath() != null ? cv.getPropertyPath().toString() : "unknown");
                    m.put("message", cv.getMessage());
                    return m;
                })
                .collect(Collectors.toList());

        String summary = fieldErrors.stream()
                .map(e -> e.get("field") + ": " + e.get("message"))
                .collect(Collectors.joining("; "));

        return ResponseEntity.badRequest()
                .body(ApiResponse.error(
                        summary.isBlank() ? "Constraint violation" : summary,
                        "VALIDATION_ERROR",
                        fieldErrors));
    }

    // ---------- Request parsing / parameters ----------

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingParam(MissingServletRequestParameterException ex) {
        String message = "Required parameter '" + ex.getParameterName()
                + "' (" + ex.getParameterType() + ") is missing";
        return ResponseEntity.badRequest()
                .body(ApiResponse.error(message, "MISSING_PARAMETER"));
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingPart(MissingServletRequestPartException ex) {
        String message = "Required multipart part '" + ex.getRequestPartName() + "' is missing";
        return ResponseEntity.badRequest()
                .body(ApiResponse.error(message, "MISSING_PART"));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String required = ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown";
        String message = "Parameter '" + ex.getName() + "' must be of type " + required
                + (ex.getValue() != null ? " (received: '" + ex.getValue() + "')" : "");
        return ResponseEntity.badRequest()
                .body(ApiResponse.error(message, "TYPE_MISMATCH"));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnreadable(HttpMessageNotReadableException ex) {
        String detail = ex.getMostSpecificCause() != null
                ? ex.getMostSpecificCause().getMessage()
                : ex.getMessage();
        // Keep client-facing message short; full detail only in logs
        log.debug("Unreadable request body: {}", detail);
        return ResponseEntity.badRequest()
                .body(ApiResponse.error(
                        "Invalid or malformed JSON request body",
                        "MALFORMED_BODY"));
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleMediaType(HttpMediaTypeNotSupportedException ex) {
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                .body(ApiResponse.error(
                        "Unsupported Content-Type: " + ex.getContentType(),
                        "UNSUPPORTED_MEDIA_TYPE"));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(ApiResponse.error(
                        "HTTP method '" + ex.getMethod() + "' is not supported for this endpoint",
                        "METHOD_NOT_ALLOWED"));
    }

    // ---------- Uploads ----------

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<Void>> handleUploadSize(MaxUploadSizeExceededException ex) {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(ApiResponse.error(
                        "Uploaded file exceeds the maximum allowed size",
                        "PAYLOAD_TOO_LARGE"));
    }

    // ---------- Persistence ----------

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataIntegrity(DataIntegrityViolationException ex) {
        String root = ex.getMostSpecificCause() != null
                ? ex.getMostSpecificCause().getMessage()
                : ex.getMessage();
        log.warn("Data integrity violation: {}", root);

        String message = "Data integrity violation";
        String code = "DATA_INTEGRITY";

        if (root != null) {
            String lower = root.toLowerCase();
            if (lower.contains("duplicate") || lower.contains("unique")) {
                message = "A record with the same unique value already exists";
                code = "DUPLICATE_KEY";
            } else if (lower.contains("foreign key") || lower.contains("cannot add or update a child row")) {
                message = "Referenced record does not exist or cannot be deleted due to related data";
                code = "FOREIGN_KEY_VIOLATION";
            } else if (lower.contains("cannot be null") || lower.contains("not null")) {
                message = "A required database field was null";
                code = "NOT_NULL_VIOLATION";
            }
        }

        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(message, code));
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleEntityNotFound(EntityNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(
                        ex.getMessage() != null ? ex.getMessage() : "Entity not found",
                        "NOT_FOUND"));
    }

    // ---------- Security ----------

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleDenied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error(
                        "Access denied. You do not have permission to access this resource.",
                        "FORBIDDEN"));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuthentication(AuthenticationException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error(
                        "Unauthorized access. Valid authentication is required.",
                        "UNAUTHORIZED"));
    }

    // ---------- 404 routing ----------

    @ExceptionHandler({NoHandlerFoundException.class, NoResourceFoundException.class})
    public ResponseEntity<ApiResponse<Void>> handleNotFound(Exception ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error("Endpoint not found", "ENDPOINT_NOT_FOUND"));
    }

    // ---------- Fallback ----------

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception ex) {
        log.error("Unhandled exception", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Internal server error", "INTERNAL_ERROR"));
    }
}
