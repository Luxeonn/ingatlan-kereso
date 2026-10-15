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

    @Test
    @DisplayName("Biztonság: Content-Security-Policy fejléc nem tartalmaz külső CDN függőséget")
    void testContentSecurityPolicyZeroExternalCdn() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + "/"))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());
        String csp = response.headers().firstValue("Content-Security-Policy").orElse("");
        assertFalse(csp.contains("fonts.googleapis.com"), "CSP nem tartalmazhat külső Google Fonts domaint");
        assertFalse(csp.contains("fonts.gstatic.com"), "CSP nem tartalmazhat külső gstatic domaint");
        assertTrue(csp.contains("default-src 'self'"));
    }

    @Test
    @DisplayName("Gzip előtömörítés: Accept-Encoding: gzip fejléc esetén Content-Encoding: gzip választ kapunk")
    void testGzipPrecompressedServing() throws Exception {
        HttpRequest htmlRequest = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + "/"))
                .GET()
                .build();
        HttpResponse<String> htmlResponse = client.send(htmlRequest, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, htmlResponse.statusCode());

        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("/assets/index-[a-zA-Z0-9_-]+\\.js").matcher(htmlResponse.body());
        assertTrue(matcher.find(), "index.html tartalmazza a JS bundle elérési útját");
        String jsPath = matcher.group();

        // 1. Kérés Accept-Encoding nélkül
        HttpRequest uncompressedReq = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + jsPath))
                .GET()
                .build();
        HttpResponse<byte[]> uncompressedResp = client.send(uncompressedReq, HttpResponse.BodyHandlers.ofByteArray());
        assertEquals(200, uncompressedResp.statusCode());
        assertTrue(uncompressedResp.headers().firstValue("Content-Type").orElse("").contains("javascript"));
        assertTrue(uncompressedResp.headers().firstValue("Content-Encoding").isEmpty());

        // 2. Kérés Accept-Encoding: gzip fejléccel
        HttpRequest gzipReq = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + jsPath))
                .header("Accept-Encoding", "gzip, deflate, br")
                .GET()
                .build();
        HttpResponse<byte[]> gzipResp = client.send(gzipReq, HttpResponse.BodyHandlers.ofByteArray());
        assertEquals(200, gzipResp.statusCode());
        assertEquals("gzip", gzipResp.headers().firstValue("Content-Encoding").orElse(""));
        assertTrue(gzipResp.headers().firstValue("Content-Type").orElse("").contains("javascript"));
        assertTrue(gzipResp.headers().firstValue("Cache-Control").orElse("").contains("immutable"));
        assertTrue(gzipResp.body().length < uncompressedResp.body().length, "A gzip tömörített tartalomnak kisebbnek kell lennie");
    }

    @Test
    @DisplayName("Ismeretlen statikus asset fájl kérése 404 Not Found hibát ad, nem index.html-t")
    void testMissingAssetReturns404() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + "/assets/non-existent-bundle-999.js"))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(404, response.statusCode());
    }

    @Test
    @DisplayName("Biztonság: érvénytelen karaktereket tartalmazó asset útvonal 400 Bad Request-et kap")
    void testInvalidAssetFileNamePath() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + "/assets/../../pom.xml"))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(400, response.statusCode());
    }
}
