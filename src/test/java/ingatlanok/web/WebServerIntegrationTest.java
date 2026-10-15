package ingatlanok.web;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import ingatlanok.common.AppLogger;
import ingatlanok.model.IngatlanItem;
import ingatlanok.model.PriceInfo;
import ingatlanok.model.SearchCriteria;
import ingatlanok.persistence.IngatlanRepository;
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
        ingatlanok.portal.PortalEngine mockEngine = new ingatlanok.portal.PortalEngine() {
            @Override
            public ingatlanok.portal.PortalDescriptor getDescriptor() {
                return new ingatlanok.portal.PortalDescriptor("mockbazar", "Mock Ingatlanbazár", "#336699", "https://mockbazar.hu", true, List.of("lakas"), true);
            }
            @Override
            public ingatlanok.scraper.PortalScrapeResult scrape(SearchCriteria criteria) {
                IngatlanItem item = new IngatlanItem(
                        "mb1", ingatlanok.model.PortalSource.of("mockbazar"), "https://mockbazar.hu/1", "Debreceni lakás",
                        new PriceInfo("50 M Ft", 50_000_000L, 50.0),
                        "Debrecen",
                        new ingatlanok.model.PropertyDetails("100 m²", 100.0, 3.0, 500_000L),
                        "2026-10-15", true
                );
                return ingatlanok.scraper.PortalScrapeResult.success("mockbazar", "Mock Ingatlanbazár", List.of(item), 10);
            }
        };
        ingatlanok.portal.PortalEngineRegistry.getInstance().register(mockEngine);

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
        criteria.setPortals(List.of("mockbazar"));

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

    @Test
    @DisplayName("GET /api/logs returns recent structured logs")
    void testLogsEndpoint() throws Exception {
        AppLogger.getLogger("IntegrationLogTest").info("Teszt INFO naplóbejegyzés");
        AppLogger.getLogger("IntegrationLogTest").warn("Teszt WARN naplóbejegyzés");

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + "/api/logs?limit=50"))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());
        JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
        assertTrue(json.has("logs"));
        assertTrue(json.get("logs").isJsonArray());
        assertTrue(json.has("rawLogs"));
        assertTrue(json.has("level"));

        JsonArray logsArray = json.getAsJsonArray("logs");
        assertFalse(logsArray.isEmpty());
        JsonObject firstLog = logsArray.get(logsArray.size() - 1).getAsJsonObject();
        assertTrue(firstLog.has("level"));
        assertTrue(firstLog.has("message"));
        assertTrue(firstLog.has("timestamp"));

        HttpRequest infoFilterRequest = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + "/api/logs?limit=50&level=INFO"))
                .GET()
                .build();
        HttpResponse<String> infoResponse = client.send(infoFilterRequest, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, infoResponse.statusCode());
        JsonObject infoJson = JsonParser.parseString(infoResponse.body()).getAsJsonObject();
        JsonArray infoLogs = infoJson.getAsJsonArray("logs");
        assertTrue(infoLogs.size() > 0);
        for (int i = 0; i < infoLogs.size(); i++) {
            assertEquals("INFO", infoLogs.get(i).getAsJsonObject().get("level").getAsString());
        }

        HttpRequest postRequest = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + "/api/logs"))
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
        HttpResponse<String> postResponse = client.send(postRequest, HttpResponse.BodyHandlers.ofString());
        assertEquals(405, postResponse.statusCode());
    }

    @Test
    @DisplayName("POST /api/logs/clear clears recent structured logs and GET returns 405")
    void testLogsClearEndpoint() throws Exception {
        HttpRequest postRequest = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + "/api/logs/clear"))
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
        HttpResponse<String> postResponse = client.send(postRequest, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, postResponse.statusCode());
        JsonObject result = JsonParser.parseString(postResponse.body()).getAsJsonObject();
        assertTrue(result.get("success").getAsBoolean());

        HttpRequest getRequest = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + "/api/logs/clear"))
                .GET()
                .build();
        HttpResponse<String> getResponse = client.send(getRequest, HttpResponse.BodyHandlers.ofString());
        assertEquals(405, getResponse.statusCode());
    }

    @Test
    @DisplayName("GET /api/portals returns registered portal descriptors and POST returns 405")
    void testPortalsEndpoint() throws Exception {
        HttpRequest getRequest = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + "/api/portals"))
                .GET()
                .build();
        HttpResponse<String> response = client.send(getRequest, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("Duna House"));
        assertTrue(response.body().contains("Otthon Centrum"));
        assertTrue(response.body().contains("dh"));
        assertTrue(response.body().contains("oc"));
        assertTrue(response.body().contains("savedListingCount"));

        HttpRequest postRequest = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + "/api/portals"))
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
        HttpResponse<String> postResponse = client.send(postRequest, HttpResponse.BodyHandlers.ofString());
        assertEquals(405, postResponse.statusCode());
    }

    @Test
    @DisplayName("WebServer responses include hardened security headers")
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

    @Test
    @DisplayName("GET /api/properties/price-history and /api/properties/status-log return history entries")
    void testPropertiesPriceHistoryAndStatusEndpoints() throws Exception {
        String testUrl = "https://dh.hu/api-history-test-1";
        IngatlanItem item1 = new IngatlanItem("api-1", "Duna House", testUrl, "API Teszt Lakás", "50 M Ft", "Debrecen", "Debrecen", "50 m²", "2026-10-14 10:00:00", true);
        item1.setPriceInfo(new PriceInfo("50 M Ft", 50_000_000L, 50.0));
        IngatlanRepository.getInstance().saveCityItems("Debrecen", List.of(item1));

        // Árcsökkentés 45 M Ft-ra
        IngatlanItem itemDrop = new IngatlanItem("api-1", "Duna House", testUrl, "API Teszt Lakás", "45 M Ft", "Debrecen", "Debrecen", "50 m²", "2026-10-14 12:00:00", false);
        itemDrop.setPriceInfo(new PriceInfo("45 M Ft", 45_000_000L, 45.0));
        IngatlanRepository.getInstance().saveCityItems("Debrecen", List.of(itemDrop));

        // 1. Ártörténet lekérdezése query paraméterrel (url)
        HttpRequest reqHistoryUrl = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + "/api/properties/price-history?url=" + java.net.URLEncoder.encode(testUrl, java.nio.charset.StandardCharsets.UTF_8)))
                .GET()
                .build();
        HttpResponse<String> respHistoryUrl = client.send(reqHistoryUrl, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, respHistoryUrl.statusCode());
        JsonArray historyArray = JsonParser.parseString(respHistoryUrl.body()).getAsJsonArray();
        assertEquals(2, historyArray.size());
        assertEquals(50_000_000L, historyArray.get(0).getAsJsonObject().get("priceAmount").getAsLong());
        assertEquals(45_000_000L, historyArray.get(1).getAsJsonObject().get("priceAmount").getAsLong());
        assertEquals(-5_000_000L, historyArray.get(1).getAsJsonObject().get("priceDelta").getAsLong());

        // 2. Státusznapló lekérdezése
        HttpRequest reqStatusUrl = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + "/api/properties/status-log?url=" + java.net.URLEncoder.encode(testUrl, java.nio.charset.StandardCharsets.UTF_8)))
                .GET()
                .build();
        HttpResponse<String> respStatusUrl = client.send(reqStatusUrl, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, respStatusUrl.statusCode());
        JsonArray statusArray = JsonParser.parseString(respStatusUrl.body()).getAsJsonArray();
        assertEquals(2, statusArray.size());
        assertEquals("ACTIVE", statusArray.get(0).getAsJsonObject().get("status").getAsString());
        assertEquals("PRICE_DROP", statusArray.get(1).getAsJsonObject().get("status").getAsString());

        // 3. /api/history?onlyDiscounted=true szűrés
        HttpRequest reqDiscounted = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + "/api/history?city=Debrecen&onlyDiscounted=true"))
                .GET()
                .build();
        HttpResponse<String> respDiscounted = client.send(reqDiscounted, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, respDiscounted.statusCode());
        JsonArray discountedArray = JsonParser.parseString(respDiscounted.body()).getAsJsonArray();
        assertTrue(discountedArray.size() >= 1);
        boolean foundTestItem = false;
        for (int i = 0; i < discountedArray.size(); i++) {
            if (testUrl.equals(discountedArray.get(i).getAsJsonObject().get("url").getAsString())) {
                foundTestItem = true;
                assertEquals(-5_000_000L, discountedArray.get(i).getAsJsonObject().get("priceDelta").getAsLong());
            }
        }
        assertTrue(foundTestItem, "A leárazott hirdetésnek meg kell jelennie a listában");

        // 4. /api/properties/discounted végpont
        HttpRequest reqPropsDiscounted = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + "/api/properties/discounted?city=Debrecen"))
                .GET()
                .build();
        HttpResponse<String> respPropsDiscounted = client.send(reqPropsDiscounted, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, respPropsDiscounted.statusCode());
        JsonArray propsDiscountedArray = JsonParser.parseString(respPropsDiscounted.body()).getAsJsonArray();
        assertTrue(propsDiscountedArray.size() >= 1);
    }
}
