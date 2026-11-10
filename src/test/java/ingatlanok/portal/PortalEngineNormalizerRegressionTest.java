package ingatlanok.portal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ingatlanok.core.PropertyFilterEngine;
import ingatlanok.core.normalization.ListingNormalizer;
import ingatlanok.model.IngatlanItem;
import ingatlanok.model.PortalScrapeResult;
import ingatlanok.model.PropertyType;
import ingatlanok.model.SearchCriteria;
import ingatlanok.model.raw.ExtractionMethod;
import ingatlanok.model.raw.RawExtractionResult;
import ingatlanok.model.raw.RawPropertyListing;
import ingatlanok.portal.dh.DunaHousePortalEngine;
import ingatlanok.portal.dh.DunaHouseUrlBuilder;
import ingatlanok.portal.ingatlanbazar.IngatlanBazarPortalEngine;
import ingatlanok.portal.ingatlanbazar.IngatlanBazarQueryBuilder;
import ingatlanok.portal.ingatlancom.IngatlanComPortalEngine;
import ingatlanok.portal.ingatlancom.IngatlanComUrlBuilder;
import ingatlanok.portal.jofogas.JofogasPortalEngine;
import ingatlanok.portal.jofogas.JofogasUrlBuilder;
import ingatlanok.portal.koltozzbe.KoltozzbePortalEngine;
import ingatlanok.portal.koltozzbe.KoltozzbeUrlBuilder;
import ingatlanok.portal.oc.OtthonCentrumPortalEngine;
import ingatlanok.portal.oc.OtthonCentrumUrlBuilder;

class PortalEngineNormalizerRegressionTest {

  private static final PortalDescriptor DESCRIPTOR = new PortalDescriptor(
      "dh",
      "Duna House",
      "#E2001A",
      "https://dh.hu",
      true,
      List.of("haz", "lakas"),
      true
  );

  @Test
  @DisplayName("Regressziós hiba: AdapterPortalEngine default konstruktora nem veszítheti el a normalizálót és az árat")
  void testAdapterPortalEngineDefaultConstructorNormalizesPriceAndArea() {
    RawPropertyListing raw = RawPropertyListing.builder()
        .portalId("dh")
        .nativeId("DH-999")
        .url("https://dh.hu/ingatlan/DH-999")
        .title("Eladó családi ház Debrecen")
        .locationText("Debrecen")
        .priceHuf(85_000_000L)
        .areaSquareMeters(120.0)
        .roomCount(4.0)
        .method(ExtractionMethod.HTML)
        .build();

    ExtractionStrategy strategy = new ExtractionStrategy() {
      @Override
      public ExtractionMethod method() {
        return ExtractionMethod.HTML;
      }

      @Override
      public RawExtractionResult extract(SearchCriteria criteria) {
        return RawExtractionResult.success(List.of(raw), method());
      }
    };

    PortalAdapter adapter = new PortalAdapter() {
      @Override
      public PortalDescriptor getDescriptor() {
        return DESCRIPTOR;
      }

      @Override
      public PortalUrlBuilder getUrlBuilder() {
        return null;
      }

      @Override
      public List<ExtractionStrategy> getStrategies() {
        return List.of(strategy);
      }
    };

    AdapterPortalEngine engine = new AdapterPortalEngine(adapter);

    SearchCriteria criteria = new SearchCriteria();
    criteria.setCity("Debrecen");
    criteria.setMinPriceMillion(70);
    criteria.setMaxPriceMillion(120);
    criteria.setMinArea(90);
    criteria.setMaxArea(200);
    criteria.setPropertyType(PropertyType.HAZ);

    PortalScrapeResult result = engine.scrape(criteria);

    assertNotNull(result, "Scrape eredmény nem lehet null");
    assertEquals(1, result.getItems().size(), "1 hirdetésnek kell lennie");

    IngatlanItem item = result.getItems().get(0);
    assertNotNull(item.getPriceInfo(), "PriceInfo nem lehet null normalizálás után!");
    assertEquals(85.0, item.getPriceInfo().getAmountMillion(), 0.01, "Árnak 85 M Ft-nak kell lennie");
    assertNotNull(item.getDetailsInfo(), "DetailsInfo nem lehet null!");
    assertEquals(120.0, item.getDetailsInfo().getAreaSquareMeters(), 0.01, "Területnek 120 nm-nek kell lennie");

    PropertyFilterEngine filterEngine = new PropertyFilterEngine();
    PropertyFilterEngine.FilterResult filterResult = filterEngine.filter(result.getItems(), criteria, "Debrecen");
    assertEquals(1, filterResult.matchingItems().size(), "A hirdetésnek át kell mennie a szűrőn, nem eshet ki!");
  }

  @Test
  @DisplayName("Minden portál motor rendelkezik működőképes normalizálóval a default konstruktorában is")
  void testAllPortalEnginesHaveWorkingNormalizer() {
    DunaHousePortalEngine dh = new DunaHousePortalEngine();
    OtthonCentrumPortalEngine oc = new OtthonCentrumPortalEngine();
    JofogasPortalEngine jofogas = new JofogasPortalEngine();
    KoltozzbePortalEngine koltozzbe = new KoltozzbePortalEngine();
    IngatlanBazarPortalEngine bazar = new IngatlanBazarPortalEngine();
    IngatlanComPortalEngine icom = new IngatlanComPortalEngine();

    assertNotNull(dh.getNormalizer(), "DH normalizer nem lehet null");
    assertNotNull(oc.getNormalizer(), "OC normalizer nem lehet null");
    assertNotNull(jofogas.getNormalizer(), "Jófogás normalizer nem lehet null");
    assertNotNull(koltozzbe.getNormalizer(), "Költözzbe normalizer nem lehet null");
    assertNotNull(bazar.getNormalizer(), "Bazár normalizer nem lehet null");
    assertNotNull(icom.getNormalizer(), "Ingatlan.com normalizer nem lehet null");

    RawPropertyListing sampleRaw = RawPropertyListing.builder()
        .portalId("dh")
        .nativeId("TEST-1")
        .url("https://dh.hu/1")
        .title("Szép ház")
        .locationText("Debrecen")
        .priceHuf(90_000_000L)
        .areaSquareMeters(100.0)
        .build();

    IngatlanItem normalized = dh.getNormalizer().normalize(
        sampleRaw, dh.getDescriptor(), new SearchCriteria(), "2026-11-08T22:00:00");
    assertNotNull(normalized.getPriceInfo(), "Ár normalizálása sikeres kell legyen");
    assertEquals(90.0, normalized.getPriceInfo().getAmountMillion(), 0.01);
  }

  @Test
  @DisplayName("Minden portál motor támogatja az explicit constructor injection-t ListingNormalizer-rel")
  void testAllPortalEnginesSupportConstructorInjection() {
    ListingNormalizer normalizer = new ListingNormalizer();
    DunaHousePortalEngine dh = new DunaHousePortalEngine(new DunaHouseUrlBuilder(), normalizer);
    OtthonCentrumPortalEngine oc = new OtthonCentrumPortalEngine(new OtthonCentrumUrlBuilder(), normalizer);
    JofogasPortalEngine jofogas = new JofogasPortalEngine(new JofogasUrlBuilder(), normalizer);
    KoltozzbePortalEngine koltozzbe = new KoltozzbePortalEngine(
        new KoltozzbeUrlBuilder(), 2, normalizer);
    IngatlanBazarPortalEngine bazar = new IngatlanBazarPortalEngine(new IngatlanBazarQueryBuilder(), normalizer);
    IngatlanComPortalEngine icom = new IngatlanComPortalEngine(new IngatlanComUrlBuilder(), normalizer);

    assertEquals(normalizer, dh.getNormalizer());
    assertEquals(normalizer, oc.getNormalizer());
    assertEquals(normalizer, jofogas.getNormalizer());
    assertEquals(normalizer, koltozzbe.getNormalizer());
    assertEquals(normalizer, bazar.getNormalizer());
    assertEquals(normalizer, icom.getNormalizer());
  }
}
