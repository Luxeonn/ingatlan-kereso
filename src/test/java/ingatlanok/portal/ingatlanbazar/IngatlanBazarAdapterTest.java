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
  }
}
