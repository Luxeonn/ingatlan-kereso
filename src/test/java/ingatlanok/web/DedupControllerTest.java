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
import org.springframework.web.filter.CorsFilter;

import ingatlanok.common.JsonSupport;
import ingatlanok.config.RequestLoggingFilter;
import ingatlanok.config.SecurityHeadersFilter;
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
  @Autowired private SecurityHeadersFilter securityHeadersFilter;
  @Autowired private RequestLoggingFilter requestLoggingFilter;
  @Autowired private CorsFilter corsFilter;
  @Autowired private ListingStorage listingStorage;
  @Autowired private DedupOverrideStorage overrideStorage;

  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    mockMvc =
        MockMvcBuilders.webAppContextSetup(webApplicationContext)
            .addFilters(securityHeadersFilter, requestLoggingFilter, corsFilter)
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

  @Test
  @DisplayName("8 URL összevonása legfeljebb N-1 (7) FORCE_MERGE élt ment és tömeges lekérdezés is működik (#488)")
  void testLargeMergeUsesMinimalEdgesAndMultiUrlQuery() throws Exception {
    List<String> urls = new ArrayList<>();
    for (int i = 1; i <= 8; i++) {
      String url = "https://koltozzbe.hu/elado-ikerhaz-hajduszoboszlo/h1098410" + i;
      urls.add(url);
      seedListing(
          "kb-" + i,
          "koltozzbe",
          url,
          "Hajdúszoboszló",
          PropertyType.HAZ,
          35_000_000L + (i * 12_000_000L),
          70.0 + (i * 25.0));
    }

    mockMvc
        .perform(
            post("/api/dedup/merge")
                .contentType(MediaType.APPLICATION_JSON)
                .content(JsonSupport.mapper().writeValueAsString(new DedupMergeRequestDto(urls))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items", hasSize(1)))
        .andExpect(jsonPath("$.items[0].sourcesCount").value(8))
        .andExpect(jsonPath("$.items[0].manuallyMerged").value(true))
        .andExpect(jsonPath("$.overrides", hasSize(7)));

    List<DedupOverride> saved = overrideStorage.getAllOverrides();
    assertEquals(7, saved.size());
    assertTrue(saved.stream().allMatch(ov -> ov.type() == DedupOverrideType.FORCE_MERGE));

    // Batch GET /api/dedup/overrides?urls=... returns deduplicated overrides in one call
    mockMvc
        .perform(
            get("/api/dedup/overrides")
                .param("urls", urls.get(0))
                .param("urls", urls.get(1))
                .param("urls", urls.get(7)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(7)));

    // Splitting the star anchor detaches anchor while keeping remaining 7 peers merged
    mockMvc
        .perform(
            post("/api/dedup/split")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    JsonSupport.mapper()
                        .writeValueAsString(new DedupSplitRequestDto(urls.get(0), urls))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items", hasSize(2)));
  }

  @Test
  @DisplayName("Meglévő kézi klaszter bővítése csak 1 új élt hoz létre, és 1 él törlése szétválasztja a tagot (#488)")
  void testIncrementalMergeAndDeleteSeparation() throws Exception {
    List<String> urls = List.of(
        "https://koltozzbe.hu/h2001",
        "https://koltozzbe.hu/h2002",
        "https://koltozzbe.hu/h2003",
        "https://koltozzbe.hu/h2004");
    for (int i = 0; i < urls.size(); i++) {
      seedListing(
          "inc-" + i,
          "koltozzbe",
          urls.get(i),
          "Hajdúszoboszló",
          PropertyType.HAZ,
          40_000_000L + (i * 20_000_000L),
          80.0 + (i * 30.0));
    }

    // Merge first 3 -> 2 edges
    mockMvc
        .perform(
            post("/api/dedup/merge")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    JsonSupport.mapper()
                        .writeValueAsString(new DedupMergeRequestDto(urls.subList(0, 3)))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.overrides", hasSize(2)));
    assertEquals(2, overrideStorage.getAllOverrides().size());

    // Merge existing 3 + 4th URL -> only 1 new edge (total 3, not 6)
    mockMvc
        .perform(
            post("/api/dedup/merge")
                .contentType(MediaType.APPLICATION_JSON)
                .content(JsonSupport.mapper().writeValueAsString(new DedupMergeRequestDto(urls))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items", hasSize(1)))
        .andExpect(jsonPath("$.items[0].sourcesCount").value(4))
        .andExpect(jsonPath("$.overrides", hasSize(3)));
    assertEquals(3, overrideStorage.getAllOverrides().size());

    // Delete the edge connecting h2004 -> immediately separates h2004
    DedupOverride edgeTo4 =
        overrideStorage.getOverridesForUrl(urls.get(3)).stream().findFirst().orElseThrow();
    mockMvc
        .perform(delete("/api/dedup/overrides/{id}", edgeTo4.id()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items", hasSize(2)));

    // Missing query params returns 400
    mockMvc.perform(get("/api/dedup/overrides")).andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName(
      "GET /api/properties/hidden, /api/history és /api/properties/discounted alkalmazza a"
          + " FORCE_MERGE deduplikációt (#500)")
  void testArchiveEndpointsDeduplicateWithOverrides() throws Exception {
    String urlHiddenA = "https://dh.hu/hidden-merge-1";
    String urlHiddenB = "https://oc.hu/hidden-merge-2";
    seedListing("h1", "dh", urlHiddenA, "Debrecen", PropertyType.HAZ, 87_500_000L, 125.0);
    seedListing("h2", "oc", urlHiddenB, "Debrecen", PropertyType.HAZ, 89_000_000L, 125.0);
    // Hide both
    listingStorage.hideProperty(urlHiddenA);
    listingStorage.hideProperty(urlHiddenB);

    // Seed two active history items (distinct areas so heuristic won't merge, only FORCE_MERGE)
    String urlHistA = "https://dh.hu/hist-merge-1";
    String urlHistB = "https://oc.hu/hist-merge-2";
    seedListing("ha1", "dh", urlHistA, "Debrecen", PropertyType.LAKAS, 40_000_000L, 50.0);
    seedListing("ha2", "oc", urlHistB, "Debrecen", PropertyType.LAKAS, 95_000_000L, 130.0);

    // Seed two discounted active items
    String urlDiscA = "https://jofogas.hu/disc-merge-1";
    String urlDiscB = "https://koltozzbe.hu/disc-merge-2";
    seedListing("da1", "jofogas", urlDiscA, "Debrecen", PropertyType.HAZ, 70_000_000L, 100.0);
    seedListing("da2", "koltozzbe", urlDiscB, "Debrecen", PropertyType.HAZ, 75_000_000L, 160.0);
    // Re-save with lower prices to produce priceDelta < 0 for /api/properties/discounted
    seedListing("da1", "jofogas", urlDiscA, "Debrecen", PropertyType.HAZ, 65_000_000L, 100.0);
    seedListing("da2", "koltozzbe", urlDiscB, "Debrecen", PropertyType.HAZ, 70_000_000L, 160.0);

    // Force merge hidden pair, history pair, and discounted pair
    overrideStorage.saveOverride(urlHiddenA, urlHiddenB, DedupOverrideType.FORCE_MERGE, "debrecen");
    overrideStorage.saveOverride(urlHistA, urlHistB, DedupOverrideType.FORCE_MERGE, "debrecen");
    overrideStorage.saveOverride(urlDiscA, urlDiscB, DedupOverrideType.FORCE_MERGE, "debrecen");

    // 1. GET /api/properties/hidden returns 1 merged cluster with sourcesCount == 2
    mockMvc
        .perform(get("/api/properties/hidden"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(1)))
        .andExpect(jsonPath("$[0].sourcesCount").value(2))
        .andExpect(jsonPath("$[0].manuallyMerged").value(true));

    // 2. GET /api/history merges history pair and discounted pair into 2 clusters (instead of 4)
    mockMvc
        .perform(get("/api/history").param("city", "Debrecen"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(2)))
        .andExpect(jsonPath("$[0].sourcesCount").value(2))
        .andExpect(jsonPath("$[1].sourcesCount").value(2));

    // 3. GET /api/properties/discounted merges discounted pair into 1 cluster
    mockMvc
        .perform(get("/api/properties/discounted").param("city", "Debrecen"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(1)))
        .andExpect(jsonPath("$[0].sourcesCount").value(2))
        .andExpect(jsonPath("$[0].manuallyMerged").value(true));
  }
}
