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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.filter.CorsFilter;

import ingatlanok.common.JsonSupport;
import ingatlanok.config.RequestLoggingFilter;
import ingatlanok.config.SecurityHeadersFilter;
import ingatlanok.model.PropertyType;
import ingatlanok.model.SearchCriteria;
import ingatlanok.model.SearchPreset;
import ingatlanok.presentation.IngatlanComHtmlImportRequestDto;

@SpringBootTest
@DisplayName("RestApiControllerValidationTest - Külső API határ runtime validációs negatív tesztek (#403)")
class RestApiControllerValidationTest {

  @Autowired
  private WebApplicationContext webApplicationContext;

  @Autowired
  private SecurityHeadersFilter securityHeadersFilter;

  @Autowired
  private RequestLoggingFilter requestLoggingFilter;

  @Autowired
  private CorsFilter corsFilter;

  private MockMvc mockMvc;

  @BeforeEach
  void setupMockMvc() {
    mockMvc =
        MockMvcBuilders.webAppContextSetup(webApplicationContext)
            .addFilters(securityHeadersFilter, requestLoggingFilter, corsFilter)
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
        .andExpect(jsonPath("$.message").value("A minimum ár nem lehet negatív"));
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
        .andExpect(jsonPath("$.message").value("A minimum ár nem lehet nagyobb a maximum árnál"));
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
        .andExpect(jsonPath("$.message").value("A minimum alapterület nem lehet negatív"));
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
                .value("A minimum alapterület nem lehet nagyobb a maximum alapterületnél"));
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
            jsonPath("$.message").value("Ismeretlen portál azonosító a keresési feltételekben"));
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
        .andExpect(jsonPath("$.message").value("A minimum szobaszám nem lehet negatív"));
  }

  @Test
  @DisplayName("GET /api/history - Negatív lapozási limit 400 Bad Request-et ad")
  void testHistoryNegativeLimit() throws Exception {
    mockMvc
        .perform(get("/api/history").param("limit", "-5"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.message").value("A limit értéke csak pozitív egész szám lehet"));
  }

  @Test
  @DisplayName("GET /api/history - Negatív lapozási offset 400 Bad Request-et ad")
  void testHistoryNegativeOffset() throws Exception {
    mockMvc
        .perform(get("/api/history").param("offset", "-10"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.message").value("Az offset értéke nem lehet negatív"));
  }

  @Test
  @DisplayName("GET /api/history - Invertált ár szűrés 400 Bad Request-et ad")
  void testHistoryInvertedPrice() throws Exception {
    mockMvc
        .perform(get("/api/history").param("minPrice", "100").param("maxPrice", "40"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.message").value("A minimum ár nem lehet nagyobb a maximum árnál"));
  }

  @Test
  @DisplayName("GET /api/history - Ismeretlen portál paraméter 400 Bad Request-et ad")
  void testHistoryUnknownPortal() throws Exception {
    mockMvc
        .perform(get("/api/history").param("portal", "nemletezo_portal"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.message").value("Ismeretlen portál azonosító"));
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
        .andExpect(jsonPath("$.message").value("A minimum ár nem lehet negatív"));
  }

  @Test
  @DisplayName("PUT /api/presets/{id} - Negatív id és üres név 400, null név megengedett (404 ha nincs)")
  void testUpdatePresetNegativeIdAndPartialName() throws Exception {
    SearchPreset preset = new SearchPreset(null, "Módosított sablon", new SearchCriteria(), null, null, null);

    mockMvc
        .perform(
            put("/api/presets/-5")
                .contentType(MediaType.APPLICATION_JSON)
                .content(JsonSupport.mapper().writeValueAsString(preset)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.message").value("Érvénytelen preset azonosító"));

    SearchPreset blankNamePreset = new SearchPreset(null, "   ", new SearchCriteria(), null, null, null);
    mockMvc
        .perform(
            put("/api/presets/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(JsonSupport.mapper().writeValueAsString(blankNamePreset)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.message").value("A preset neve nem lehet üres!"));

    SearchPreset nullNamePreset = new SearchPreset(null, null, new SearchCriteria(), null, null, null);
    mockMvc
        .perform(
            put("/api/presets/999999")
                .contentType(MediaType.APPLICATION_JSON)
                .content(JsonSupport.mapper().writeValueAsString(nullNamePreset)))
        .andExpect(status().isNotFound());
  }

  @Test
  @DisplayName("DELETE /api/presets/{id} - Negatív id 400 Bad Request-et ad")
  void testDeletePresetNegativeId() throws Exception {
    mockMvc
        .perform(delete("/api/presets/-1"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.message").value("Érvénytelen preset azonosító"));
  }

  @Test
  @DisplayName("POST /api/presets/{id}/use - Negatív id 400 Bad Request-et ad")
  void testUsePresetNegativeId() throws Exception {
    mockMvc
        .perform(post("/api/presets/-10/use"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.message").value("Érvénytelen preset azonosító"));
  }

  @Test
  @DisplayName("POST /api/properties/{id}/hide - Üres azonosító 400 Bad Request-et ad")
  void testHidePropertyBlankId() throws Exception {
    mockMvc
        .perform(post("/api/properties/   /hide"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.message").value("Az azonosító nem lehet üres!"));
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

  @Test
  @DisplayName("GET /api/properties/* történeti és megfigyelési végpontok - hiányzó, üres és >1000 kulcs")
  void testPropertyLookupEndpointsValidation() throws Exception {
    String oversized = "x".repeat(1001);
    for (String suffix : List.of("price-history", "status-log", "observations")) {
      String queryPath = "/api/properties/" + suffix;
      expectBadRequest(get(queryPath), queryPath, "Hiányzó 'id' vagy 'url' paraméter!");
      expectBadRequest(
          get(queryPath).param("id", "   "), queryPath, "Hiányzó 'id' vagy 'url' paraméter!");
      expectBadRequest(
          get(queryPath).param("url", "   "), queryPath, "Hiányzó 'id' vagy 'url' paraméter!");
      expectBadRequest(
          get(queryPath).param("id", oversized),
          queryPath,
          "A megadott azonosító vagy URL túl hosszú!");
      expectBadRequest(
          get(queryPath).param("url", oversized),
          queryPath,
          "A megadott azonosító vagy URL túl hosszú!");

      String blankPath = "/api/properties/   /" + suffix;
      expectBadRequest(get(blankPath), "/api/properties/%20%20%20/" + suffix, "Hiányzó 'id' vagy 'url' paraméter!");
      String oversizedPath = "/api/properties/" + oversized + "/" + suffix;
      expectBadRequest(
          get(oversizedPath), oversizedPath, "A megadott azonosító vagy URL túl hosszú!");
    }
  }

  @Test
  @DisplayName("GET /api/properties/discounted, POST /items/viewed, /history/clear és /import-html")
  void testRemainingEndpointValidationBoundaries() throws Exception {
    String oversizedCity = "C".repeat(201);
    String oversizedUrl = "https://dh.hu/" + "a".repeat(987);
    String oversizedImportUrl = "https://ingatlan.com/" + "u".repeat(1980);

    expectBadRequest(
        get("/api/properties/discounted").param("city", oversizedCity),
        "/api/properties/discounted",
        "A település neve túl hosszú");

    mockMvc
        .perform(post("/api/items/viewed").contentType(MediaType.APPLICATION_JSON).content("{}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.marked").value(false));

    expectBadRequest(
        post("/api/items/viewed")
            .contentType(MediaType.APPLICATION_JSON)
            .content(JsonSupport.mapper().writeValueAsString(new MarkViewedRequest(oversizedUrl))),
        "/api/items/viewed",
        "Az URL túl hosszú!");

    expectBadRequest(
        post("/api/history/clear")
            .contentType(MediaType.APPLICATION_JSON)
            .content(JsonSupport.mapper().writeValueAsString(new ClearHistoryRequest(oversizedCity))),
        "/api/history/clear",
        "A település neve túl hosszú");
    expectBadRequest(
        post("/api/history/clear").param("city", oversizedCity),
        "/api/history/clear",
        "A település neve túl hosszú");

    expectBadRequest(
        post("/api/portals/ingatlancom/import-html").contentType(MediaType.APPLICATION_JSON),
        "/api/portals/ingatlancom/import-html",
        "Érvénytelen kéréstörzs (hibás JSON formátum)");
    expectBadRequest(
        post("/api/portals/ingatlancom/import-html")
            .contentType(MediaType.APPLICATION_JSON)
            .content(
                JsonSupport.mapper()
                    .writeValueAsString(
                        new IngatlanComHtmlImportRequestDto(
                            "<html></html>", null, oversizedCity, PropertyType.HAZ))),
        "/api/portals/ingatlancom/import-html",
        "A település neve túl hosszú");
    expectBadRequest(
        post("/api/portals/ingatlancom/import-html")
            .contentType(MediaType.APPLICATION_JSON)
            .content(
                JsonSupport.mapper()
                    .writeValueAsString(
                        new IngatlanComHtmlImportRequestDto(
                            "<html></html>", oversizedImportUrl, "Debrecen", PropertyType.HAZ))),
        "/api/portals/ingatlancom/import-html",
        "A forrás URL túl hosszú");
  }

  private void expectBadRequest(
      MockHttpServletRequestBuilder request, String expectedPath, String expectedMessage)
      throws Exception {
    mockMvc
        .perform(request)
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.error").value("Bad Request"))
        .andExpect(jsonPath("$.message").value(expectedMessage))
        .andExpect(jsonPath("$.path").value(expectedPath));
  }
}
