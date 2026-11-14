package ingatlanok.portal.koltozzbe;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ingatlanok.core.DeduplicationEngine;
import ingatlanok.core.dedup.LocationConflictDetector;
import ingatlanok.core.dedup.LocationConflictDetector.ExtractedLocation;
import ingatlanok.core.normalization.ListingNormalizer;
import ingatlanok.model.IngatlanItem;
import ingatlanok.model.PortalScrapeResult;
import ingatlanok.model.PortalScrapeStatus;
import ingatlanok.model.PropertyType;
import ingatlanok.model.SearchCriteria;
import ingatlanok.model.raw.RawPropertyListing;
import ingatlanok.portal.PortalDescriptor;

class KoltozzbeLiveHtmlParsingTest {

  private static final Path FIXTURE_PATH =
      Path.of("src/test/resources/fixtures/koltozzbe/koltozzbe-search-debrecen.html");

  @Test
  @DisplayName("Költözzbe önálló HTML kártya parzolás: .item-type ár, .listing-size alapterület és félszoba")
  void testStandaloneHtmlParsing() throws Exception {
    Document doc = loadDocument();
    KoltozzbeRawParser rawParser = new KoltozzbeRawParser();
    SearchCriteria criteria = buildDebrecenHouseCriteria();
    ListingNormalizer normalizer = new ListingNormalizer();
    PortalDescriptor descriptor = new KoltozzbePortalAdapter().getDescriptor();

    List<RawPropertyListing> raws = rawParser.parseHtmlListings(doc);
    List<IngatlanItem> items = raws.stream()
        .map(raw -> normalizer.normalize(raw, descriptor, criteria, "2026-11-14T12:00:00"))
        .toList();

    assertEquals(20, items.size(), "A Költözzbe 1. oldali HTML snapshot 20 hirdetési kártyát tartalmaz");
    assertTrue(items.stream().allMatch(item -> "Debrecen".equals(item.getCity())),
        "A városmező minden tételnél kanonikus 'Debrecen' maradjon");

    Map<String, IngatlanItem> byId = items.stream()
        .collect(Collectors.toMap(IngatlanItem::getId, Function.identity()));

    IngatlanItem h10911678 = byId.get("10911678");
    assertNotNull(h10911678);
    assertEquals(106_000_000L, h10911678.getPriceInfo().getAmountHuf());
    assertEquals(132.0, h10911678.getDetailsInfo().getAreaSquareMeters());
    assertEquals(4.5, h10911678.getDetailsInfo().getRoomCount());
    assertEquals("Debrecen", h10911678.getCity());
    assertEquals("Debrecen, Buzogány utca", h10911678.getLocation());

    IngatlanItem h10841919 = byId.get("10841919");
    assertNotNull(h10841919);
    assertEquals(109_000_000L, h10841919.getPriceInfo().getAmountHuf());
    assertEquals(157.0, h10841919.getDetailsInfo().getAreaSquareMeters());
    assertEquals(3.5, h10841919.getDetailsInfo().getRoomCount());
  }

  @Test
  @DisplayName("Költözzbe teljes scrape + intraportál és utca vs városrész deduplikációs védelem")
  void testFullScrapeNoFalseMergesAndStreetQuarterCompatibility() throws Exception {
    Document doc = loadDocument();
    KoltozzbePortalEngine engine =
        new KoltozzbePortalEngine(new KoltozzbeUrlBuilder(), 1, new ListingNormalizer());
    SearchCriteria criteria = buildDebrecenHouseCriteria();

    PortalScrapeResult result =
        engine.scrape(criteria, url -> url.contains("page=") ? null : doc, 1);

    assertEquals(PortalScrapeStatus.SUCCESS, result.getStatus());
    assertEquals(20, result.getItems().size());

    DeduplicationEngine dedup = new DeduplicationEngine(null);
    List<IngatlanItem> deduplicated = dedup.deduplicateMultiSignal(result.getItems());
    assertEquals(20, deduplicated.size(),
        "A Költözzbe 1. oldalán szereplő 20 eltérő hirdetés nem vonható össze tévesen");

    IngatlanItem h10911678 = result.getItems().stream()
        .filter(item -> "10911678".equals(item.getId()))
        .findFirst()
        .orElseThrow();
    ExtractedLocation kbLoc = LocationConflictDetector.extractLocation(h10911678, "debrecen");
    ExtractedLocation juliaLoc = LocationConflictDetector.extractLocation(
        new IngatlanItem("oc-1", "Otthon Centrum", "https://www.oc.hu/ingatlanok/X1",
            "Eladó családi ház", "99.9 M Ft", "Debrecen, Júlia-telep", "Debrecen",
            "132 m²", "2026-11-14T12:00:00", false),
        "debrecen");

    assertFalse(LocationConflictDetector.hasConflict(kbLoc, juliaLoc, "debrecen"),
        "Utcanév a location mezőben ('Debrecen, Buzogány utca') nem generálhat ál-városrész konfliktust");
  }

  private Document loadDocument() throws Exception {
    String html = Files.readString(FIXTURE_PATH, StandardCharsets.UTF_8);
    return Jsoup.parse(html, KoltozzbeUrlBuilder.BASE_URL);
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
