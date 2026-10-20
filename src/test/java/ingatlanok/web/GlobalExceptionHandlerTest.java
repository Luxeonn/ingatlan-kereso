package ingatlanok.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@DisplayName("GlobalExceptionHandlerTest - Globális REST kivételkezelő tesztek")
class GlobalExceptionHandlerTest {

  private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

  @Test
  @DisplayName("IllegalArgumentException kezelése - 400 Bad Request és strukturált JSON válasz")
  void testHandleIllegalArgument() {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/test");
    IllegalArgumentException ex = new IllegalArgumentException("Teszt érvénytelen paraméter");

    ResponseEntity<Map<String, Object>> response = handler.handleIllegalArgument(ex, request);

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals(400, response.getBody().get("status"));
    assertEquals("Bad Request", response.getBody().get("error"));
    assertEquals("Teszt érvénytelen paraméter", response.getBody().get("message"));
    assertEquals("/api/test", response.getBody().get("path"));
    assertNotNull(response.getBody().get("timestamp"));
  }

  @Test
  @DisplayName("MethodArgumentTypeMismatchException kezelése - 400 Bad Request típuseltérés esetén")
  void testHandleTypeMismatch() {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/test-param");
    MethodArgumentTypeMismatchException ex =
        new MethodArgumentTypeMismatchException("abc", Integer.class, "limit", null, null);

    ResponseEntity<Map<String, Object>> response = handler.handleTypeMismatch(ex, request);

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals(400, response.getBody().get("status"));
    assertEquals("Érvénytelen paraméter típus: limit", response.getBody().get("message"));
    assertEquals("/api/test-param", response.getBody().get("path"));
  }

  @Test
  @DisplayName("Generic Exception kezelése - 500 Internal Server Error strukturált JSON válasz")
  void testHandleGenericException() {
    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/search");
    RuntimeException ex = new RuntimeException("Adatbázis kapcsolat megszakadt");

    ResponseEntity<Map<String, Object>> response = handler.handleGenericException(ex, request);

    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals(500, response.getBody().get("status"));
    assertEquals("Internal Server Error", response.getBody().get("error"));
    assertEquals("Váratlan szerverhiba történt", response.getBody().get("message"));
    assertEquals("/api/search", response.getBody().get("path"));
  }

  @Test
  @DisplayName("NoResourceFoundException kezelése - 404 Not Found és strukturált JSON válasz")
  void testHandleNoResourceFound() {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/dashboard");
    org.springframework.web.servlet.resource.NoResourceFoundException ex =
        new org.springframework.web.servlet.resource.NoResourceFoundException(
            org.springframework.http.HttpMethod.GET,
            "/dashboard",
            "Erőforrás nem található: /dashboard");

    ResponseEntity<Map<String, Object>> response = handler.handleNoResourceFound(ex, request);

    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals(404, response.getBody().get("status"));
    assertEquals("Not Found", response.getBody().get("error"));
    assertEquals("Erőforrás nem található: /dashboard", response.getBody().get("message"));
    assertEquals("/dashboard", response.getBody().get("path"));
    assertNotNull(response.getBody().get("timestamp"));
  }

  @Test
  @DisplayName("NoHandlerFoundException kezelése - 404 Not Found és strukturált JSON válasz")
  void testHandleNoHandlerFound() {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/dashboard");
    org.springframework.web.servlet.NoHandlerFoundException ex =
        new org.springframework.web.servlet.NoHandlerFoundException(
            "GET", "/dashboard", new org.springframework.http.HttpHeaders());

    ResponseEntity<Map<String, Object>> response = handler.handleNoResourceFound(ex, request);

    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals(404, response.getBody().get("status"));
    assertEquals("Not Found", response.getBody().get("error"));
    assertEquals("Erőforrás nem található: /dashboard", response.getBody().get("message"));
    assertEquals("/dashboard", response.getBody().get("path"));
    assertNotNull(response.getBody().get("timestamp"));
  }

  @Test
  @DisplayName(
      "HttpRequestMethodNotSupportedException kezelése - 405 Method Not Allowed strukturált JSON"
          + " válasz")
  void testHandleMethodNotSupported() {
    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/status");
    org.springframework.web.HttpRequestMethodNotSupportedException ex =
        new org.springframework.web.HttpRequestMethodNotSupportedException("POST");

    ResponseEntity<Map<String, Object>> response = handler.handleMethodNotSupported(ex, request);

    assertEquals(HttpStatus.METHOD_NOT_ALLOWED, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals(405, response.getBody().get("status"));
    assertEquals("Method Not Allowed", response.getBody().get("error"));
    assertEquals("A kért HTTP metódus nem támogatott: POST", response.getBody().get("message"));
    assertEquals("/api/status", response.getBody().get("path"));
    assertNotNull(response.getBody().get("timestamp"));
  }

  @Test
  @DisplayName("HttpMessageNotReadableException kezelése - 400 Bad Request hibás JSON esetén")
  void testHandleMessageNotReadable() {
    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/search");
    org.springframework.http.converter.HttpMessageNotReadableException ex =
        new org.springframework.http.converter.HttpMessageNotReadableException(
            "JSON parse error", (org.springframework.http.HttpInputMessage) null);

    ResponseEntity<Map<String, Object>> response = handler.handleMessageNotReadable(ex, request);

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals(400, response.getBody().get("status"));
    assertEquals("Bad Request", response.getBody().get("error"));
    assertEquals("Érvénytelen kéréstörzs (hibás JSON formátum)", response.getBody().get("message"));
    assertEquals("/api/search", response.getBody().get("path"));
    assertNotNull(response.getBody().get("timestamp"));
  }

  @Test
  @DisplayName(
      "HttpMediaTypeNotSupportedException kezelése - 415 Unsupported Media Type strukturált JSON"
          + " válasz")
  void testHandleMediaTypeNotSupported() {
    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/search");
    org.springframework.web.HttpMediaTypeNotSupportedException ex =
        new org.springframework.web.HttpMediaTypeNotSupportedException(
            org.springframework.http.MediaType.TEXT_PLAIN,
            java.util.List.of(org.springframework.http.MediaType.APPLICATION_JSON));

    ResponseEntity<Map<String, Object>> response = handler.handleMediaTypeNotSupported(ex, request);

    assertEquals(HttpStatus.UNSUPPORTED_MEDIA_TYPE, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals(415, response.getBody().get("status"));
    assertEquals("Unsupported Media Type", response.getBody().get("error"));
    assertTrue(((String) response.getBody().get("message")).contains("text/plain"));
    assertEquals("/api/search", response.getBody().get("path"));
    assertNotNull(response.getBody().get("timestamp"));
  }

  @Test
  @DisplayName(
      "HttpMediaTypeNotAcceptableException kezelése - 406 Not Acceptable strukturált JSON válasz")
  void testHandleMediaTypeNotAcceptable() {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/status");
    org.springframework.web.HttpMediaTypeNotAcceptableException ex =
        new org.springframework.web.HttpMediaTypeNotAcceptableException(
            "No acceptable representation");

    ResponseEntity<String> response = handler.handleMediaTypeNotAcceptable(ex, request);

    assertEquals(HttpStatus.NOT_ACCEPTABLE, response.getStatusCode());
    assertNotNull(response.getBody());
    assertTrue(response.getBody().contains("\"status\":406"));
    assertTrue(response.getBody().contains("\"error\":\"Not Acceptable\""));
    assertTrue(response.getBody().contains("/api/status"));
  }

  @Test
  @DisplayName(
      "MissingServletRequestParameterException kezelése - 400 Bad Request strukturált JSON válasz")
  void testHandleMissingParameter() {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/test");
    org.springframework.web.bind.MissingServletRequestParameterException ex =
        new org.springframework.web.bind.MissingServletRequestParameterException(
            "requiredParam", "String");

    ResponseEntity<Map<String, Object>> response = handler.handleMissingParameter(ex, request);

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals(400, response.getBody().get("status"));
    assertEquals("Bad Request", response.getBody().get("error"));
    assertEquals(
        "Hiányzó kötelező kérés paraméter: requiredParam", response.getBody().get("message"));
    assertEquals("/api/test", response.getBody().get("path"));
    assertNotNull(response.getBody().get("timestamp"));
  }
}
