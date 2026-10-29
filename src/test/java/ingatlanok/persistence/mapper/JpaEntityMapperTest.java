package ingatlanok.persistence.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import ingatlanok.model.IngatlanItem;
import ingatlanok.model.ListingObservation;
import ingatlanok.model.PriceHistoryEntry;
import ingatlanok.model.PriceInfo;
import ingatlanok.model.PropertyDetails;
import ingatlanok.model.PropertyType;
import ingatlanok.model.SourceListing;
import ingatlanok.model.StatusLogEntry;
import ingatlanok.persistence.entity.ListingObservationEntity;
import ingatlanok.persistence.entity.PriceHistoryEntity;
import ingatlanok.persistence.entity.PropertyEntity;
import ingatlanok.persistence.entity.StatusLogEntity;

@DisplayName("JpaEntityMapper Unit Tests")
class JpaEntityMapperTest {

  private JpaEntityMapper mapper;

  @BeforeEach
  void setUp() {
    mapper = Mappers.getMapper(JpaEntityMapper.class);
  }

  @Test
  @DisplayName("toItem maps all PropertyEntity fields to IngatlanItem correctly")
  void testToItem() {
    assertNull(mapper.toItem(null));

    PropertyEntity entity = new PropertyEntity();
    entity.setPropertyId("dh_123");
    entity.setUrl("https://dh.hu/123");
    entity.setPortal("Duna House");
    entity.setTitle("Kiadó lakás");
    entity.setPriceRaw("55 M Ft");
    entity.setPriceAmount(55_000_000L);
    entity.setPriceMillions(55.0);
    entity.setPricePerSqm(1_000_000L);
    entity.setCity("Budapest");
    entity.setLocation("Budapest");
    entity.setNormalizedCity("budapest");
    entity.setDetails("55 m2, 2 szoba");
    entity.setAreaSqm(55.0);
    entity.setRooms(2.0);
    entity.setDiscoveredAt("2026-03-01 10:00:00");
    entity.setNew(true);
    entity.setViewed(true);
    entity.setViewedAt("2026-03-01 11:00:00");
    entity.setImageUrl("https://dh.hu/img.jpg");
    entity.setStatus("ACTIVE");
    entity.setPropertyType(PropertyType.LAKAS);
    entity.setPriceDelta(-1_000_000L);
    entity.setPriceChangePercent(-1.79);
    entity.setLastPriceChangeAt("2026-03-01 10:30:00");

    IngatlanItem item = mapper.toItem(entity);
    assertNotNull(item);
    assertEquals("dh_123", item.getId());
    assertEquals("https://dh.hu/123", item.getUrl());
    assertEquals("Duna House", item.getPortal());
    assertEquals("Kiadó lakás", item.getTitle());
    assertEquals("55 M Ft", item.getPrice());
    assertEquals("Budapest", item.getCity());
    assertEquals(PropertyType.LAKAS, item.getPropertyType());
    assertTrue(item.isNew());
    assertTrue(item.isViewed());
    assertEquals("2026-03-01 11:00:00", item.getViewedAt());
    assertEquals("ACTIVE", item.getStatus());
    assertEquals(-1_000_000L, item.getPriceDelta());

    assertNotNull(item.getPriceInfo());
    assertEquals(55_000_000L, item.getPriceInfo().getAmountHuf());
    assertEquals(55.0, item.getPriceInfo().getAmountMillion());

    assertNotNull(item.getDetailsInfo());
    assertEquals(55.0, item.getDetailsInfo().getAreaSquareMeters());
    assertEquals(2.0, item.getDetailsInfo().getRoomCount());
    assertEquals(1_000_000L, item.getDetailsInfo().getPricePerSquareMeter());
  }

  @Test
  @DisplayName("toItems converts list of PropertyEntity")
  void testToItems() {
    assertTrue(mapper.toItems(null).isEmpty());
    assertTrue(mapper.toItems(List.of()).isEmpty());

    PropertyEntity e1 = new PropertyEntity();
    e1.setPropertyId("p1");
    List<IngatlanItem> items = mapper.toItems(List.of(e1));
    assertEquals(1, items.size());
    assertEquals("p1", items.get(0).getId());
  }

  @Test
  @DisplayName("toEntity creates new entity when existing is null")
  void testToEntityNew() {
    IngatlanItem item = new IngatlanItem();
    item.setId("456");
    item.setUrl("https://oc.hu/456");
    item.setPortal("Otthon Centrum");
    item.setCity("Szeged");
    item.setTitle("Szép ház");
    item.setPriceInfo(new PriceInfo("40 M Ft", 40_000_000L, 40.0));
    item.setDetailsInfo(new PropertyDetails("80 m2, 3 szoba", 80.0, 3.0, 500_000L));
    item.setDiscoveredAt("2026-03-01 09:00:00");
    item.setViewed(false);

    PropertyEntity entity =
        mapper.toEntity(item, null, "2026-03-01 12:00:00", 0L, 0.0, null, "ACTIVE");

    assertNotNull(entity);
    assertEquals("Otthon Centrum_456", entity.getPropertyId());
    assertEquals("https://oc.hu/456", entity.getUrl());
    assertEquals("Szeged", entity.getCity());
    assertEquals("szeged", entity.getNormalizedCity());
    assertEquals(40_000_000L, entity.getPriceAmount());
    assertEquals(80.0, entity.getAreaSqm());
    assertEquals("2026-03-01 09:00:00", entity.getDiscoveredAt());
    assertEquals("2026-03-01 12:00:00", entity.getUpdatedAt());
    assertFalse(entity.isViewed());
  }

  @Test
  @DisplayName("toEntity preserves existing entity ID and viewed state")
  void testToEntityExisting() {
    PropertyEntity existing = new PropertyEntity();
    existing.setPropertyId("orig_id");
    existing.setUrl("https://dh.hu/1");
    existing.setViewed(true);
    existing.setViewedAt("2026-02-01 10:00:00");
    existing.setDiscoveredAt("2026-02-01 09:00:00");

    IngatlanItem item = new IngatlanItem();
    item.setId("new_id");
    item.setUrl("https://dh.hu/1");
    item.setPortal("Duna House");
    item.setCity("Debrecen");
    item.setViewed(false);

    PropertyEntity updated =
        mapper.toEntity(
            item,
            existing,
            "2026-03-01 12:00:00",
            -500_000L,
            -1.0,
            "2026-03-01 12:00:00",
            "PRICE_DROP");

    assertEquals("orig_id", updated.getPropertyId());
    assertEquals("2026-02-01 09:00:00", updated.getDiscoveredAt());
    assertTrue(updated.isViewed());
    assertEquals("2026-02-01 10:00:00", updated.getViewedAt());
    assertEquals("PRICE_DROP", updated.getStatus());
    assertEquals(-500_000L, updated.getPriceDelta());
  }

  @Test
  @DisplayName("generatePropertyId handles raw ID and fallback hash")
  void testGeneratePropertyId() {
    assertEquals("dh_999", mapper.generatePropertyId("dh", "999", "https://dh.hu/999"));
    String hashed = mapper.generatePropertyId("oc", "", "https://oc.hu/abc");
    assertTrue(hashed.startsWith("oc_"));
  }

  @Test
  @DisplayName("normalizeItem parses price and details if missing")
  void testNormalizeItem() {
    IngatlanItem item = new IngatlanItem();
    item.setPrice("45.5 M Ft");
    item.setDetails("75 m² 3 szoba");

    assertNull(item.getPriceInfo());
    assertNull(item.getDetailsInfo());

    mapper.normalizeItem(item);

    assertNotNull(item.getPriceInfo());
    assertEquals(45_500_000L, item.getPriceInfo().getAmountHuf());
    assertNotNull(item.getDetailsInfo());
    assertEquals(75.0, item.getDetailsInfo().getAreaSquareMeters());
    assertEquals(3.0, item.getDetailsInfo().getRoomCount());
  }

  @Test
  @DisplayName("create and map PriceHistoryEntity and StatusLogEntity")
  void testHistoryAndStatusMapping() {
    PriceHistoryEntity ph =
        mapper.createPriceHistoryEntity(
            "p1",
            "url1",
            "2026-03-01",
            "30 M Ft",
            30_000_000L,
            30.0,
            500_000L,
            32_000_000L,
            -2_000_000L,
            -6.25);
    ph.setId(7L);
    PriceHistoryEntry phEntry = mapper.toPriceHistoryEntry(ph);
    assertNotNull(phEntry);
    assertEquals(7, phEntry.id());
    assertEquals("p1", phEntry.propertyId());
    assertEquals(-2_000_000L, phEntry.priceDelta());

    StatusLogEntity sl =
        mapper.createStatusLogEntity("p1", "url1", "ACTIVE", "2026-03-01", "Kezdeti állapot");
    sl.setId(9L);
    StatusLogEntry slEntry = mapper.toStatusLogEntry(sl);
    assertNotNull(slEntry);
    assertEquals(9, slEntry.id());
    assertEquals("ACTIVE", slEntry.status());
    assertEquals("Kezdeti állapot", slEntry.note());
  }

  @Test
  @DisplayName("PropertyType maps bidirectionally with safe fallback for nulls")
  void testPropertyTypeMappingAndNullFallback() {
    PropertyEntity entity = new PropertyEntity();
    entity.setPropertyType(PropertyType.GARAZS);
    IngatlanItem item = mapper.toItem(entity);
    assertEquals(PropertyType.GARAZS, item.getPropertyType());

    entity.setPropertyType(null);
    IngatlanItem itemNull = mapper.toItem(entity);
    assertEquals(PropertyType.EGYEB_ISMERETLEN, itemNull.getPropertyType());

    IngatlanItem item2 = new IngatlanItem();
    item2.setPropertyType(PropertyType.IPARI_KERESKEDELMI);
    PropertyEntity entity2 = mapper.toEntity(item2, null, "now", 0L, 0.0, null, "ACTIVE");
    assertEquals(PropertyType.IPARI_KERESKEDELMI, entity2.getPropertyType());

    item2.setPropertyType((PropertyType) null);
    PropertyEntity entityNull = mapper.toEntity(item2, null, "now", 0L, 0.0, null, "ACTIVE");
    assertEquals(PropertyType.EGYEB_ISMERETLEN, entityNull.getPropertyType());
  }

  @Test
  @DisplayName("toSourceListing falls back to discovered/updated timestamps and maps renamed fields")
  void testSourceListingFallbacksAndNull() {
    assertNull(mapper.toSourceListing(null));

    PropertyEntity entity = new PropertyEntity();
    entity.setPropertyId("P1");
    entity.setDiscoveredAt("disc");
    entity.setUpdatedAt("upd");
    entity.setPriceAmount(42L);
    entity.setPricePerSqm(7L);
    SourceListing sl = mapper.toSourceListing(entity);
    assertEquals("P1", sl.getExternalListingId());
    assertEquals("disc", sl.getFirstSeenAt());
    assertEquals("upd", sl.getLastSeenAt());
    assertEquals(42L, sl.getPriceHuf());
    assertEquals(7L, sl.getPricePerM2());

    entity.setFirstSeenAt("first");
    entity.setLastSeenAt("last");
    sl = mapper.toSourceListing(entity);
    assertEquals("first", sl.getFirstSeenAt());
    assertEquals("last", sl.getLastSeenAt());
  }

  @Test
  @DisplayName("Observation, history and status log mappings handle nulls and unknown events")
  void testObservationAndLogNullSafety() {
    assertNull(mapper.toListingObservation(null));
    assertNull(mapper.toPriceHistoryEntry(null));
    assertNull(mapper.toStatusLogEntry(null));

    ListingObservationEntity lo =
        mapper.createListingObservationEntity("p", "u", "c", "t", 1L, 2L, "BOGUS", 3L, 4.0);
    assertNull(lo.getId());
    assertEquals("c", lo.getCanonicalId());
    ListingObservation obs = mapper.toListingObservation(lo);
    assertEquals(ListingObservation.StatusEvent.UNCHANGED, obs.getStatusEvent());
    assertEquals(1L, obs.getPriceHuf());

    PriceHistoryEntry entry = mapper.toPriceHistoryEntry(new PriceHistoryEntity());
    assertEquals(0, entry.id());
    assertEquals(0L, entry.priceAmount());
    assertEquals(0.0, entry.priceMillions());
  }}
