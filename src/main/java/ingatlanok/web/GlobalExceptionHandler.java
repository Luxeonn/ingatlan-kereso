package ingatlanok.web;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import ingatlanok.common.AppLogger;
import ingatlanok.common.JsonSupport;

@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final AppLogger LOGGER = AppLogger.getLogger(GlobalExceptionHandler.class);

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ApiErrorResponse> handleIllegalArgument(
      IllegalArgumentException ex, HttpServletRequest request) {
    LOGGER.warn("Érvénytelen kérés: " + ex.getMessage());
    return buildErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), request.getRequestURI());
  }

  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  public ResponseEntity<ApiErrorResponse> handleTypeMismatch(
      MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
    String msg = "Érvénytelen paraméter típus: " + ex.getName();
    LOGGER.warn(msg);
    return buildErrorResponse(HttpStatus.BAD_REQUEST, msg, request.getRequestURI());
  }

  @ExceptionHandler(org.springframework.web.HttpRequestMethodNotSupportedException.class)
  public ResponseEntity<ApiErrorResponse> handleMethodNotSupported(
      org.springframework.web.HttpRequestMethodNotSupportedException ex,
      HttpServletRequest request) {
    String msg = "A kért HTTP metódus nem támogatott: " + ex.getMethod();
    LOGGER.warn(msg);
    return buildErrorResponse(HttpStatus.METHOD_NOT_ALLOWED, msg, request.getRequestURI());
  }

  @ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class)
  public ResponseEntity<ApiErrorResponse> handleMessageNotReadable(
      org.springframework.http.converter.HttpMessageNotReadableException ex,
      HttpServletRequest request) {
    Throwable cause = ex.getMostSpecificCause();
    String msg =
        (cause instanceof IllegalArgumentException && cause.getMessage() != null && !cause.getMessage().isBlank())
            ? cause.getMessage()
            : "Érvénytelen kéréstörzs (hibás JSON formátum)";
    LOGGER.warn(msg);
    return buildErrorResponse(HttpStatus.BAD_REQUEST, msg, request.getRequestURI());
  }

  @ExceptionHandler(org.springframework.web.HttpMediaTypeNotSupportedException.class)
  public ResponseEntity<ApiErrorResponse> handleMediaTypeNotSupported(
      org.springframework.web.HttpMediaTypeNotSupportedException ex, HttpServletRequest request) {
    String msg = "Nem támogatott média típus: " + ex.getContentType();
    LOGGER.warn(msg);
    return buildErrorResponse(HttpStatus.UNSUPPORTED_MEDIA_TYPE, msg, request.getRequestURI());
  }

  @ExceptionHandler(org.springframework.web.HttpMediaTypeNotAcceptableException.class)
  public ResponseEntity<String> handleMediaTypeNotAcceptable(
      org.springframework.web.HttpMediaTypeNotAcceptableException ex, HttpServletRequest request) {
    String msg = "Nem elfogadható média típus a kért erőforráshoz";
    LOGGER.warn(msg);
    ApiErrorResponse body =
        ApiErrorResponse.of(
            HttpStatus.NOT_ACCEPTABLE.value(),
            HttpStatus.NOT_ACCEPTABLE.getReasonPhrase(),
            msg,
            request.getRequestURI());
    return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE)
        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
        .body(JsonSupport.mapper().writeValueAsString(body));
  }

  @ExceptionHandler(org.springframework.web.bind.MissingServletRequestParameterException.class)
  public ResponseEntity<ApiErrorResponse> handleMissingParameter(
      org.springframework.web.bind.MissingServletRequestParameterException ex,
      HttpServletRequest request) {
    String msg = "Hiányzó kötelező kérés paraméter: " + ex.getParameterName();
    LOGGER.warn(msg);
    return buildErrorResponse(HttpStatus.BAD_REQUEST, msg, request.getRequestURI());
  }

  @ExceptionHandler(
      org.springframework.web.method.annotation.HandlerMethodValidationException.class)
  public ResponseEntity<ApiErrorResponse> handleValidationException(
      org.springframework.web.method.annotation.HandlerMethodValidationException ex,
      HttpServletRequest request) {
    String msg = "Érvénytelen kérés paraméterek";
    LOGGER.warn(msg);
    return buildErrorResponse(HttpStatus.BAD_REQUEST, msg, request.getRequestURI());
  }

  @ExceptionHandler(org.springframework.web.bind.MethodArgumentNotValidException.class)
  public ResponseEntity<ApiErrorResponse> handleMethodArgumentNotValid(
      org.springframework.web.bind.MethodArgumentNotValidException ex, HttpServletRequest request) {
    org.springframework.validation.FieldError fieldError = ex.getBindingResult().getFieldError();
    String msg =
        fieldError != null
            ? "Érvénytelen paraméter típus: " + fieldError.getField()
            : "Érvénytelen kérés paraméterek";
    LOGGER.warn(msg);
    return buildErrorResponse(HttpStatus.BAD_REQUEST, msg, request.getRequestURI());
  }

  @ExceptionHandler({
    org.springframework.web.servlet.resource.NoResourceFoundException.class,
    org.springframework.web.servlet.NoHandlerFoundException.class
  })
  public ResponseEntity<ApiErrorResponse> handleNoResourceFound(
      Exception ex, HttpServletRequest request) {
    String msg = "Erőforrás nem található: " + request.getRequestURI();
    LOGGER.warn(msg);
    return buildErrorResponse(HttpStatus.NOT_FOUND, msg, request.getRequestURI());
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiErrorResponse> handleGenericException(
      Exception ex, HttpServletRequest request) {
    LOGGER.error(
        "Váratlan hiba történt a(z) " + request.getRequestURI() + " kérés feldolgozása közben", ex);
    return buildErrorResponse(
        HttpStatus.INTERNAL_SERVER_ERROR, "Váratlan szerverhiba történt", request.getRequestURI());
  }

  private ResponseEntity<ApiErrorResponse> buildErrorResponse(
      HttpStatus status, String message, String path) {
    return ResponseEntity.status(status)
        .body(ApiErrorResponse.of(status.value(), status.getReasonPhrase(), message, path));
  }
}
