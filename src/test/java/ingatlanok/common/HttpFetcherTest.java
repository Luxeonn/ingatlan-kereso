package ingatlanok.common;

import com.sun.net.httpserver.HttpServer;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class HttpFetcherTest {

    private static HttpServer testServer;
    private static int testPort;

    @BeforeAll
    static void startServer() throws IOException {
        testServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        testPort = testServer.getAddress().getPort();

        testServer.createContext("/test-html", exchange -> {
            byte[] response = "<html><head><title>Test Page</title></head><body><h1>Hello World</h1></body></html>".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
            exchange.sendResponseHeaders(200, response.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response);
            }
        });

        testServer.createContext("/test-json", exchange -> {
            byte[] response = "{\"status\":\"ok\",\"value\":42}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
            exchange.sendResponseHeaders(200, response.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response);
            }
        });

        testServer.createContext("/status-404", exchange -> {
            byte[] response = "Not Found".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(404, response.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response);
            }
        });

        testServer.createContext("/status-403", exchange -> {
            byte[] response = "Forbidden".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(403, response.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response);
            }
        });

        testServer.start();
    }

    @AfterAll
    static void stopServer() {
        if (testServer != null) {
            testServer.stop(0);
        }
    }

    private String getBaseUrl() {
        return "http://127.0.0.1:" + testPort;
    }

    @Test
    @DisplayName("Constants and headers validation")
    void testConstants() {
        assertNotNull(HttpFetcher.DEFAULT_USER_AGENT);
        assertTrue(HttpFetcher.DEFAULT_USER_AGENT.contains("Chrome"));
        assertTrue(HttpFetcher.DEFAULT_TIMEOUT_MS > 0);
    }

    @Test
    @DisplayName("fetchDocument returns null on invalid or blank URLs")
    void testFetchDocumentGracefulHandling() {
        assertNull(HttpFetcher.fetchDocument(null));
        assertNull(HttpFetcher.fetchDocument(""));
        assertNull(HttpFetcher.fetchDocument("   "));
        assertNull(HttpFetcher.fetchDocument("https://invalid-non-existing-host-987654321.test", 1000));
    }

    @Test
    @DisplayName("fetchJson returns null on invalid or blank URLs")
    void testFetchJsonGracefulHandling() {
        assertNull(HttpFetcher.fetchJson(null));
        assertNull(HttpFetcher.fetchJson(""));
        assertNull(HttpFetcher.fetchJson("   "));
        assertNull(HttpFetcher.fetchJson("https://invalid-non-existing-host-987654321.test", 1000));
    }

    @Test
    @DisplayName("fetchDocument successfully downloads HTML and parses Document")
    void testFetchDocumentSuccess() {
        Document doc = HttpFetcher.fetchDocument(getBaseUrl() + "/test-html");
        assertNotNull(doc);
        assertEquals("Test Page", doc.title());
        assertEquals("Hello World", doc.select("h1").text());
    }

    @Test
    @DisplayName("fetchDocument handles 404 and 403 gracefully with logging")
    void testFetchDocumentHttpStatusErrors() {
        assertNull(HttpFetcher.fetchDocument(getBaseUrl() + "/status-404"));
        assertNull(HttpFetcher.fetchDocument(getBaseUrl() + "/status-403"));
    }

    @Test
    @DisplayName("fetchJson successfully downloads JSON string")
    void testFetchJsonSuccess() {
        String json = HttpFetcher.fetchJson(getBaseUrl() + "/test-json");
        assertNotNull(json);
        assertTrue(json.contains("\"status\":\"ok\""));
        assertTrue(json.contains("42"));
    }

    @Test
    @DisplayName("fetchJson handles 404 and 403 gracefully with logging")
    void testFetchJsonHttpStatusErrors() {
        assertNull(HttpFetcher.fetchJson(getBaseUrl() + "/status-404"));
        assertNull(HttpFetcher.fetchJson(getBaseUrl() + "/status-403"));
    }
}
