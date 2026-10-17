package ingatlanok.web;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.*;
import java.lang.reflect.Field;
import java.net.Socket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("WebServerAdversarialStressTest - Empirical Challenger Stress Harness")
@SuppressWarnings("removal")
class WebServerAdversarialStressTest {

    private static WebServer webServer;
    private static int port;
    private static HttpClient client;
    private static final Gson GSON = new Gson();

    @BeforeAll
    static void setUp() throws IOException {
        webServer = new WebServer(8998);
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

    // =========================================================================
    // 1. DYNAMIC EPHEMERAL PORT BINDING (PORT 0) INVESTIGATION
    // =========================================================================

    @Test
    @DisplayName("Ephemeral port 0 binding behavior: WebServer constructor overrides 0 to default port")
    void testConstructorPortZeroResolution() {
        WebServer s = new WebServer("127.0.0.1", 0);
        // By design/contract: port <= 0 is resolved to resolveDefaultPort() (8080)
        assertEquals(8080, s.getPort(), "Constructor with port 0 must fall back to default port 8080");
    }

    @Test
    @DisplayName("Ephemeral port 0 binding via setPort(0): Probing actual socket bind and facade state")
    void testDynamicEphemeralPortBindingViaSetPort() throws Exception {
        WebServer ephemeralServer = new WebServer("127.0.0.1", 8999);
        ephemeralServer.setPort(0);
        try {
            ephemeralServer.start();

            // Extract the underlying HttpServer instance via reflection to inspect actual OS bind
            Field serverField = WebServer.class.getDeclaredField("server");
            serverField.setAccessible(true);
            HttpServer rawHttpServer = (HttpServer) serverField.get(ephemeralServer);
            assertNotNull(rawHttpServer, "Underlying HttpServer must be initialized");

            int actualOsBoundPort = rawHttpServer.getAddress().getPort();
            assertTrue(actualOsBoundPort > 0, "OS must bind to a positive ephemeral port (> 0)");

            // Empirical check on facade state: Does getPort() reflect the actual OS bound port?
            int facadeReportedPort = ephemeralServer.getPort();

            // Direct communication with the actual OS bound port
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create("http://127.0.0.1:" + actualOsBoundPort + "/api/status"))
                    .GET()
                    .build();
            HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
            assertEquals(200, resp.statusCode());

            JsonObject statusJson = JsonParser.parseString(resp.body()).getAsJsonObject();
            assertEquals("UP", statusJson.get("status").getAsString());

            int statusJsonPort = statusJson.get("port").getAsInt();

            // Document empirical behavior:
            // facadeReportedPort is 0 because WebServer.start() did not assign this.port = server.getAddress().getPort()
            // statusJsonPort is 0 because LegacyHttpHandler.register(server, port) received 0
            if (facadeReportedPort == 0) {
                System.out.println("[EMPIRICAL FINDING] WebServer.setPort(0) leaves facade getPort() as 0 instead of updating to actual OS bound port " + actualOsBoundPort);
                System.out.println("[EMPIRICAL FINDING] /api/status reports 'port': " + statusJsonPort + " instead of " + actualOsBoundPort);
            }
        } finally {
            ephemeralServer.stop();
        }
    }

    // =========================================================================
    // 2. PATH TRAVERSAL AND ARBITRARY RESOURCE ACCESS
    // =========================================================================

    @ParameterizedTest
    @ValueSource(strings = {
            "/../../etc/passwd",
            "/../../../etc/shadow",
            "/assets/../index.html",
            "/assets/../../build.gradle.kts",
            "/assets/../secrets.properties",
            "/assets/.env",
            "/assets/.git/config",
            "/assets/nested/file.js",
            "/assets/bundle.js%00.png",
            "//etc/passwd",
            "/..%2f..%2fetc%2fpasswd"
    })
    @DisplayName("Path traversal attacks via HttpClient must be rejected with 400 Bad Request")
    void testPathTraversalRejection(String malformedPath) throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + malformedPath))
                .GET()
                .build();
        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
        assertEquals(400, resp.statusCode(),
                "Path traversal attempt '" + malformedPath + "' must return 400 Bad Request, but got: " + resp.statusCode());
    }

    @Test
    @DisplayName("Path traversal with raw backslashes via TCP Socket returns 400 Bad Request")
    void testPathTraversalWithRawBackslashes() throws IOException {
        try (Socket socket = new Socket("127.0.0.1", port);
             OutputStream os = socket.getOutputStream();
             BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {

            String rawRequest = "GET /..\\..\\windows\\system32\\cmd.exe HTTP/1.1\r\n" +
                    "Host: 127.0.0.1:" + port + "\r\n" +
                    "Connection: close\r\n\r\n";
            os.write(rawRequest.getBytes(StandardCharsets.UTF_8));
            os.flush();

            String statusLine = reader.readLine();
            assertNotNull(statusLine, "Server should respond to raw request");
            assertTrue(statusLine.contains("400") || statusLine.contains("Bad Request"),
                    "Raw backslash traversal request must receive 400 Bad Request, got: " + statusLine);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/nonexistent-page.html",
            "/missing-resource.json",
            "/assets/non-existent-bundle.js",
            "/some-script.php"
    })
    @DisplayName("Non-existent static files with extensions must return 404 Not Found")
    void testNonExistentFilesReturn404(String missingPath) throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + missingPath))
                .GET()
                .build();
        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
        assertEquals(404, resp.statusCode(),
                "Missing file '" + missingPath + "' must return 404 Not Found");
    }

    // =========================================================================
    // 3. HTTP METHOD ENFORCEMENT (405 METHOD NOT ALLOWED)
    // =========================================================================

    @ParameterizedTest
    @ValueSource(strings = {
            "/",
            "/api/status",
            "/api/portals",
            "/api/cities",
            "/api/history",
            "/api/logs"
    })
    @DisplayName("GET-only endpoints must reject POST with 405 Method Not Allowed")
    void testGetOnlyEndpointsRejectPost(String endpoint) throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + endpoint))
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
        assertEquals(405, resp.statusCode(),
                "POST on GET-only endpoint '" + endpoint + "' must return 405 Method Not Allowed, got: " + resp.statusCode());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/api/search",
            "/api/open-urls",
            "/api/items/viewed",
            "/api/history/clear",
            "/api/logs/clear"
    })
    @DisplayName("POST-only endpoints must reject GET with 405 Method Not Allowed")
    void testPostOnlyEndpointsRejectGet(String endpoint) throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + endpoint))
                .GET()
                .build();
        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
        assertEquals(405, resp.statusCode(),
                "GET on POST-only endpoint '" + endpoint + "' must return 405 Method Not Allowed, got: " + resp.statusCode());
    }

    @Test
    @DisplayName("Unsupported HTTP methods (PUT, DELETE) on read-only endpoints return 405")
    void testUnsupportedHttpMethods() throws Exception {
        HttpRequest putReq = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + "/api/status"))
                .PUT(HttpRequest.BodyPublishers.noBody())
                .build();
        HttpResponse<String> putResp = client.send(putReq, HttpResponse.BodyHandlers.ofString());
        assertEquals(405, putResp.statusCode());

        HttpRequest deleteReq = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + "/api/cities"))
                .DELETE()
                .build();
        HttpResponse<String> deleteResp = client.send(deleteReq, HttpResponse.BodyHandlers.ofString());
        assertEquals(405, deleteResp.statusCode());
    }

    // =========================================================================
    // 4. CORS AND SECURITY HEADERS AUDIT
    // =========================================================================

    @ParameterizedTest
    @ValueSource(strings = {
            "/",
            "/api/status",
            "/api/search",
            "/api/config",
            "/api/history",
            "/api/portals",
            "/api/cities"
    })
    @DisplayName("All endpoints respond to CORS preflight OPTIONS with 204 No Content")
    void testCorsPreflightAcrossAllEndpoints(String endpoint) throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + endpoint))
                .method("OPTIONS", HttpRequest.BodyPublishers.noBody())
                .header("Origin", "http://localhost:5173")
                .header("Access-Control-Request-Method", "POST")
                .build();
        HttpResponse<Void> resp = client.send(req, HttpResponse.BodyHandlers.discarding());
        assertEquals(204, resp.statusCode(),
                "OPTIONS preflight on '" + endpoint + "' must return 204 No Content");
        assertEquals("*", resp.headers().firstValue("Access-Control-Allow-Origin").orElse(""));
        assertTrue(resp.headers().firstValue("Access-Control-Allow-Methods").orElse("").contains("GET"));
        assertTrue(resp.headers().firstValue("Access-Control-Allow-Methods").orElse("").contains("POST"));
        assertTrue(resp.headers().firstValue("Access-Control-Allow-Headers").orElse("").contains("Content-Type"));
    }

    @Test
    @DisplayName("Security headers present on standard responses")
    void testSecurityHeadersIntegrity() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + "/api/cities"))
                .GET()
                .build();
        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, resp.statusCode());

        assertEquals("nosniff", resp.headers().firstValue("X-Content-Type-Options").orElse(""));
        assertEquals("DENY", resp.headers().firstValue("X-Frame-Options").orElse(""));
        assertEquals("strict-origin-when-cross-origin", resp.headers().firstValue("Referrer-Policy").orElse(""));
        String csp = resp.headers().firstValue("Content-Security-Policy").orElse("");
        assertTrue(csp.contains("default-src 'self'"));
        assertFalse(csp.contains("googleapis.com"));
    }

    // =========================================================================
    // 5. CONCURRENCY & VIRTUAL THREAD STRESS HARNESS
    // =========================================================================

    @Test
    @DisplayName("Concurrency: 100 parallel requests handled gracefully by Virtual Threads")
    void testParallelVirtualThreadLoad() throws Exception {
        int concurrency = 100;
        try (ExecutorService pool = Executors.newVirtualThreadPerTaskExecutor()) {
            List<CompletableFuture<HttpResponse<String>>> futures = new ArrayList<>();
            for (int i = 0; i < concurrency; i++) {
                final int idx = i;
                futures.add(CompletableFuture.supplyAsync(() -> {
                    try {
                        String endpoint = (idx % 2 == 0) ? "/api/status" : "/api/cities";
                        HttpRequest req = HttpRequest.newBuilder()
                                .uri(URI.create(getBaseUrl() + endpoint))
                                .GET()
                                .build();
                        return client.send(req, HttpResponse.BodyHandlers.ofString());
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }, pool));
            }

            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

            for (CompletableFuture<HttpResponse<String>> f : futures) {
                HttpResponse<String> resp = f.get();
                assertEquals(200, resp.statusCode(), "Parallel request must succeed with 200");
                assertNotNull(resp.body());
                assertFalse(resp.body().isBlank());
            }
        }
    }

    // =========================================================================
    // 6. MALFORMED JSON BODY RESILIENCE (0-CRASH)
    // =========================================================================

    @Test
    @DisplayName("Malformed JSON payload on POST /api/search does not crash server")
    void testMalformedJsonPayloadOnSearch() throws Exception {
        String malformedJson = "{\"cities\": [\"Debrecen\", unclosed string}";
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + "/api/search"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(malformedJson))
                .build();

        try {
            HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
            assertTrue(resp.statusCode() >= 400, "Malformed JSON should return client/server error code, got: " + resp.statusCode());
        } catch (Exception ignored) {
        }

        // Liveness probe: WebServer must still be alive and responsive!
        HttpRequest ping = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + "/api/status"))
                .GET()
                .build();
        HttpResponse<String> pingResp = client.send(ping, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, pingResp.statusCode(), "Server must remain responsive after malformed payload");
    }
}
