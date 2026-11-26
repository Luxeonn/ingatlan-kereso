package ingatlanok.portal.ingatlanbazar;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ingatlanok.core.DeduplicationEngine;
import ingatlanok.core.PropertyFilterEngine;
import ingatlanok.core.normalization.ListingNormalizer;
import ingatlanok.model.IngatlanItem;
import ingatlanok.model.PortalScrapeResult;
import ingatlanok.model.PortalScrapeStatus;
import ingatlanok.model.PropertyType;
import ingatlanok.model.SearchCriteria;
import ingatlanok.testsupport.TestFixtures;

class IngatlanBazarLiveSnapshotParsingTest {

  private static final Path FIXTURE_PATH =
      Path.of("src/test/resources/fixtures/ingatlanbazar/ingatlanbazar-search-debrecen.json");

  @Test
  @DisplayName("Élő Ingatlanbazár JSON snapshot parzolás, szűrés (39/50) és intraportál duplikáció-védelem")
  void testLiveSnapshotParsingFilterAndDedup() throws Exception {
    String json = Files.readString(FIXTURE_PATH, StandardCharsets.UTF_8);

    IngatlanBazarPortalEngine engine =
        new IngatlanBazarPortalEngine(new IngatlanBazarQueryBuilder(), new ListingNormalizer());
    SearchCriteria criteria = buildDebrecenHouseCriteria();

    PortalScrapeResult result =
        engine.scrape(criteria, url -> url.contains("page=1") ? json : "{\"hits\":[]}", 2);

    assertEquals(PortalScrapeStatus.SUCCESS, result.getStatus());
    assertEquals(50, result.getItems().size(), "Az Ingatlanbazár 1. oldal 50 nyers találatot ad vissza");

    assertTrue(
        result.getItems().stream().allMatch(item -> "Debrecen".equals(item.getCity())),
        "A városmező minden tételnél kanonikus 'Debrecen' maradjon");

    Map<String, IngatlanItem> byId = result.getItems().stream()
        .collect(Collectors.toMap(IngatlanItem::getId, Function.identity()));

    IngatlanItem hitWithUserTitle = byId.get("061862b7-c0c7-11f1-b393-bc2411815210");
    assertNotNull(hitWithUserTitle);
    assertEquals(115_000_000L, hitWithUserTitle.getPriceInfo().amountHuf());
    assertEquals(100.0, hitWithUserTitle.getDetailsInfo().areaSquareMeters());
    assertEquals(3.0, hitWithUserTitle.getDetailsInfo().roomCount());
    assertEquals("Debrecen", hitWithUserTitle.getCity());
    assertEquals("Debrecen, Veres Péter-kert", hitWithUserTitle.getLocation());
    assertTrue(hitWithUserTitle.getTitle().contains("Tulajdonostól"),
        "Az egyedi userTitle jelenjen meg a címben");

    IngatlanItem hitWithStreetOnly = byId.get("23f51207-2275-11f1-b6a6-005056b3506b");
    assertNotNull(hitWithStreetOnly);
    assertTrue(hitWithStreetOnly.getLocation().contains("Liget tér"),
        "Ha quarter null, az utcanév egészítse ki a helyszínt");

    PropertyFilterEngine filterEngine = new PropertyFilterEngine();
    PropertyFilterEngine.FilterResult filtered =
        filterEngine.filter(result.getItems(), criteria, "Debrecen");

    assertEquals(11, filtered.filteredByPriceCount(),
        "A 11 ártartományon kívüli (67.5M..125M Ft) hirdetés szűrésre kerül");
    assertEquals(39, filtered.matchingItems().size(),
        "39 hirdetés marad meg a 70-120M Ft szűrőn belül");

    DeduplicationEngine dedup = TestFixtures.deduplicationEngine();
    List<IngatlanItem> deduplicated = dedup.deduplicateMultiSignal(filtered.matchingItems());
    assertEquals(39, deduplicated.size(),
        "Sabloncímű Ingatlanbazár hirdetések nem olvadhatnak össze tévesen intraportálon belül");
  }

  private SearchCriteria buildDebrecenHouseCriteria() {
    SearchCriteria criteria = new SearchCriteria();
    criteria.setCity("Debrecen");
    criteria.setPropertyType(PropertyType.HAZ);
    criteria.setMinPriceMillion(70);
    criteria.setMaxPriceMillion(120);
    criteria.setMinArea(90);
    criteria.setMaxArea(200);
    return criteria;
  }
}
