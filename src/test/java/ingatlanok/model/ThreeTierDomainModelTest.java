package ingatlanok.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import ingatlanok.persistence.entity.ListingObservationEntity;
import ingatlanok.persistence.entity.PropertyEntity;
import ingatlanok.persistence.mapper.JpaEntityMapper;

@DisplayName("3-Tier Domain Architecture Model Tests")
class ThreeTierDomainModelTest {

  @Test
  @DisplayName("SourceListing model maintains properties, active flag, and url equality")
  void testSourceListingModel() {
    SourceListing source1 =
        new SourceListing(
            "dh.hu",
            "DH-12345",
            "https://dh.hu/ingatlan/12345",
            "Szép lakás Debrecenben",
            "https://img.dh.hu/12345.jpg",
            "2026-10-24 10:00:00",
            "2026-10-24 12:00:00",
            "ACTIVE",
            45_000_000L,
            750_000L,
            false,
            null);

    assertEquals("dh.hu", source1.getPortal());
    assertEquals("DH-12345", source1.getExternalListingId());
    assertEquals("https://dh.hu/ingatlan/12345", source1.getUrl());
    assertEquals("Szép lakás Debrecenben", source1.getTitle());
    assertEquals("https://img.dh.hu/12345.jpg", source1.getImageUrl());
    assertEquals("2026-10-24 10:00:00", source1.getFirstSeenAt());
    assertEquals("2026-10-24 12:00:00", source1.getLastSeenAt());
    assertEquals("ACTIVE", source1.getStatus());
    assertTrue(source1.isActive());
    assertEquals(45_000_000L, source1.getPriceHuf());
    assertEquals(750_000L, source1.getPricePerM2());
    assertFalse(source1.isViewed());
    assertNull(source1.getViewedAt());

    SourceListing source2 = new SourceListing();
    source2.setUrl("https://dh.hu/ingatlan/12345");
    source2.setStatus("INACTIVE");
    assertFalse(source2.isActive());

    assertEquals(source1, source2);
    assertEquals(source1.hashCode(), source2.hashCode());
    assertTrue(source1.toString().contains("dh.hu"));

    SourceListing source3 = new SourceListing();
    source3.setUrl("https://oc.hu/ingatlan/999");
    assertNotEquals(source1, source3);
  }

  @Test
  @DisplayName("ListingObservation model correctly tracks time-series snapshots and status events")
  void testListingObservationModel() {
    ListingObservation obs1 =
        new ListingObservation(
            "2026-10-24 12:00:00",
            42_000_000L,
            700_000L,
            ListingObservation.StatusEvent.PRICE_CHANGED,
            -3_000_000L,
            -6.67);

    assertEquals("2026-10-24 12:00:00", obs1.getObservedAt());
    assertEquals(42_000_000L, obs1.getPriceHuf());
    assertEquals(700_000L, obs1.getPricePerM2());
    assertEquals(ListingObservation.StatusEvent.PRICE_CHANGED, obs1.getStatusEvent());
    assertEquals(-3_000_000L, obs1.getPriceDelta());
    assertEquals(-6.67, obs1.getChangePercent());
    assertTrue(obs1.toString().contains("PRICE_CHANGED"));

    ListingObservation obs2 =
        new ListingObservation(
            "2026-10-24 12:00:00",
            42_000_000L,
            700_000L,
            "PRICE_CHANGED",
            -3_000_000L,
            -6.67);
    assertEquals(obs1, obs2);
    assertEquals(obs1.hashCode(), obs2.hashCode());

    assertEquals(
        ListingObservation.StatusEvent.NEW, ListingObservation.StatusEvent.fromString("NEW"));
    assertEquals(
        ListingObservation.StatusEvent.RELISTED,
        ListingObservation.StatusEvent.fromString("relisted"));
    assertEquals(
        ListingObservation.StatusEvent.UNCHANGED,
        ListingObservation.StatusEvent.fromString("unknown_val"));
    assertEquals(
        ListingObservation.StatusEvent.UNCHANGED, ListingObservation.StatusEvent.fromString(null));
  }

  @Test
  @DisplayName("Property model computes min active price and manages source listings")
  void testPropertyModel() {
    SourceListing s1 =
        new SourceListing(
            "dh.hu", "DH-1", "https://dh.hu/1", "Tégla lakás", null, null, null, "ACTIVE", 50_000_000L,
            800_000L, false, null);
    SourceListing s2 =
        new SourceListing(
            "oc.hu", "OC-1", "https://oc.hu/1", "Tégla lakás", null, null, null, "ACTIVE", 48_000_000L,
            768_000L, false, null);
    SourceListing s3 =
        new SourceListing(
            "jofogas.hu", "JF-1", "https://jf.hu/1", "Tégla lakás", null, null, null, "INACTIVE", 40_000_000L,
            640_000L, false, null);

    Property prop =
        new Property(
            "PROP-CANONICAL-1",
            "debrecen",
            "Debrecen",
            PropertyType.LAKAS,
            62.5,
            2.5,
            "Debrecen, Belváros",
            null,
            List.of(s1, s2, s3));

    assertEquals("PROP-CANONICAL-1", prop.getCanonicalId());
    assertEquals("debrecen", prop.getNormalizedCity());
    assertEquals("Debrecen", prop.getCity());
    assertEquals(PropertyType.LAKAS, prop.getPropertyType());
    assertEquals(62.5, prop.getFloorAreaSquareMeters());
    assertEquals(2.5, prop.getRoomCount());
    assertEquals(3, prop.getSourcesCount());
    assertEquals(2, prop.getActiveSources().size());
    // Lowest active price is 48M (ignoring inactive 40M)
    assertEquals(48_000_000L, prop.getMinActivePriceHuf());

    Property propSame = new Property();
    propSame.setCanonicalId("PROP-CANONICAL-1");
    assertEquals(prop, propSame);
    assertEquals(prop.hashCode(), propSame.hashCode());
    assertTrue(prop.toString().contains("PROP-CANONICAL-1"));
  }

  @Test
  @DisplayName("IngatlanItem maintains sourceListings and backwards compatibility")
  void testIngatlanItemThreeTierCompatibility() {
    IngatlanItem item = new IngatlanItem();
    assertEquals(1, item.getSourcesCount());
    assertTrue(item.getSourceListings().isEmpty());

    SourceListing s1 =
        new SourceListing(
            "dh.hu", "DH-1", "https://dh.hu/1", "Lakás", null, null, null, "ACTIVE", 40_000_000L,
            800_000L, false, null);
    item.addSourceListing(s1);

    assertEquals(1, item.getSourcesCount());
    assertEquals(1, item.getSourceListings().size());

    SourceListing s2 =
        new SourceListing(
            "oc.hu", "OC-1", "https://oc.hu/1", "Lakás", null, null, null, "ACTIVE", 39_000_000L,
            780_000L, false, null);
    item.addSourceListing(s2);

    assertEquals(2, item.getSourcesCount());
    assertEquals(2, item.getSourceListings().size());

    item.setSourceListings(null);
    assertEquals(0, item.getSourceListings().size());
    assertEquals(1, item.getSourcesCount());
  }

  @Test
  @DisplayName("JpaEntityMapper maps 3-tier entities and DTOs bidirectionally")
  void testJpaEntityMapperThreeTier() {
    JpaEntityMapper mapper = Mappers.getMapper(JpaEntityMapper.class);

    PropertyEntity entity = new PropertyEntity();
    entity.setPropertyId("DH_123");
    entity.setCanonicalId("CANONICAL-999");
    entity.setUrl("https://dh.hu/123");
    entity.setPortal("dh.hu");
    entity.setTitle("Kertvárosi ház");
    entity.setCity("Debrecen");
    entity.setNormalizedCity("debrecen");
    entity.setLocation("Debrecen");
    entity.setDetails("120 m2, 4 szoba");
    entity.setAreaSqm(120.0);
    entity.setRooms(4.0);
    entity.setPriceAmount(95_000_000L);
    entity.setPriceMillions(95.0);
    entity.setPricePerSqm(791_666L);
    entity.setPropertyType(PropertyType.HAZ);
    entity.setStatus("ACTIVE");
    entity.setFirstSeenAt("2026-10-20 10:00:00");
    entity.setLastSeenAt("2026-10-24 15:00:00");

    SourceListing sl = mapper.toSourceListing(entity);
    assertNotNull(sl);
    assertEquals("https://dh.hu/123", sl.getUrl());
    assertEquals("DH_123", sl.getExternalListingId());
    assertEquals("2026-10-20 10:00:00", sl.getFirstSeenAt());
    assertEquals("2026-10-24 15:00:00", sl.getLastSeenAt());
    assertEquals(95_000_000L, sl.getPriceHuf());

    IngatlanItem item = mapper.toItem(entity);
    assertNotNull(item);
    assertEquals(1, item.getSourcesCount());
    assertEquals(1, item.getSourceListings().size());
    assertEquals("https://dh.hu/123", item.getSourceListings().get(0).getUrl());

    Property prop = mapper.toProperty(entity, List.of(sl));
    assertNotNull(prop);
    assertEquals("CANONICAL-999", prop.getCanonicalId());
    assertEquals(PropertyType.HAZ, prop.getPropertyType());
    assertEquals(95_000_000L, prop.getMinActivePriceHuf());

    ListingObservationEntity obsEntity =
        mapper.createListingObservationEntity(
            "DH_123",
            "https://dh.hu/123",
            "CANONICAL-999",
            "2026-10-24 15:00:00",
            92_000_000L,
            766_666L,
            "PRICE_CHANGED",
            -3_000_000L,
            -3.15);
    ListingObservation obs = mapper.toListingObservation(obsEntity);
    assertNotNull(obs);
    assertEquals(ListingObservation.StatusEvent.PRICE_CHANGED, obs.getStatusEvent());
    assertEquals(-3_000_000L, obs.getPriceDelta());
  }
}
