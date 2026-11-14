package ingatlanok.portal.jofogas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ingatlanok.core.normalization.ListingNormalizer;
import ingatlanok.model.IngatlanItem;
import ingatlanok.model.PortalScrapeResult;
import ingatlanok.model.PortalScrapeStatus;
import ingatlanok.model.PropertyType;
import ingatlanok.model.SearchCriteria;

class JofogasLiveHtmlParsingTest {

  private static final Path FIXTURE_PATH =
      Path.of("src/test/resources/fixtures/jofogas/jofogas-search-debrecen.html");

  @Test
  @DisplayName("Élő Jófogás snapshot parzolás: 36 egyedi találat, pontos paraméterek és __NEXT_DATA__ dúsítás")
  void testLiveSnapshotParsing() throws Exception {
    String html = Files.readString(FIXTURE_PATH, StandardCharsets.UTF_8);
    Document doc = Jsoup.parse(html, JofogasUrlBuilder.BASE_URL);

    JofogasPortalEngine engine =
        new JofogasPortalEngine(new JofogasUrlBuilder(), new ListingNormalizer());
    SearchCriteria criteria = buildDebrecenHouseCriteria();

    PortalScrapeResult result =
        engine.scrape(criteria, url -> url.contains("o=2") ? null : doc, 2);

    assertEquals(PortalScrapeStatus.SUCCESS, result.getStatus());
    assertEquals(36, result.getItems().size(), "Az 1. oldali Jófogás snapshot 36 egyedi hirdetést tartalmaz");

    assertTrue(
        result.getItems().stream().allMatch(item -> "Debrecen".equals(item.getCity())),
        "A városmező minden tételnél Debrecen maradjon");
    assertFalse(
        result.getItems().stream()
            .anyMatch(item -> item.getDetailsInfo() != null
                && Double.valueOf(1350.0).equals(item.getDetailsInfo().getAreaSquareMeters())),
        "A címben szereplő 1350 nm-es telekméret nem írhatja felül az épület alapterületét");

    Map<String, IngatlanItem> byId = result.getItems().stream()
        .collect(Collectors.toMap(IngatlanItem::getId, Function.identity()));

    IngatlanItem ad146862190 = byId.get("146862190");
    assertNotNull(ad146862190);
    assertEquals(91_900_000L, ad146862190.getPriceInfo().getAmountHuf());
    assertEquals(90.0, ad146862190.getDetailsInfo().getAreaSquareMeters());
    assertEquals(5.0, ad146862190.getDetailsInfo().getRoomCount());
    assertEquals("Debrecen", ad146862190.getCity());
    assertEquals("Debrecen, Alsó-Józsa", ad146862190.getLocation());

    IngatlanItem ad153288767 = byId.get("153288767");
    assertNotNull(ad153288767);
    assertEquals(109_999_000L, ad153288767.getPriceInfo().getAmountHuf());
    assertEquals(120.0, ad153288767.getDetailsInfo().getAreaSquareMeters());
    assertEquals("Debrecen, Júliakert", ad153288767.getLocation());

    IngatlanItem ad157303459 = byId.get("157303459");
    assertNotNull(ad157303459);
    assertEquals(115_000_000L, ad157303459.getPriceInfo().getAmountHuf());
    assertEquals(150.0, ad157303459.getDetailsInfo().getAreaSquareMeters());
    assertEquals(5.0, ad157303459.getDetailsInfo().getRoomCount());
    assertEquals("Debrecen, Lenc-telep", ad157303459.getLocation());
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
