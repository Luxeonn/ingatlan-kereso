package ingatlanok.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import ingatlanok.model.PropertyType;
import ingatlanok.presentation.IngatlanComHtmlImportRequestDto;

@SpringBootTest
@DisplayName("RestApiControllerImportHtmlTest - Ingatlan.com HTML import API tesztek (#455)")
class RestApiControllerImportHtmlTest {

  @Autowired private WebApplicationContext webApplicationContext;

  @Autowired private GlobalCorsFilter globalCorsFilter;

  private MockMvc mockMvc;

  @BeforeEach
  void setupMockMvc() {
    mockMvc =
        MockMvcBuilders.webAppContextSetup(webApplicationContext)
            .addFilters(globalCorsFilter)
            .build();
  }

  @Test
  @DisplayName("POST /api/portals/ingatlancom/import-html sikeres import és HTTP 200")
  void testImportHtmlEndpointSuccess() throws Exception {
    String html =
        """
        <html><body>
          <a href="/39990001" class="listing-card" data-listing-id="39990001">
            <span class="d-block fs-7 text-gray-900">Hajdúszoboszló, Bethlen utca 8.</span>
            <span class="fw-bold fs-5">92 M Ft</span>
            <span>Alapterület 130 m² Szobák 4</span>
          </a>
        </body></html>
        """;
    IngatlanComHtmlImportRequestDto request =
        new IngatlanComHtmlImportRequestDto(
            html,
            "https://ingatlan.com/lista/elado+haz+hajduszoboszlo?page=2",
            "Hajdúszoboszló",
            PropertyType.HAZ);

    mockMvc
        .perform(
            post("/api/portals/ingatlancom/import-html")
                .contentType(MediaType.APPLICATION_JSON)
                .content(JsonSupport.mapper().writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.parsedCount").value(1))
        .andExpect(jsonPath("$.savedCount").value(1))
        .andExpect(jsonPath("$.city").value("Hajdúszoboszló"))
        .andExpect(jsonPath("$.items[0].url").value("https://ingatlan.com/39990001"));
  }

  @Test
  @DisplayName("POST /api/portals/ingatlancom/import-html üres HTML vagy külső URL 400 Bad Request")
  void testImportHtmlEndpointValidationBadRequest() throws Exception {
    IngatlanComHtmlImportRequestDto emptyReq =
        new IngatlanComHtmlImportRequestDto("   ", null, "Debrecen", PropertyType.HAZ);

    mockMvc
        .perform(
            post("/api/portals/ingatlancom/import-html")
                .contentType(MediaType.APPLICATION_JSON)
                .content(JsonSupport.mapper().writeValueAsString(emptyReq)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400));

    IngatlanComHtmlImportRequestDto externalUrlReq =
        new IngatlanComHtmlImportRequestDto(
            "<html></html>", "https://attacker.test/page2", "Debrecen", PropertyType.HAZ);

    mockMvc
        .perform(
            post("/api/portals/ingatlancom/import-html")
                .contentType(MediaType.APPLICATION_JSON)
                .content(JsonSupport.mapper().writeValueAsString(externalUrlReq)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400));
  }
}
