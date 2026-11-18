package ingatlanok.testsupport;

import java.util.List;

import org.mapstruct.factory.Mappers;

import ingatlanok.core.DataQualityEngine;
import ingatlanok.core.DeduplicationEngine;
import ingatlanok.core.MarketAnalyticsEngine;
import ingatlanok.core.MarketStatisticsCalculator;
import ingatlanok.core.ParallelPortalScrapeExecutor;
import ingatlanok.core.PropertyFilterEngine;
import ingatlanok.core.SearchExecutionEngine;
import ingatlanok.core.SearchPersistenceSynchronizer;
import ingatlanok.core.SearchResultAggregator;
import ingatlanok.core.normalization.ListingNormalizer;
import ingatlanok.core.normalization.ListingValidator;
import ingatlanok.model.IngatlanItem;
import ingatlanok.model.PortalSource;
import ingatlanok.model.PriceInfo;
import ingatlanok.model.PropertyDetails;
import ingatlanok.model.PropertyType;
import ingatlanok.model.SearchCriteria;
import ingatlanok.persistence.ConfigStorage;
import ingatlanok.persistence.ListingStorage;
import ingatlanok.portal.PortalEngineRegistry;
import ingatlanok.portal.RawListingNormalizer;
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
import ingatlanok.presentation.DashboardPresentationService;
import ingatlanok.presentation.PresentationMapper;

/**
 * Shared Object Mother / Test Data Builder factory for backend tests.
 * Produces deterministic, {@link ingatlanok.core.DataQualityEngine}-compliant domain instances in a
 * single line to minimize test boilerplate and AI token consumption.
 */
public final class TestFixtures {

  private TestFixtures() {}

  public static PortalSource validPortalSource(String portalId, String portalName) {
    return PortalSource.builder()
        .portalId(portalId)
        .portalName(portalName)
        .themeColor("#008638")
        .directListing(true)
        .build();
  }

  public static PriceInfo validPriceInfo(long amountHuf) {
    double millions = Math.round((amountHuf / 1_000_000.0) * 10.0) / 10.0;
    return PriceInfo.builder()
        .rawText(millions + " M Ft")
        .amountHuf(amountHuf)
        .amountMillion(millions)
        .build();
  }

  public static PropertyDetails validPropertyDetails(double areaM2, double rooms, long amountHuf) {
    long sqmPrice = areaM2 > 0 ? Math.round(amountHuf / areaM2) : 0L;
    return PropertyDetails.builder()
        .rawText((int) areaM2 + " m², " + (int) rooms + " szoba")
        .areaSquareMeters(areaM2)
        .roomCount(rooms)
        .pricePerSquareMeter(sqmPrice)
        .build();
  }

  public static IngatlanItem validResidentialItem(String id) {
    return validResidentialItem(id, "Debrecen", 45_000_000L, 60.0, 2.0);
  }

  public static IngatlanItem validResidentialItem(
      String id, String city, long amountHuf, double areaM2, double rooms) {
    String url = "https://dh.hu/ingatlan/" + id;
    PriceInfo priceInfo = validPriceInfo(amountHuf);
    PropertyDetails detailsInfo = validPropertyDetails(areaM2, rooms, amountHuf);
    IngatlanItem item =
        IngatlanItem.builder()
            .id(id)
            .source(validPortalSource("dh", "Duna House"))
            .url(url)
            .title("Eladó tégla lakás " + city + " (" + id + ")")
            .priceInfo(priceInfo)
            .city(city)
            .location(city + ", Belváros")
            .detailsInfo(detailsInfo)
            .discoveredAt("2026-03-15 10:00:00")
            .isNew(true)
            .propertyType(PropertyType.LAKAS)
            .build();
    item.setQualityScore(100);
    return item;
  }

  public static IngatlanItem validPlotItem(String id, String city, long amountHuf, double areaM2) {
    String url = "https://oc.hu/ingatlan/" + id;
    PriceInfo priceInfo = validPriceInfo(amountHuf);
    long sqmPrice = areaM2 > 0 ? Math.round(amountHuf / areaM2) : 0L;
    PropertyDetails detailsInfo =
        PropertyDetails.builder()
            .rawText((int) areaM2 + " m²")
            .areaSquareMeters(areaM2)
            .pricePerSquareMeter(sqmPrice)
            .build();
    IngatlanItem item =
        IngatlanItem.builder()
            .id(id)
            .source(validPortalSource("oc", "Otthon Centrum"))
            .url(url)
            .title("Eladó építési telek " + city + " (" + id + ")")
            .priceInfo(priceInfo)
            .city(city)
            .location(city)
            .detailsInfo(detailsInfo)
            .discoveredAt("2026-03-15 10:00:00")
            .isNew(true)
            .propertyType(PropertyType.TELEK)
            .build();
    item.setQualityScore(100);
    return item;
  }

  public static IngatlanItem validHouseItem(
      String id, String city, long amountHuf, double areaM2, double rooms) {
    String url = "https://oc.hu/ingatlan/" + id;
    PriceInfo priceInfo = validPriceInfo(amountHuf);
    PropertyDetails detailsInfo = validPropertyDetails(areaM2, rooms, amountHuf);
    IngatlanItem item =
        IngatlanItem.builder()
            .id(id)
            .source(validPortalSource("oc", "Otthon Centrum"))
            .url(url)
            .title("Eladó családi ház " + city + " (" + id + ")")
            .priceInfo(priceInfo)
            .city(city)
            .location(city + ", Kertváros")
            .detailsInfo(detailsInfo)
            .discoveredAt("2026-03-15 10:00:00")
            .isNew(true)
            .propertyType(PropertyType.HAZ)
            .build();
    item.setQualityScore(100);
    return item;
  }

  public static IngatlanItem validItemWithQuality(
      String id, String city, PropertyType type, long amountHuf, double areaM2, int qualityScore) {
    String url = "https://dh.hu/ingatlan/" + id;
    PriceInfo priceInfo = validPriceInfo(amountHuf);
    PropertyDetails detailsInfo = validPropertyDetails(areaM2, 2.0, amountHuf);
    IngatlanItem item =
        IngatlanItem.builder()
            .id(id)
            .source(validPortalSource("dh", "Duna House"))
            .url(url)
            .title("Ingatlan " + city + " (" + id + ")")
            .priceInfo(priceInfo)
            .city(city)
            .location(city)
            .detailsInfo(detailsInfo)
            .discoveredAt("2026-03-15 10:00:00")
            .isNew(true)
            .propertyType(type)
            .build();
    item.setQualityScore(qualityScore);
    return item;
  }

  public static SearchCriteria validSearchCriteria(String city) {
    SearchCriteria criteria = new SearchCriteria();
    criteria.setCity(city);
    criteria.setCities(List.of(city));
    criteria.setPropertyType("lakas");
    criteria.setMinPriceMillion(20);
    criteria.setMaxPriceMillion(80);
    criteria.setMinArea(40);
    criteria.setMaxArea(120);
    criteria.setPortals(List.of("dh", "oc", "jofogas", "koltozzbe", "ingatlanbazar"));
    return criteria;
  }

  public static ListingNormalizer listingNormalizer() {
    return new ListingNormalizer(new ListingValidator());
  }

  public static MarketAnalyticsEngine marketAnalyticsEngine() {
    return new MarketAnalyticsEngine(new DataQualityEngine(), new MarketStatisticsCalculator());
  }

  public static MarketAnalyticsEngine marketAnalyticsEngine(DataQualityEngine qualityEngine) {
    return new MarketAnalyticsEngine(qualityEngine, new MarketStatisticsCalculator());
  }

  public static DeduplicationEngine deduplicationEngine() {
    return new DeduplicationEngine(null, null);
  }

  public static DeduplicationEngine deduplicationEngine(ListingStorage storage) {
    return new DeduplicationEngine(storage, null);
  }

  public static DunaHousePortalEngine dhEngine() {
    return new DunaHousePortalEngine(new DunaHouseUrlBuilder(), listingNormalizer());
  }

  public static OtthonCentrumPortalEngine ocEngine() {
    return new OtthonCentrumPortalEngine(new OtthonCentrumUrlBuilder(), listingNormalizer());
  }

  public static JofogasPortalEngine jofogasEngine() {
    return new JofogasPortalEngine(new JofogasUrlBuilder(), listingNormalizer());
  }

  public static IngatlanBazarPortalEngine ingatlanBazarEngine() {
    return new IngatlanBazarPortalEngine(new IngatlanBazarQueryBuilder(), listingNormalizer());
  }

  public static KoltozzbePortalEngine koltozzbeEngine() {
    return new KoltozzbePortalEngine(new KoltozzbeUrlBuilder(), listingNormalizer());
  }

  public static IngatlanComPortalEngine ingatlanComEngine() {
    return ingatlanComEngine(listingNormalizer());
  }

  public static IngatlanComPortalEngine ingatlanComEngine(RawListingNormalizer normalizer) {
    return new IngatlanComPortalEngine(new IngatlanComUrlBuilder(), normalizer);
  }

  public static PresentationMapper presentationMapper() {
    return Mappers.getMapper(PresentationMapper.class);
  }

  public static SearchExecutionEngine searchExecutionEngine(
      PortalEngineRegistry registry, ListingStorage listingStorage, ConfigStorage configStorage) {
    DataQualityEngine qualityEngine = new DataQualityEngine();
    return searchExecutionEngine(
        registry, listingStorage, configStorage, qualityEngine, marketAnalyticsEngine(qualityEngine));
  }

  public static SearchExecutionEngine searchExecutionEngine(
      PortalEngineRegistry registry,
      ListingStorage listingStorage,
      ConfigStorage configStorage,
      DataQualityEngine qualityEngine,
      MarketAnalyticsEngine analyticsEngine) {
    return SearchExecutionEngine.builder()
        .portalRegistry(registry)
        .listingStorage(listingStorage)
        .configStorage(configStorage)
        .filterEngine(new PropertyFilterEngine())
        .dedupEngine(deduplicationEngine(listingStorage))
        .analyticsEngine(analyticsEngine)
        .qualityEngine(qualityEngine)
        .scrapeExecutor(new ParallelPortalScrapeExecutor())
        .resultAggregator(new SearchResultAggregator())
        .persistenceSynchronizer(new SearchPersistenceSynchronizer(registry, listingStorage))
        .build();
  }

  public static DashboardPresentationService dashboardPresentationService(
      SearchExecutionEngine searchEngine, MarketAnalyticsEngine analyticsEngine) {
    return new DashboardPresentationService(
        searchEngine, analyticsEngine, null, presentationMapper());
  }
}
