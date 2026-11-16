package ingatlanok.web;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
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
import ingatlanok.model.DedupOverride;
import ingatlanok.model.DedupOverrideType;
import ingatlanok.model.IngatlanItem;
import ingatlanok.model.PortalSource;
import ingatlanok.model.PriceInfo;
import ingatlanok.model.PropertyDetails;
import ingatlanok.model.PropertyType;
import ingatlanok.persistence.DedupOverrideStorage;
import ingatlanok.persistence.ListingStorage;
import ingatlanok.presentation.DedupMergeRequestDto;
import ingatlanok.presentation.DedupSplitRequestDto;

@SpringBootTest
@DisplayName("DedupController MockMvc Integration Tests")
class DedupControllerTest {

  @Autowired private WebApplicationContext webApplicationContext;
  @Autowired private GlobalCorsFilter globalCorsFilter;
  @Autowired private ListingStorage listingStorage;
  @Autowired private DedupOverrideStorage overrideStorage;

  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    mockMvc =
        MockMvcBuilders.webAppContextSetup(webApplicationContext)
            .addFilters(globalCorsFilter)
            .build();
    listingStorage.clearHistory();
    overrideStorage.clearAllOverrides();
  }

  private void seedListing(
      String id, String portal, String url, String city, PropertyType type, long priceHuf, double area) {
    IngatlanItem item =
        IngatlanItem.builder()
            .id(id)
            .source(PortalSource.of(portal))
            .url(url)
            .title("Kossuth utca 10 eladó ingatlan")
            .city(city)
            .location(city)
            .propertyType(type)
            .priceInfo(new PriceInfo((priceHuf / 1_000_000.0) + " M Ft", priceHuf, priceHuf / 1_000_000.0))
            .detailsInfo(new PropertyDetails(area + " m²", area, 2.0, Math.round((double) priceHuf / area)))
            .discoveredAt("2026-11-16 10:00:00")
            .imageUrl("https://cdn.dh.hu/shared-img.jpg")
            .build();
    listingStorage.saveAll(List.of(item));
  }

  @Test
  @DisplayName("POST /api/dedup/merge - Validációs hibák (ismeretlen URL, 1 elem, >30 elem, eltérő város)")
  void testMergeValidationErrors() throws Exception {
    seedListing("1", "dh", "https://dh.hu/m1", "Debrecen", PropertyType.LAKAS, 50_000_000L, 60.0);
    seedListing("2", "oc", "https://oc.hu/m2", "Eger", PropertyType.LAKAS, 50_000_000L, 60.0);

    // 1 item
    mockMvc
        .perform(
            post("/api/dedup/merge")
                .contentType(MediaType.APPLICATION_JSON)
                .content(JsonSupport.mapper().writeValueAsString(new DedupMergeRequestDto(List.of("https://dh.hu/m1")))))
        .andExpect(status().isBadRequest());

    // >30 items
    List<String> tooMany = new ArrayList<>();
    for (int i = 0; i < 31; i++) {
      tooMany.add("https://dh.hu/item-" + i);
    }
    mockMvc
        .perform(
            post("/api/dedup/merge")
                .contentType(MediaType.APPLICATION_JSON)
                .content(JsonSupport.mapper().writeValueAsString(new DedupMergeRequestDto(tooMany))))
        .andExpect(status().isBadRequest());

    // Unknown URL
    mockMvc
        .perform(
            post("/api/dedup/merge")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    JsonSupport.mapper()
                        .writeValueAsString(
                            new DedupMergeRequestDto(
                                List.of("https://dh.hu/m1", "https://dh.hu/unknown")))))
        .andExpect(status().isBadRequest());

    // Different cities
    mockMvc
        .perform(
            post("/api/dedup/merge")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    JsonSupport.mapper()
                        .writeValueAsString(
                            new DedupMergeRequestDto(
                                List.of("https://dh.hu/m1", "https://oc.hu/m2")))))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("Sikeres merge -> overrides lekérdezés -> visszavonás")
  void testMergeGetAndUndoFlow() throws Exception {
    seedListing("1", "dh", "https://dh.hu/f1", "Debrecen", PropertyType.LAKAS, 40_000_000L, 50.0);
    seedListing("2", "oc", "https://oc.hu/f2", "Debrecen", PropertyType.HAZ, 90_000_000L, 120.0);

    mockMvc
        .perform(
            post("/api/dedup/merge")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    JsonSupport.mapper()
                        .writeValueAsString(
                            new DedupMergeRequestDto(
                                List.of("https://dh.hu/f1", "https://oc.hu/f2")))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items", hasSize(1)))
        .andExpect(jsonPath("$.items[0].manuallyMerged").value(true))
        .andExpect(jsonPath("$.items[0].sourcesCount").value(2));

    mockMvc
        .perform(get("/api/dedup/overrides").param("url", "https://dh.hu/f1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(1)))
        .andExpect(jsonPath("$[0].type").value("FORCE_MERGE"));

    List<DedupOverride> saved = overrideStorage.getAllOverrides();
    assertEquals(1, saved.size());
    Long id = saved.get(0).id();

    mockMvc
        .perform(delete("/api/dedup/overrides/{id}", id))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items", hasSize(2)));

    assertTrue(overrideStorage.getAllOverrides().isEmpty());
  }

  @Test
  @DisplayName("Sikeres split szétválasztja a klasztert és menti a FORCE_SPLIT-et")
  void testSplitClusterSuccess() throws Exception {
    seedListing("1", "dh", "https://dh.hu/s1", "Debrecen", PropertyType.LAKAS, 50_000_000L, 65.0);
    seedListing("2", "oc", "https://oc.hu/s2", "Debrecen", PropertyType.LAKAS, 50_500_000L, 65.0);

    mockMvc
        .perform(
            post("/api/dedup/split")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    JsonSupport.mapper()
                        .writeValueAsString(
                            new DedupSplitRequestDto(
                                "https://dh.hu/s1",
                                List.of("https://dh.hu/s1", "https://oc.hu/s2")))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items", hasSize(2)))
        .andExpect(jsonPath("$.items[0].manuallySplit").value(true));

    List<DedupOverride> saved = overrideStorage.getAllOverrides();
    assertEquals(1, saved.size());
    assertEquals(DedupOverrideType.FORCE_SPLIT, saved.get(0).type());
  }
}
