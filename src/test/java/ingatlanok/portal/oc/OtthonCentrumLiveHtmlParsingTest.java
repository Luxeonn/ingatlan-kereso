package ingatlanok.portal.oc;

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

import ingatlanok.core.normalization.ListingNormalizer;
import ingatlanok.model.IngatlanItem;
import ingatlanok.model.PortalScrapeResult;
import ingatlanok.model.PortalScrapeStatus;
import ingatlanok.model.PropertyType;
import ingatlanok.model.SearchCriteria;
import ingatlanok.model.raw.RawPropertyListing;
import ingatlanok.portal.PortalDescriptor;

class OtthonCentrumLiveHtmlParsingTest {

  private static final Path PAGE1_PATH =
      Path.of("src/test/resources/fixtures/oc/oc-search-debrecen-page1.html");
  private static final Path CHUNK2_PATH =
      Path.of("src/test/resources/fixtures/oc/oc-search-debrecen-chunk2.html");

  @Test
  @DisplayName("Élő OC 1. oldal és chunk 2 HTML snapshot parzolás: 12+12 tétel, projekt /uj-lakas és városrész")
  void testLiveSnapshotPage1AndChunk2Parsing() throws Exception {
    Document doc1 = loadDocument(PAGE1_PATH);
    Document doc2 = loadDocument(CHUNK2_PATH);

    OtthonCentrumRawParser rawParser = new OtthonCentrumRawParser();
    SearchCriteria criteria = buildDebrecenHouseCriteria();
    ListingNormalizer normalizer = new ListingNormalizer();
    PortalDescriptor descriptor = new OtthonCentrumPortalAdapter().getDescriptor();

    List<IngatlanItem> page1Items = normalizeAll(
        rawParser.parseRawListings(doc1), normalizer, descriptor, criteria);
    List<IngatlanItem> chunk2Items = normalizeAll(
        rawParser.parseRawListings(doc2), normalizer, descriptor, criteria);

    assertEquals(12, page1Items.size(), "Az 1. oldali OC snapshot pontosan 12 kártyát tartalmaz");
    assertEquals(12, chunk2Items.size(), "A 2. chunk OC snapshot pontosan 12 kártyát tartalmaz");

    Map<String, IngatlanItem> byId = page1Items.stream()
        .collect(Collectors.toMap(IngatlanItem::getId, Function.identity()));
    chunk2Items.forEach(item -> byId.put(item.getId(), item));

    assertTrue(page1Items.stream().allMatch(item -> "Debrecen".equals(item.getCity())),
        "A városmező minden tételnél kanonikus 'Debrecen' maradjon");

    IngatlanItem h526137 = byId.get("H526137");
    assertNotNull(h526137, "H526137 szerepel az 1. oldalon");
    assertEquals("https://www.oc.hu/ingatlanok/H526137", h526137.getUrl());
    assertEquals(84_990_000L, h526137.getPriceInfo().amountHuf());
    assertEquals(110.0, h526137.getDetailsInfo().areaSquareMeters());
    assertEquals(4.0, h526137.getDetailsInfo().roomCount());
    assertEquals("Debrecen", h526137.getCity());
    assertEquals("Debrecen, Belváros", h526137.getLocation());
    assertNotNull(h526137.getImageUrl());
    assertTrue(h526137.getImageUrl().startsWith("https://"));
    assertFalse(h526137.getImageUrl().endsWith("_53x39_crop.jpg"));

    IngatlanItem dhz = byId.get("DHZ090164");
    assertNotNull(dhz, "DHZ090164 szerepel a 2. chunkban");
    assertEquals("https://www.oc.hu/ingatlanok/DHZ090164", dhz.getUrl());
    assertEquals(99_900_000L, dhz.getPriceInfo().amountHuf());
    assertEquals(142.0, dhz.getDetailsInfo().areaSquareMeters());
    assertEquals(5.0, dhz.getDetailsInfo().roomCount());
    assertEquals("Debrecen", dhz.getCity());
    assertEquals("Debrecen, Júlia-telep", dhz.getLocation());

    IngatlanItem projectP4922 = byId.get("P4922");
    assertNotNull(projectP4922, "Az /uj-lakas/P4922 projektkártya nem veszhet el");
    assertEquals("https://www.oc.hu/uj-lakas/P4922", projectP4922.getUrl());
    assertEquals(105_000_000L, projectP4922.getPriceInfo().amountHuf());
    assertEquals(95.0, projectP4922.getDetailsInfo().areaSquareMeters());
    assertEquals(3.0, projectP4922.getDetailsInfo().roomCount());
    assertEquals("Debrecen", projectP4922.getCity());
    assertEquals("Debrecen, Felső-Józsa", projectP4922.getLocation());
  }

  @Test
  @DisplayName("OC chunk lapozás: POST /ingatlanok/kereses/chunk?page=2 visszaadja mind a 24 egyedi találatot")
  void testChunkPaginationEndToEndReturns24Items() throws Exception {
    Document doc1 = loadDocument(PAGE1_PATH);
    Document doc2 = loadDocument(CHUNK2_PATH);
    SearchCriteria criteria = buildDebrecenHouseCriteria();
    OtthonCentrumPortalEngine engine =
        new OtthonCentrumPortalEngine(new OtthonCentrumUrlBuilder(), new ListingNormalizer());

    PortalScrapeResult result = engine.scrape(
        criteria,
        url -> doc1,
        (chunkUrl, body) -> chunkUrl.contains("page=2")
            && body != null
            && body.contains("\"debrecen\"") ? doc2 : null,
        3);

    assertEquals(PortalScrapeStatus.SUCCESS, result.getStatus());
    assertEquals(24, result.getItems().size(),
        "Az 1. oldal (12) + 2. chunk POST (12) összesen 24 egyedi tételt ad vissza");
  }

  private Document loadDocument(Path path) throws Exception {
    String html = Files.readString(path, StandardCharsets.UTF_8);
    return Jsoup.parse(html, OtthonCentrumUrlBuilder.BASE_URL);
  }

  private List<IngatlanItem> normalizeAll(
      List<RawPropertyListing> rawListings,
      ListingNormalizer normalizer,
      PortalDescriptor descriptor,
      SearchCriteria criteria) {
    return rawListings.stream()
        .map(raw -> normalizer.normalize(raw, descriptor, criteria, "2026-11-14T12:00:00"))
        .toList();
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
