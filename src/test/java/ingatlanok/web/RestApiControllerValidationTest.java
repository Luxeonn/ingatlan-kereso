package ingatlanok.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import ingatlanok.common.JsonSupport;
import ingatlanok.config.GlobalCorsFilter;
import ingatlanok.model.SearchCriteria;
import ingatlanok.model.SearchPreset;

@SpringBootTest
@DisplayName("RestApiControllerValidationTest - Külső API határ runtime validációs negatív tesztek (#403)")
class RestApiControllerValidationTest {

  @Autowired
  private WebApplicationContext webApplicationContext;

  @Autowired
  private GlobalCorsFilter globalCorsFilter;

  private MockMvc mockMvc;

  @BeforeEach
  void setupMockMvc() {
    mockMvc =
        MockMvcBuilders.webAppContextSetup(webApplicationContext)
            .addFilters(globalCorsFilter)
            .build();
  }

  @Test
  @DisplayName("POST /api/search - Negatív minimum ár 400 Bad Request-et ad")
  void testSearchNegativeMinPrice() throws Exception {
    SearchCriteria criteria = new SearchCriteria();
    criteria.setMinPriceMillion(-10);

    mockMvc
        .perform(
            post("/api/search")
                .contentType(MediaType.APPLICATION_JSON)
                .content(JsonSupport.mapper().writeValueAsString(criteria)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.error").value("Bad Request"))
        .andExpect(jsonPath("$.message").value("A minimum ár nem lehet negatív: -10"));
  }

  @Test
  @DisplayName("POST /api/search - Invertált ár tartomány (min > max) 400 Bad Request-et ad")
  void testSearchInvertedPriceRange() throws Exception {
    SearchCriteria criteria = new SearchCriteria();
    criteria.setMinPriceMillion(150);
    criteria.setMaxPriceMillion(50);

    mockMvc
        .perform(
            post("/api/search")
                .contentType(MediaType.APPLICATION_JSON)
                .content(JsonSupport.mapper().writeValueAsString(criteria)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.message").value("A minimum ár nem lehet nagyobb a maximum árnál: 150 > 50"));
  }

  @Test
  @DisplayName("POST /api/search - Negatív terület 400 Bad Request-et ad")
  void testSearchNegativeArea() throws Exception {
    SearchCriteria criteria = new SearchCriteria();
    criteria.setMinArea(-20);

    mockMvc
        .perform(
            post("/api/search")
                .contentType(MediaType.APPLICATION_JSON)
                .content(JsonSupport.mapper().writeValueAsString(criteria)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.message").value("A minimum alapterület nem lehet negatív: -20"));
  }

  @Test
  @DisplayName("POST /api/search - Invertált alapterület tartomány (min > max) 400 Bad Request-et ad")
  void testSearchInvertedAreaRange() throws Exception {
    SearchCriteria criteria = new SearchCriteria();
    criteria.setMinArea(250);
    criteria.setMaxArea(100);

    mockMvc
        .perform(
            post("/api/search")
                .contentType(MediaType.APPLICATION_JSON)
                .content(JsonSupport.mapper().writeValueAsString(criteria)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(
            jsonPath("$.message")
                .value("A minimum alapterület nem lehet nagyobb a maximum alapterületnél: 250 > 100"));
  }

  @Test
  @DisplayName("POST /api/search - Ismeretlen portál a feltételekben 400 Bad Request-et ad")
  void testSearchUnknownPortal() throws Exception {
    SearchCriteria criteria = new SearchCriteria();
    criteria.setPortals(List.of("dh", "ismeretlen_portal"));

    mockMvc
        .perform(
            post("/api/search")
                .contentType(MediaType.APPLICATION_JSON)
                .content(JsonSupport.mapper().writeValueAsString(criteria)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(
            jsonPath("$.message").value("Ismeretlen portál azonosító a keresési feltételekben: ismeretlen_portal"));
  }

  @Test
  @DisplayName("POST /api/config - Érvénytelen keresési feltétel mentése 400 Bad Request-et ad")
  void testSaveConfigValidationFailure() throws Exception {
    SearchCriteria criteria = new SearchCriteria();
    criteria.setMinRooms(-5);

    mockMvc
        .perform(
            post("/api/config")
                .contentType(MediaType.APPLICATION_JSON)
                .content(JsonSupport.mapper().writeValueAsString(criteria)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.message").value("A minimum szobaszám nem lehet negatív: -5"));
  }

  @Test
  @DisplayName("GET /api/history - Negatív lapozási limit 400 Bad Request-et ad")
  void testHistoryNegativeLimit() throws Exception {
    mockMvc
        .perform(get("/api/history").param("limit", "-5"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.message").value("A limit értéke csak pozitív egész szám lehet: -5"));
  }

  @Test
  @DisplayName("GET /api/history - Negatív lapozási offset 400 Bad Request-et ad")
  void testHistoryNegativeOffset() throws Exception {
    mockMvc
        .perform(get("/api/history").param("offset", "-10"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.message").value("Az offset értéke nem lehet negatív: -10"));
  }

  @Test
  @DisplayName("GET /api/history - Invertált ár szűrés 400 Bad Request-et ad")
  void testHistoryInvertedPrice() throws Exception {
    mockMvc
        .perform(get("/api/history").param("minPrice", "100").param("maxPrice", "40"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.message").value("A minimum ár nem lehet nagyobb a maximum árnál: 100.0 > 40.0"));
  }

  @Test
  @DisplayName("GET /api/history - Ismeretlen portál paraméter 400 Bad Request-et ad")
  void testHistoryUnknownPortal() throws Exception {
    mockMvc
        .perform(get("/api/history").param("portal", "nemletezo_portal"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.message").value("Ismeretlen portál azonosító: nemletezo_portal"));
  }

  @Test
  @DisplayName("POST /api/presets - Üres névvel beküldött preset 400 Bad Request-et ad")
  void testCreatePresetEmptyName() throws Exception {
    SearchPreset preset = new SearchPreset(null, "   ", new SearchCriteria(), null, null, null);

    mockMvc
        .perform(
            post("/api/presets")
                .contentType(MediaType.APPLICATION_JSON)
                .content(JsonSupport.mapper().writeValueAsString(preset)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.message").value("A preset neve kötelező!"));
  }

  @Test
  @DisplayName("POST /api/presets - Érvénytelen feltételekkel beküldött preset 400 Bad Request-et ad")
  void testCreatePresetInvalidCriteria() throws Exception {
    SearchCriteria criteria = new SearchCriteria();
    criteria.setMinPriceMillion(-50);
    SearchPreset preset = new SearchPreset(null, "Érvénytelen sablon", criteria, null, null, null);

    mockMvc
        .perform(
            post("/api/presets")
                .contentType(MediaType.APPLICATION_JSON)
                .content(JsonSupport.mapper().writeValueAsString(preset)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.message").value("A minimum ár nem lehet negatív: -50"));
  }

  @Test
  @DisplayName("PUT /api/presets/{id} - Negatív id 400 Bad Request-et ad")
  void testUpdatePresetNegativeId() throws Exception {
    SearchPreset preset = new SearchPreset(null, "Módosított sablon", new SearchCriteria(), null, null, null);

    mockMvc
        .perform(
            put("/api/presets/-5")
                .contentType(MediaType.APPLICATION_JSON)
                .content(JsonSupport.mapper().writeValueAsString(preset)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.message").value("Érvénytelen preset azonosító: -5"));
  }

  @Test
  @DisplayName("DELETE /api/presets/{id} - Negatív id 400 Bad Request-et ad")
  void testDeletePresetNegativeId() throws Exception {
    mockMvc
        .perform(delete("/api/presets/-1"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.message").value("Érvénytelen preset azonosító: -1"));
  }

  @Test
  @DisplayName("POST /api/presets/{id}/use - Negatív id 400 Bad Request-et ad")
  void testUsePresetNegativeId() throws Exception {
    mockMvc
        .perform(post("/api/presets/-10/use"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.message").value("Érvénytelen preset azonosító: -10"));
  }

  @Test
  @DisplayName("GET /api/properties/price-history - Hiányzó azonosító vagy URL 400 Bad Request-et ad")
  void testPriceHistoryMissingKey() throws Exception {
    mockMvc
        .perform(get("/api/properties/price-history"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.message").value("Hiányzó 'id' vagy 'url' paraméter!"));
  }
}
