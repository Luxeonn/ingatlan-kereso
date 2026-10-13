package ingatlanok.web;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import ingatlanok.model.SearchCriteria;
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
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class WebServerIntegrationTest {

    private static WebServer webServer;
    private static int port;
    private static HttpClient client;
    private static final Gson GSON = new Gson();

    @BeforeAll
    static void setUp() throws IOException {
        webServer = new WebServer(8990);
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
    @DisplayName("GET / returns 200 and HTML content")
    void testRootEndpoint() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + "/"))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());
        assertTrue(response.headers().firstValue("Content-Type").orElse("").contains("text/html"));
        assertTrue(response.body().contains("<!DOCTYPE html>"));
        assertTrue(response.body().contains("Ingatlan Kereső"));
    }

    @Test
    @DisplayName("POST / returns 405 Method Not Allowed")
    void testRootMethodNotAllowed() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + "/"))
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(405, response.statusCode());
    }

    @Test
    @DisplayName("GET /api/status returns server info")
    void testStatusEndpoint() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + "/api/status"))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());
        JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
        assertEquals("UP", json.get("status").getAsString());
        assertEquals(port, json.get("port").getAsInt());
    }

    @Test
    @DisplayName("GET /api/config returns default or loaded configuration")
    void testConfigEndpoint() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + "/api/config"))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());
        JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
        assertTrue(json.has("city") || json.has("cities"));
    }

    @Test
    @DisplayName("GET /api/cities returns Hungarian cities list and presets")
    void testCitiesEndpoint() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + "/api/cities"))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());
        JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
        assertTrue(json.has("predefined"));
        assertTrue(json.get("predefined").isJsonArray());
        assertTrue(json.get("predefined").getAsJsonArray().toString().contains("Debrecen"));
    }

    @Test
    @DisplayName("GET /api/history returns stored properties")
    void testHistoryEndpoint() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + "/api/history?city=Debrecen"))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());
        assertNotNull(response.body());
    }

    @Test
    @DisplayName("POST /api/open-urls handles URL opening limits")
    void testOpenUrlsEndpoint() throws Exception {
        String jsonPayload = GSON.toJson(List.of("https://dh.hu", "https://oc.hu"));
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + "/api/open-urls?max=1"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());
        JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
        assertTrue(json.get("success").getAsBoolean());
        assertEquals(2, json.get("count").getAsInt());
    }

    @Test
    @DisplayName("POST /api/history/clear handles clearing")
    void testClearHistoryEndpoint() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + "/api/history/clear"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{}"))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());
        JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
        assertTrue(json.get("success").getAsBoolean());
    }

    @Test
    @DisplayName("POST /api/search executes query and returns structured result")
    void testSearchEndpoint() throws Exception {
        SearchCriteria criteria = new SearchCriteria();
        criteria.setCities(List.of("Debrecen"));
        criteria.setPropertyType("lakas");
        criteria.setMinPriceMillion(40);
        criteria.setMaxPriceMillion(60);
        criteria.setPortals(List.of("ingatlanbazar"));

        String payload = GSON.toJson(criteria);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + "/api/search"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());
        JsonObject result = JsonParser.parseString(response.body()).getAsJsonObject();
        assertTrue(result.has("items"));
        assertTrue(result.has("portalCounts"));
    }

    @Test
    @DisplayName("POST /api/items/viewed marks an item as viewed")
    void testMarkItemViewedEndpoint() throws Exception {
        String payload = GSON.toJson(Map.of("url", "https://dh.hu/ingatlan/test-viewed-1"));
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + "/api/items/viewed"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());
        JsonObject result = JsonParser.parseString(response.body()).getAsJsonObject();
        assertTrue(result.get("success").getAsBoolean());
        assertEquals("https://dh.hu/ingatlan/test-viewed-1", result.get("url").getAsString());

        HttpRequest getRequest = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + "/api/items/viewed"))
                .GET()
                .build();
        HttpResponse<String> methodNotAllowedResponse = client.send(getRequest, HttpResponse.BodyHandlers.ofString());
        assertEquals(405, methodNotAllowedResponse.statusCode());
    }
}
