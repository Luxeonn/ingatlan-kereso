package ingatlanok.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import tools.jackson.databind.ObjectMapper;

import ingatlanok.common.AppLogger;

@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

  private static final AppLogger LOGGER = AppLogger.getLogger(GlobalExceptionHandler.class);

  private final ObjectMapper objectMapper;

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
        .body(objectMapper.writeValueAsString(body));
  }

  @ExceptionHandler(org.springframework.web.bind.MissingServletRequestParameterException.class)
  public ResponseEntity<ApiErrorResponse> handleMissingParameter(
      org.springframework.web.bind.MissingServletRequestParameterException ex,
      HttpServletRequest request) {
    String msg = "Hiányzó kötelező kérés paraméter: " + ex.getParameterName();
    LOGGER.warn(msg);
    return buildErrorResponse(HttpStatus.BAD_REQUEST, msg, request.getRequestURI());
  }

  @ExceptionHandler(HandlerMethodValidationException.class)
  public ResponseEntity<ApiErrorResponse> handleValidationException(
      HandlerMethodValidationException ex, HttpServletRequest request) {
    String msg =
        ex.getAllErrors().stream()
            .map(MessageSourceResolvable::getDefaultMessage)
            .filter(m -> m != null && !m.isBlank())
            .findFirst()
            .orElse("Érvénytelen kérés paraméterek");
    LOGGER.warn(msg);
    return buildErrorResponse(HttpStatus.BAD_REQUEST, msg, request.getRequestURI());
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiErrorResponse> handleMethodArgumentNotValid(
      MethodArgumentNotValidException ex, HttpServletRequest request) {
    String msg = resolveBindingValidationMessage(ex);
    LOGGER.warn(msg);
    return buildErrorResponse(HttpStatus.BAD_REQUEST, msg, request.getRequestURI());
  }

  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<ApiErrorResponse> handleConstraintViolation(
      ConstraintViolationException ex, HttpServletRequest request) {
    String msg =
        ex.getConstraintViolations().stream()
            .map(ConstraintViolation::getMessage)
            .filter(m -> m != null && !m.isBlank())
            .findFirst()
            .orElse("Érvénytelen kérés paraméterek");
    LOGGER.warn(msg);
    return buildErrorResponse(HttpStatus.BAD_REQUEST, msg, request.getRequestURI());
  }

  private String resolveBindingValidationMessage(MethodArgumentNotValidException ex) {
    FieldError fieldError = ex.getBindingResult().getFieldError();
    if (fieldError != null && fieldError.isBindingFailure()) {
      return "Érvénytelen paraméter típus: " + fieldError.getField();
    }
    if (fieldError != null
        && fieldError.getDefaultMessage() != null
        && !fieldError.getDefaultMessage().isBlank()) {
      return fieldError.getDefaultMessage();
    }
    ObjectError globalError = ex.getBindingResult().getGlobalError();
    if (globalError != null
        && globalError.getDefaultMessage() != null
        && !globalError.getDefaultMessage().isBlank()) {
      return globalError.getDefaultMessage();
    }
    return "Érvénytelen kérés paraméterek";
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
