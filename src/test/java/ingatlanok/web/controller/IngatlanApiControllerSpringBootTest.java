package ingatlanok.web.controller;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import ingatlanok.Main;
import ingatlanok.model.SearchCriteria;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = Main.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class IngatlanApiControllerSpringBootTest {

    @LocalServerPort
    private int port;

    private static final Gson GSON = new Gson();
    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    private String getBaseUrl() {
        return "http://127.0.0.1:" + port;
    }

    @Test
    @DisplayName("GET / returns 200 and HTML content from Spring Boot")
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
    @DisplayName("GET /api/status returns UP status and port")
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
    @DisplayName("GET /api/portals returns registered portal descriptors")
    void testPortalsEndpoint() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + "/api/portals"))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("Duna House"));
        assertTrue(response.body().contains("Otthon Centrum"));
        assertTrue(response.body().contains("dh"));
        assertTrue(response.body().contains("oc"));
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
    @DisplayName("GET and POST /api/config manages criteria configuration")
    void testConfigEndpoint() throws Exception {
        HttpRequest getRequest = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + "/api/config"))
                .GET()
                .build();

        HttpResponse<String> getResponse = client.send(getRequest, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, getResponse.statusCode());

        SearchCriteria sc = new SearchCriteria();
        sc.setCities(List.of("Szeged"));
        sc.setPropertyType("haz");

        HttpRequest postRequest = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + "/api/config"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(GSON.toJson(sc)))
                .build();

        HttpResponse<String> postResponse = client.send(postRequest, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, postResponse.statusCode());
        JsonObject json = JsonParser.parseString(postResponse.body()).getAsJsonObject();
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

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + "/api/search"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(GSON.toJson(criteria)))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());
        JsonObject result = JsonParser.parseString(response.body()).getAsJsonObject();
        assertTrue(result.has("items"));
        assertTrue(result.has("portalCounts"));
    }

    @Test
    @DisplayName("POST /api/open-urls marks urls viewed")
    void testOpenUrlsEndpoint() throws Exception {
        String payload = GSON.toJson(List.of("https://dh.hu/test1", "https://oc.hu/test2"));
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + "/api/open-urls"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());
        JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
        assertTrue(json.get("success").getAsBoolean());
        assertEquals(2, json.get("count").getAsInt());
    }

    @Test
    @DisplayName("POST /api/items/viewed marks an item as viewed")
    void testMarkItemViewedEndpoint() throws Exception {
        String payload = GSON.toJson(Map.of("url", "https://dh.hu/ingatlan/spring-viewed-1"));
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + "/api/items/viewed"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());
        JsonObject result = JsonParser.parseString(response.body()).getAsJsonObject();
        assertTrue(result.get("success").getAsBoolean());
    }

    @Test
    @DisplayName("GET /api/logs and POST /api/logs/clear manage logs")
    void testLogsEndpoints() throws Exception {
        HttpRequest getRequest = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + "/api/logs?limit=50"))
                .GET()
                .build();

        HttpResponse<String> getResponse = client.send(getRequest, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, getResponse.statusCode());
        JsonObject json = JsonParser.parseString(getResponse.body()).getAsJsonObject();
        assertTrue(json.has("logs"));

        HttpRequest clearRequest = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + "/api/logs/clear"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();

        HttpResponse<String> clearResponse = client.send(clearRequest, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, clearResponse.statusCode());
    }

    @Test
    @DisplayName("Spring Boot responses include hardened security headers")
    void testSecurityHeaders() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + "/api/status"))
                .GET()
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals("nosniff", response.headers().firstValue("X-Content-Type-Options").orElse(""));
        assertEquals("DENY", response.headers().firstValue("X-Frame-Options").orElse(""));
        assertEquals("strict-origin-when-cross-origin", response.headers().firstValue("Referrer-Policy").orElse(""));
        assertTrue(response.headers().firstValue("Content-Security-Policy").orElse("").contains("default-src 'self'"));
    }
}
