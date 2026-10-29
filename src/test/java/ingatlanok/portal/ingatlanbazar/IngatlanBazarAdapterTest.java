package ingatlanok.portal.ingatlanbazar;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ingatlanok.model.PortalScrapeResult;
import ingatlanok.model.PortalScrapeStatus;
import ingatlanok.model.SearchCriteria;
import ingatlanok.model.raw.ExtractionMethod;
import ingatlanok.model.raw.RawPropertyListing;
import ingatlanok.portal.AdapterPortalEngine;

class IngatlanBazarAdapterTest {

  @Test
  @DisplayName("IngatlanBazarRawParser feldolgozza a JSON válasz mezőit nyers modellbe")
  void testParseRawListings() throws Exception {
    String json = new String(
        getClass().getResourceAsStream("/fixtures/ingatlanbazar/sample-ingatlanbazar.json").readAllBytes());

    IngatlanBazarRawParser parser = new IngatlanBazarRawParser();
    List<RawPropertyListing> rawListings = parser.parseRawListings(json);

    assertEquals(2, rawListings.size());
    RawPropertyListing first = rawListings.get(0);
    assertEquals("ingatlanbazar", first.portalId());
    assertEquals("IB999", first.nativeId());
    assertEquals("https://www.ingatlanbazar.hu/ingatlan/hajduszoboszlo/IB999", first.url());
    assertEquals(92_000_000L, first.priceHuf());
    assertEquals(115.0, first.areaSquareMeters());
    assertEquals(4.0, first.roomCount());
    assertEquals(ExtractionMethod.API, first.method());
    assertEquals(
        "https://images2.ingatlanbazar.hu/static/property/images/b3/4c/44/2d/b34c442d-c20e-11f1-b393-bc2411815210/4455ddbb-8d38-453a-a0c7-063fd4872b56_485x354.jpg",
        first.imageUrl());
  }

  @Test
  @DisplayName("IngatlanBazarPortalAdapter + AdapterPortalEngine sikeres kinyerés és normalizálás")
  void testIngatlanBazarAdapterWithEngine() throws Exception {
    String json = new String(
        getClass().getResourceAsStream("/fixtures/ingatlanbazar/sample-ingatlanbazar.json").readAllBytes());

    IngatlanBazarApiStrategy strategy = new IngatlanBazarApiStrategy(
        new IngatlanBazarQueryBuilder(),
        new IngatlanBazarRawParser(),
        url -> json
    );

    IngatlanBazarPortalAdapter adapter = new IngatlanBazarPortalAdapter(
        new IngatlanBazarQueryBuilder(),
        List.of(strategy)
    );

    AdapterPortalEngine engine = new AdapterPortalEngine(adapter);
    SearchCriteria criteria = new SearchCriteria();
    criteria.setCity("Hajdúszoboszló");
    criteria.setPropertyType("haz");

    PortalScrapeResult result = engine.scrape(criteria);

    assertEquals(PortalScrapeStatus.SUCCESS, result.getStatus());
    assertEquals(2, result.getItems().size());
    assertEquals("IB999", result.getItems().get(0).getId());
    assertEquals(92_000_000L, result.getItems().get(0).getPriceInfo().getAmountHuf());
    assertEquals(
        "https://images2.ingatlanbazar.hu/static/property/images/b3/4c/44/2d/b34c442d-c20e-11f1-b393-bc2411815210/4455ddbb-8d38-453a-a0c7-063fd4872b56_485x354.jpg",
        result.getItems().get(0).getImageUrl());
  }

  @Test
  @DisplayName("extractImageUrl helyesen kezeli a különböző képmezőket és méret utótagokat")
  void testExtractImageUrlVariants() throws Exception {
    tools.jackson.databind.ObjectMapper mapper = new tools.jackson.databind.ObjectMapper();

    // 1. renderPictures tömbből nyers bázis URL -> hozzáadja a _485x354.jpg-t
    String jsonRenderPics = "{\"renderPictures\": [\"https://images2.ingatlanbazar.hu/static/property/images/1/2\"]}";
    assertEquals(
        "https://images2.ingatlanbazar.hu/static/property/images/1/2_485x354.jpg",
        IngatlanBazarRawParser.extractImageUrl(mapper.readTree(jsonRenderPics)));

    // 2. renderPictures már kiterjesztéssel rendelkező URL -> nem duplázza az utótagot
    String jsonRenderPicsWithExt = "{\"renderPictures\": [\"https://images2.ingatlanbazar.hu/static/property/images/1/2_485x354.jpg\"]}";
    assertEquals(
        "https://images2.ingatlanbazar.hu/static/property/images/1/2_485x354.jpg",
        IngatlanBazarRawParser.extractImageUrl(mapper.readTree(jsonRenderPicsWithExt)));

    // 3. photo objektum url mezővel
    String jsonPhotoObj = "{\"photo\": {\"url\": \"https://example.com/test.jpg\"}}";
    assertEquals(
        "https://example.com/test.jpg",
        IngatlanBazarRawParser.extractImageUrl(mapper.readTree(jsonPhotoObj)));

    // 4. picture string
    String jsonPicture = "{\"picture\": \"//images.ingatlanbazar.hu/pic.jpg\"}";
    assertEquals(
        "https://images.ingatlanbazar.hu/pic.jpg",
        IngatlanBazarRawParser.extractImageUrl(mapper.readTree(jsonPicture)));

    // 5. images tömb relatív URL-lel
    String jsonImages = "{\"images\": [\"/img/prop1.jpg\"]}";
    assertEquals(
        "https://www.ingatlanbazar.hu/img/prop1.jpg",
        IngatlanBazarRawParser.extractImageUrl(mapper.readTree(jsonImages)));

    // 6. null vagy hiányzó képmezők
    String jsonNoImages = "{\"title\": \"Nincs kép\"}";
    org.junit.jupiter.api.Assertions.assertNull(
        IngatlanBazarRawParser.extractImageUrl(mapper.readTree(jsonNoImages)));
    org.junit.jupiter.api.Assertions.assertNull(
        IngatlanBazarRawParser.extractImageUrl(null));
  }
}
