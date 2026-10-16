package ingatlanok.web;

import com.google.gson.Gson;
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

    private MockMvc mockMvc;
    private final Gson gson = new Gson();

    @BeforeEach
    void setupMockMvc() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
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
        mockMvc.perform(get("/api/logs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.logs").isArray())
                .andExpect(jsonPath("$.level").exists());

        mockMvc.perform(post("/api/logs/clear"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
