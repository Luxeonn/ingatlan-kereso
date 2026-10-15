package ingatlanok.web;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("SpaStaticServingIntegrationTest - Beágyazott SPA statikus kiszolgáló és CORS tesztek")
class SpaStaticServingIntegrationTest {

    private static WebServer webServer;
    private static int port;
    private static HttpClient client;

    @BeforeAll
    static void setUp() throws IOException {
        webServer = new WebServer(8995);
        webServer.start();
        port = webServer.getPort();
        client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    @AfterAll
    static void tearDown() {
        if (webServer != null) {
            webServer.stop();
        }
    }

    private String getBaseUrl() {
        return "http://127.0.0.1:" + port;
    }

    @Test
    @DisplayName("GET / kiszolgálja az SPA index.html-t és root tárolót")
    void testRootServingIndexHtml() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + "/"))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());
        assertTrue(response.headers().firstValue("Content-Type").orElse("").contains("text/html"));
        assertTrue(response.body().contains("<!DOCTYPE html>"));
        assertTrue(response.body().contains("id=\"root\""));
        assertTrue(response.headers().firstValue("Access-Control-Allow-Origin").isPresent());
    }

    @Test
    @DisplayName("CORS Preflight (OPTIONS /api/search) sikeres 204 választ és megfelelő fejléceket ad")
    void testCorsPreflight() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + "/api/search"))
                .method("OPTIONS", HttpRequest.BodyPublishers.noBody())
                .build();

        HttpResponse<Void> response = client.send(request, HttpResponse.BodyHandlers.discarding());
        assertEquals(204, response.statusCode());
        assertEquals("*", response.headers().firstValue("Access-Control-Allow-Origin").orElse(""));
        assertTrue(response.headers().firstValue("Access-Control-Allow-Methods").orElse("").contains("POST"));
        assertTrue(response.headers().firstValue("Access-Control-Allow-Headers").orElse("").contains("Content-Type"));
    }

    @Test
    @DisplayName("SPA Routing Fallback: ismeretlen útvonalra is az index.html-t adja vissza")
    void testSpaRoutingFallback() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + "/search/custom-route?city=Debrecen"))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());
        assertTrue(response.headers().firstValue("Content-Type").orElse("").contains("text/html"));
        assertTrue(response.body().contains("id=\"root\""));
    }

    @Test
    @DisplayName("Biztonság: Path Traversal kísérlet (..) azonnali 400 Bad Request-et kap")
    void testPathTraversalProtection() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + "/../../etc/passwd"))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(400, response.statusCode());
    }
}
