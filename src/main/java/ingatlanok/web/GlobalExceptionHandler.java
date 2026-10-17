package ingatlanok.web;

import ingatlanok.common.AppLogger;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Global exception handler providing consistent, structured JSON responses
 * across all REST endpoints instead of HTML whitelabel error pages.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final AppLogger LOGGER = AppLogger.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest request) {
        LOGGER.warn("Érvénytelen kérés: " + ex.getMessage());
        return buildErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> handleTypeMismatch(MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        String msg = "Érvénytelen paraméter típus: " + ex.getName();
        LOGGER.warn(msg);
        return buildErrorResponse(HttpStatus.BAD_REQUEST, msg, request.getRequestURI());
    }

    @ExceptionHandler(org.springframework.web.HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Map<String, Object>> handleMethodNotSupported(org.springframework.web.HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {
        String msg = "A kért HTTP metódus nem támogatott: " + ex.getMethod();
        LOGGER.warn(msg);
        return buildErrorResponse(HttpStatus.METHOD_NOT_ALLOWED, msg, request.getRequestURI());
    }

    @ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleMessageNotReadable(org.springframework.http.converter.HttpMessageNotReadableException ex, HttpServletRequest request) {
        String msg = "Érvénytelen kéréstörzs (hibás JSON formátum)";
        LOGGER.warn(msg);
        return buildErrorResponse(HttpStatus.BAD_REQUEST, msg, request.getRequestURI());
    }

    @ExceptionHandler(org.springframework.web.HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<Map<String, Object>> handleMediaTypeNotSupported(org.springframework.web.HttpMediaTypeNotSupportedException ex, HttpServletRequest request) {
        String msg = "Nem támogatott média típus: " + ex.getContentType();
        LOGGER.warn(msg);
        return buildErrorResponse(HttpStatus.UNSUPPORTED_MEDIA_TYPE, msg, request.getRequestURI());
    }

    @ExceptionHandler(org.springframework.web.HttpMediaTypeNotAcceptableException.class)
    public ResponseEntity<String> handleMediaTypeNotAcceptable(org.springframework.web.HttpMediaTypeNotAcceptableException ex, HttpServletRequest request) {
        String msg = "Nem elfogadható média típus a kért erőforráshoz";
        LOGGER.warn(msg);
        String json = "{\"timestamp\":\"" + Instant.now() + "\",\"status\":406,\"error\":\"Not Acceptable\",\"message\":\""
                + msg + "\",\"path\":\"" + request.getRequestURI() + "\"}";
        return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE)
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .body(json);
    }

    @ExceptionHandler(org.springframework.web.bind.MissingServletRequestParameterException.class)
    public ResponseEntity<Map<String, Object>> handleMissingParameter(org.springframework.web.bind.MissingServletRequestParameterException ex, HttpServletRequest request) {
        String msg = "Hiányzó kötelező kérés paraméter: " + ex.getParameterName();
        LOGGER.warn(msg);
        return buildErrorResponse(HttpStatus.BAD_REQUEST, msg, request.getRequestURI());
    }

    @ExceptionHandler(org.springframework.web.method.annotation.HandlerMethodValidationException.class)
    public ResponseEntity<Map<String, Object>> handleValidationException(org.springframework.web.method.annotation.HandlerMethodValidationException ex, HttpServletRequest request) {
        String msg = "Érvénytelen kérés paraméterek";
        LOGGER.warn(msg);
        return buildErrorResponse(HttpStatus.BAD_REQUEST, msg, request.getRequestURI());
    }

    @ExceptionHandler({
        org.springframework.web.servlet.resource.NoResourceFoundException.class,
        org.springframework.web.servlet.NoHandlerFoundException.class
    })
    public ResponseEntity<Map<String, Object>> handleNoResourceFound(Exception ex, HttpServletRequest request) {
        String msg = "Erőforrás nem található: " + request.getRequestURI();
        LOGGER.warn(msg);
        return buildErrorResponse(HttpStatus.NOT_FOUND, msg, request.getRequestURI());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex, HttpServletRequest request) {
        LOGGER.error("Váratlan hiba történt a(z) " + request.getRequestURI() + " kérés feldolgozása közben", ex);
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Váratlan szerverhiba történt", request.getRequestURI());
    }

    private ResponseEntity<Map<String, Object>> buildErrorResponse(HttpStatus status, String message, String path) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);
        body.put("path", path);
        return ResponseEntity.status(status).body(body);
    }
}
