package ingatlanok.web;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import ingatlanok.config.GlobalCorsFilter;
import ingatlanok.model.IngatlanItem;
import ingatlanok.persistence.ListingStorage;
import ingatlanok.testsupport.TestFixtures;

@SpringBootTest
@DisplayName("RestApiController Hide and Restore Tests (#318)")
class RestApiControllerHideTest {

  @Autowired private WebApplicationContext webApplicationContext;

  @Autowired private ListingStorage listingStorage;

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
  @DisplayName("POST /api/properties/{id}/hide és restore működése")
  void testHideAndRestorePropertyEndpoints() throws Exception {
    IngatlanItem item = TestFixtures.validResidentialItem("dh_api_hide_1");
    listingStorage.saveCityItems(item.getCity(), List.of(item));
    assertFalse(listingStorage.isHidden(item.getUrl()));

    String entityPropertyId = "Duna House_dh_api_hide_1";

    // 1. Hide property by propertyId
    mockMvc
        .perform(post("/api/properties/{id}/hide", entityPropertyId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(entityPropertyId))
        .andExpect(jsonPath("$.hidden").value(true))
        .andExpect(jsonPath("$.success").value(true));

    assertTrue(listingStorage.isHidden(item.getUrl()));

    // 2. GET /api/properties/hidden
    mockMvc
        .perform(get("/api/properties/hidden"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$[?(@.url == '" + item.getUrl() + "')]").exists());

    // 3. Restore property by propertyId
    mockMvc
        .perform(post("/api/properties/{id}/restore", entityPropertyId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(entityPropertyId))
        .andExpect(jsonPath("$.hidden").value(false))
        .andExpect(jsonPath("$.success").value(true));

    assertFalse(listingStorage.isHidden(item.getUrl()));
  }

  @Test
  @DisplayName("POST /api/properties/{id}/hide nem létező id-vel 404-et ad")
  void testHideNonExistentReturns404() throws Exception {
    mockMvc
        .perform(post("/api/properties/{id}/hide", "non_existent_id_9999"))
        .andExpect(status().isNotFound());
  }

  @Test
  @DisplayName("POST /api/properties/{id}/restore nem létező id-vel 404-et ad")
  void testRestoreNonExistentReturns404() throws Exception {
    mockMvc
        .perform(post("/api/properties/{id}/restore", "non_existent_id_9999"))
        .andExpect(status().isNotFound());
  }
}
