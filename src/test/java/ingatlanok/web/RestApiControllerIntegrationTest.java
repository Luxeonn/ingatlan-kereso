package ingatlanok.web;

import com.google.gson.Gson;
import ingatlanok.config.GlobalCorsFilter;
import ingatlanok.model.IngatlanItem;
import ingatlanok.model.SearchCriteria;
import ingatlanok.persistence.ListingStorage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class RestApiControllerIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ListingStorage listingStorage;

    @Autowired
    private GlobalCorsFilter globalCorsFilter;

    private MockMvc mockMvc;
    private final Gson gson = new Gson();

    @BeforeEach
    void setupMockMvc() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .addFilters(globalCorsFilter)
                .build();
    }

    @Test
    @DisplayName("GET /api/status - Spring Boot státusz válasz ellenőrzése")
    void testGetStatus() throws Exception {
        mockMvc.perform(get("/api/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.totalInDb").exists())
                .andExpect(jsonPath("$.trackedCities").isArray());
    }

    @Test
    @DisplayName("GET /api/portals - Portál metaadatok lekérése")
    void testGetPortals() throws Exception {
        mockMvc.perform(get("/api/portals"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").exists());
    }

    @Test
    @DisplayName("GET /api/cities - Előre definiált városok lekérése")
    void testGetCities() throws Exception {
        mockMvc.perform(get("/api/cities"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.predefined").isArray())
                .andExpect(jsonPath("$.presets").isMap());
    }

    @Test
    @DisplayName("GET /api/config és POST /api/config - Keresési feltételek perzisztálása")
    void testConfigFlow() throws Exception {
        SearchCriteria criteria = new SearchCriteria();
        criteria.setCity("Debrecen");
        criteria.setMinPriceMillion(60);
        criteria.setMaxPriceMillion(110);

        mockMvc.perform(post("/api/config")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(gson.toJson(criteria)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(get("/api/config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.minPriceMillion").value(60))
                .andExpect(jsonPath("$.maxPriceMillion").value(110));
    }

    @Test
    @DisplayName("POST /api/search - Párhuzamos keresés lefutása és DTO válasz")
    void testSearchExecution() throws Exception {
        SearchCriteria criteria = new SearchCriteria();
        criteria.setCity("Debrecen");

        mockMvc.perform(post("/api/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(gson.toJson(criteria)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.portalStatuses").isArray());
    }

    @Test
    @DisplayName("POST /api/items/viewed és POST /api/open-urls - Megtekintett jelölés")
    void testMarkViewedAndOpenUrls() throws Exception {
        String testUrl = "https://teszt-ingatlan.hu/1";
        IngatlanItem item = new IngatlanItem("t1", "dh", testUrl, "Teszt lakás", "50 M Ft", "Debrecen", "60 m²", "2026-10-16", true);
        listingStorage.saveAll(List.of(item));

        mockMvc.perform(post("/api/items/viewed")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\":\"" + testUrl + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(post("/api/open-urls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[\"" + testUrl + "\"]"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.count").value(1));
    }

    @Test
    @DisplayName("GET /api/logs és POST /api/logs/clear - Naplózás ellenőrzése")
    void testLogsEndpoints() throws Exception {
        mockMvc.perform(get("/api/logs?limit=50&level=INFO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.logs").isArray())
                .andExpect(jsonPath("$.level").exists());

        mockMvc.perform(post("/api/logs/clear"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("GET /api/properties/price-history - Hiányzó paraméter esetén 400 Bad Request")
    void testGetPriceHistory_MissingParams_ReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/properties/price-history"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    @DisplayName("GET /api/properties/price-history - Érvényes ID és PathVariable esetén 200 OK")
    void testGetPriceHistory_ValidId() throws Exception {
        mockMvc.perform(get("/api/properties/price-history?id=t1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        mockMvc.perform(get("/api/properties/t1/price-history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("GET /api/properties/status-log - Hiányzó paraméter esetén 400 Bad Request")
    void testGetStatusLog_MissingParams_ReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/properties/status-log"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    @DisplayName("GET /api/properties/status-log - Érvényes ID és PathVariable esetén 200 OK")
    void testGetStatusLog_ValidId() throws Exception {
        mockMvc.perform(get("/api/properties/status-log?url=https://teszt.hu/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        mockMvc.perform(get("/api/properties/t1/status-log"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("GET /api/properties/discounted - Árcsökkentett ingatlanok lekérése")
    void testGetDiscountedProperties() throws Exception {
        mockMvc.perform(get("/api/properties/discounted"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        mockMvc.perform(get("/api/properties/discounted?city=Debrecen"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("GET /api/history - Szűrt keresési előzmények")
    void testGetHistoryWithFilters() throws Exception {
        mockMvc.perform(get("/api/history")
                        .param("city", "Debrecen")
                        .param("minPrice", "30.0")
                        .param("maxPrice", "90.0")
                        .param("minArea", "40.0")
                        .param("maxArea", "100.0")
                        .param("minRooms", "1.0")
                        .param("maxRooms", "3.0")
                        .param("sortBy", "PRICE_ASC")
                        .param("limit", "10")
                        .param("offset", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("POST /api/history/clear - Város és teljes előzmény törlése")
    void testClearHistory() throws Exception {
        mockMvc.perform(post("/api/history/clear?city=Debrecen"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clearedCity").value("Debrecen"));

        mockMvc.perform(post("/api/history/clear")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"city\":\"Szeged\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clearedCity").value("Szeged"));

        mockMvc.perform(post("/api/history/clear"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clearedAll").value(true));
    }

    @Test
    @DisplayName("CORS és biztonsági fejlécek - Localhost engedélyezett, külső origin tiltott")
    void testCorsAndSecurityHeaders() throws Exception {
        // Localhost origin kérés
        mockMvc.perform(get("/api/status")
                        .header("Origin", "http://localhost:3000"))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header()
                        .string("Access-Control-Allow-Origin", "http://localhost:3000"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header()
                        .string("Content-Security-Policy", org.hamcrest.Matchers.containsString("default-src 'self'")))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header()
                        .string("X-Content-Type-Options", "nosniff"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header()
                        .string("X-Frame-Options", "DENY"));

        // Preflight OPTIONS kérés engedélyezett originről
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options("/api/search")
                        .header("Origin", "http://127.0.0.1:8080")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header()
                        .string("Access-Control-Allow-Origin", "http://127.0.0.1:8080"));

        // Preflight OPTIONS kérés nem engedélyezett külső originről -> 403 Forbidden
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options("/api/search")
                        .header("Origin", "http://malicious-site.com")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/open-urls és /api/items/viewed - Üres és érvénytelen törzs kezelése")
    void testOpenUrlsAndMarkViewed_EdgeCases() throws Exception {
        mockMvc.perform(post("/api/open-urls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[]"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(0));

        mockMvc.perform(post("/api/items/viewed")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.marked").value(false));
    }

    @Test
    @DisplayName("GET /api/logs hibás típussal - GlobalExceptionHandler 400 Bad Request JSON választ ad")
    void testGetLogs_InvalidLimitParam_TriggersGlobalExceptionHandler() throws Exception {
        mockMvc.perform(get("/api/logs?limit=not-a-number"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("limit")))
                .andExpect(jsonPath("$.path").value("/api/logs"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("GET /dashboard - Spring MVC SPA router index.html fallback kiszolgálása")
    void testSpaFallbackRouting() throws Exception {
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                        .string(org.hamcrest.Matchers.containsString("id=\"root\"")));
    }
}
