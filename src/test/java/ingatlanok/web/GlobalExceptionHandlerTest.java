package ingatlanok.web;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

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
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/logs");
        MethodArgumentTypeMismatchException ex = new MethodArgumentTypeMismatchException(
                "abc", Integer.class, "limit", null, null);

        ResponseEntity<Map<String, Object>> response = handler.handleTypeMismatch(ex, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(400, response.getBody().get("status"));
        assertEquals("Érvénytelen paraméter típus: limit", response.getBody().get("message"));
        assertEquals("/api/logs", response.getBody().get("path"));
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
}
