package ingatlanok.model;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Adversarial Domain Model Stress Tests")
class AdversarialDomainModelStressTest {

  @Test
  @DisplayName("1.1 Mixed active and inactive sources: returns lowest ACTIVE source price")
  void testMixedActiveAndInactiveSources() {
    Property property = new Property();
    property.setCanonicalId("CANONICAL-TEST-1");

    SourceListing activeHigh = new SourceListing();
    activeHigh.setUrl("https://portal1.hu/1");
    activeHigh.setStatus("ACTIVE");
    activeHigh.setPriceHuf(50_000_000L);

    SourceListing activeLow = new SourceListing();
    activeLow.setUrl("https://portal2.hu/2");
    activeLow.setStatus("ACTIVE");
    activeLow.setPriceHuf(45_000_000L);

    SourceListing inactiveLowest = new SourceListing();
    inactiveLowest.setUrl("https://portal3.hu/3");
    inactiveLowest.setStatus("INACTIVE");
    inactiveLowest.setPriceHuf(30_000_000L);

    SourceListing archivedLower = new SourceListing();
    archivedLower.setUrl("https://portal4.hu/4");
    archivedLower.setStatus("ARCHIVED");
    archivedLower.setPriceHuf(20_000_000L);

    property.setSources(List.of(activeHigh, activeLow, inactiveLowest, archivedLower));

    assertEquals(4, property.getSourcesCount());
    assertEquals(2, property.getActiveSources().size());
    // Inactive prices (30M, 20M) must NOT be chosen over active 45M
    assertEquals(45_000_000L, property.calculateMinActivePriceHuf());
    assertEquals(45_000_000L, property.getMinActivePriceHuf());
  }

  @Test
  @DisplayName("1.2 All inactive sources: handles gracefully without throwing exceptions")
  void testAllInactiveSources() {
    Property property = new Property();
    property.setCanonicalId("CANONICAL-TEST-2");

    SourceListing s1 = new SourceListing();
    s1.setUrl("https://portal1.hu/1");
    s1.setStatus("INACTIVE");
    s1.setPriceHuf(50_000_000L);

    SourceListing s2 = new SourceListing();
    s2.setUrl("https://portal2.hu/2");
    s2.setStatus("ARCHIVED");
    s2.setPriceHuf(42_000_000L);

    property.setSources(List.of(s1, s2));

    assertEquals(2, property.getSourcesCount());
    assertTrue(property.getActiveSources().isEmpty());
    assertDoesNotThrow(() -> {
      Long minPrice = property.calculateMinActivePriceHuf();
      // Implementation falls back to minimum of existing sources when no active sources exist
      assertEquals(42_000_000L, minPrice);
    });
  }

  @Test
  @DisplayName("1.3 Empty sources list: returns null gracefully without throwing exceptions")
  void testEmptySourcesList() {
    Property property = new Property();
    property.setSources(Collections.emptyList());

    assertEquals(0, property.getSourcesCount());
    assertTrue(property.getActiveSources().isEmpty());
    assertNull(property.calculateMinActivePriceHuf());
    assertNull(property.getMinActivePriceHuf());

    // Null sources
    property.setSources(null);
    assertEquals(0, property.getSourcesCount());
    assertTrue(property.getActiveSources().isEmpty());
    assertNull(property.calculateMinActivePriceHuf());
    assertNull(property.getMinActivePriceHuf());
  }

  @Test
  @DisplayName("1.4 Mixed null, zero, negative prices: ignores invalid prices and selects minimum positive")
  void testNullZeroNegativePrices() {
    Property property = new Property();

    SourceListing sNull = new SourceListing();
    sNull.setUrl("https://portal1.hu/1");
    sNull.setStatus("ACTIVE");
    sNull.setPriceHuf(null);

    SourceListing sZero = new SourceListing();
    sZero.setUrl("https://portal2.hu/2");
    sZero.setStatus("ACTIVE");
    sZero.setPriceHuf(0L);

    SourceListing sNegative = new SourceListing();
    sNegative.setUrl("https://portal3.hu/3");
    sNegative.setStatus("ACTIVE");
    sNegative.setPriceHuf(-50_000_000L);

    SourceListing sValidHigh = new SourceListing();
    sValidHigh.setUrl("https://portal4.hu/4");
    sValidHigh.setStatus("ACTIVE");
    sValidHigh.setPriceHuf(60_000_000L);

    SourceListing sValidLow = new SourceListing();
    sValidLow.setUrl("https://portal5.hu/5");
    sValidLow.setStatus("ACTIVE");
    sValidLow.setPriceHuf(40_000_000L);

    property.setSources(List.of(sNull, sZero, sNegative, sValidHigh, sValidLow));

    assertEquals(40_000_000L, property.calculateMinActivePriceHuf());

    // What if ALL active sources have non-positive prices?
    Property allInvalid = new Property();
    allInvalid.setSources(List.of(sNull, sZero, sNegative));
    assertNull(allInvalid.calculateMinActivePriceHuf());
  }

  @Test
  @DisplayName("1.5 Null elements in sources list: test robustness against NullPointerException")
  void testNullElementInSourcesList() {
    Property property = new Property();

    // addSource with null is safely ignored
    property.addSource(null);
    assertEquals(0, property.getSourcesCount());

    // If a list containing null is passed to setSources
    List<SourceListing> listWithNull = Arrays.asList(new SourceListing(), null);
    property.setSources(listWithNull);
    assertEquals(2, property.getSourcesCount());

    // Calling getActiveSources currently throws NPE if sources contains null element
    assertThrows(NullPointerException.class, () -> property.getActiveSources());
    assertThrows(NullPointerException.class, () -> property.calculateMinActivePriceHuf());
  }

  @Test
  @DisplayName("1.6 Immutability and defensive copying stress tests")
  void testImmutabilityAndDefensiveCopying() {
    List<SourceListing> externalList = new ArrayList<>();
    SourceListing s1 = new SourceListing();
    s1.setUrl("https://p1.hu/1");
    s1.setStatus("ACTIVE");
    s1.setPriceHuf(50_000_000L);
    externalList.add(s1);

    // Constructor defensive copy test
    Property prop =
        new Property(
            "CAN-1",
            "debrecen",
            "Debrecen",
            PropertyType.LAKAS,
            50.0,
            2.0,
            "Debrecen",
            null,
            externalList);

    // Mutate the external list passed to constructor
    externalList.clear();
    assertEquals(1, prop.getSourcesCount(), "Constructor must defensively copy sources list");

    // setSources defensive copy test
    externalList.add(s1);
    prop.setSources(externalList);
    externalList.clear();
    assertEquals(1, prop.getSourcesCount(), "setSources must defensively copy sources list");

    // getSources() returns internal list reference (mutable)
    List<SourceListing> internalRef = prop.getSources();
    assertNotNull(internalRef);
    internalRef.clear();
    assertEquals(0, prop.getSourcesCount(), "getSources() exposes mutable list reference");
  }

  @Test
  @DisplayName("1.7 Pre-set minActivePriceHuf caching behavior")
  void testPreSetMinActivePriceHuf() {
    Property prop = new Property();
    prop.setMinActivePriceHuf(99_000_000L);

    SourceListing s = new SourceListing();
    s.setUrl("https://p.hu/1");
    s.setStatus("ACTIVE");
    s.setPriceHuf(40_000_000L);
    prop.addSource(s);

    // getMinActivePriceHuf returns pre-set price if not null
    assertEquals(99_000_000L, prop.getMinActivePriceHuf());
    // calculateMinActivePriceHuf dynamically computes from sources
    assertEquals(40_000_000L, prop.calculateMinActivePriceHuf());

    // When reset to null, getMinActivePriceHuf computes dynamically
    prop.setMinActivePriceHuf(null);
    assertEquals(40_000_000L, prop.getMinActivePriceHuf());
  }

  @Test
  @DisplayName("1.8 Property equals and hashCode contract under canonicalId asymmetry")
  void testPropertyEqualsAndHashCodeContract() {
    Property propA = new Property();
    propA.setCanonicalId("CANONICAL-A");
    propA.setNormalizedCity("debrecen");
    propA.setPropertyType(PropertyType.LAKAS);
    propA.setFloorAreaSquareMeters(50.0);
    propA.setRoomCount(2.0);

    Property propB = new Property();
    propB.setCanonicalId(null); // No canonicalId
    propB.setNormalizedCity("debrecen");
    propB.setPropertyType(PropertyType.LAKAS);
    propB.setFloorAreaSquareMeters(50.0);
    propB.setRoomCount(2.0);

    // When canonicalId is null on one side, equals falls back to matching physical attributes
    assertEquals(propA, propB, "Equals falls back to physical attributes when one canonicalId is null");
    assertEquals(propB, propA, "Equals must be symmetric");

    // But hashCode() uses canonicalId if non-null, and physical attributes if null!
    assertNotEquals(
        propA.hashCode(),
        propB.hashCode(),
        "HashCode violates Object contract when canonicalId is asymmetric");

    // Demonstrate failure in Set lookup
    Set<Property> set = new HashSet<>();
    set.add(propA);
    assertFalse(
        set.contains(propB),
        "HashSet lookup fails for equal object due to hashCode asymmetry");
  }

  @Test
  @DisplayName("2.1 SourceListing status case sensitivity and null handling")
  void testSourceListingStatusEdgeCases() {
    SourceListing source = new SourceListing();

    // Default status
    assertEquals("ACTIVE", source.getStatus());
    assertTrue(source.isActive());

    // Case insensitivity
    source.setStatus("active");
    assertTrue(source.isActive());
    assertEquals("active", source.getStatus());

    source.setStatus("Active");
    assertTrue(source.isActive());

    source.setStatus("INACTIVE");
    assertFalse(source.isActive());

    source.setStatus("archived");
    assertFalse(source.isActive());

    // Status null edge case: getStatus() returns "ACTIVE", but isActive() returns false!
    source.setStatus(null);
    assertEquals("ACTIVE", source.getStatus(), "getStatus() defaults null to ACTIVE");
    assertFalse(source.isActive(), "isActive() checks status field directly and returns false when null");
  }

  @Test
  @DisplayName("2.2 ListingObservation status events and pricePerM2 tracking")
  void testListingObservationEventsAndCalculations() {
    // StatusEvent enum parsing
    assertEquals(ListingObservation.StatusEvent.NEW, ListingObservation.StatusEvent.fromString("NEW"));
    assertEquals(ListingObservation.StatusEvent.NEW, ListingObservation.StatusEvent.fromString("new"));
    assertEquals(
        ListingObservation.StatusEvent.PRICE_CHANGED,
        ListingObservation.StatusEvent.fromString("price_changed"));
    assertEquals(
        ListingObservation.StatusEvent.RELISTED,
        ListingObservation.StatusEvent.fromString("  RELISTED  "));
    assertEquals(
        ListingObservation.StatusEvent.UNCHANGED,
        ListingObservation.StatusEvent.fromString("unchanged"));

    // Fallbacks
    assertEquals(
        ListingObservation.StatusEvent.UNCHANGED,
        ListingObservation.StatusEvent.fromString(null));
    assertEquals(
        ListingObservation.StatusEvent.UNCHANGED,
        ListingObservation.StatusEvent.fromString(""));
    assertEquals(
        ListingObservation.StatusEvent.UNCHANGED,
        ListingObservation.StatusEvent.fromString("   "));
    assertEquals(
        ListingObservation.StatusEvent.UNCHANGED,
        ListingObservation.StatusEvent.fromString("INVALID_EVENT"));

    // Constructor with String status
    ListingObservation obs =
        new ListingObservation(
            "2026-10-24 14:00:00",
            45_000_000L,
            900_000L,
            "PRICE_CHANGED",
            -2_000_000L,
            -4.25);

    assertEquals("2026-10-24 14:00:00", obs.getObservedAt());
    assertEquals(45_000_000L, obs.getPriceHuf());
    assertEquals(900_000L, obs.getPricePerM2());
    assertEquals(ListingObservation.StatusEvent.PRICE_CHANGED, obs.getStatusEvent());
    assertEquals(-2_000_000L, obs.getPriceDelta());
    assertEquals(-4.25, obs.getChangePercent());

    // Null safety in setStatusEvent
    obs.setStatusEvent((ListingObservation.StatusEvent) null);
    assertEquals(ListingObservation.StatusEvent.UNCHANGED, obs.getStatusEvent());

    obs.setStatusEvent((String) null);
    assertEquals(ListingObservation.StatusEvent.UNCHANGED, obs.getStatusEvent());
  }

  @Test
  @DisplayName("2.3 ListingObservation price per m2 mathematical relationship")
  void testListingObservationPricePerM2Math() {
    long priceHuf = 45_000_000L;
    double areaSqm = 50.0;
    long expectedPricePerM2 = Math.round((double) priceHuf / areaSqm); // 900,000

    ListingObservation obs = new ListingObservation();
    obs.setPriceHuf(priceHuf);
    obs.setPricePerM2(expectedPricePerM2);

    assertEquals(900_000L, obs.getPricePerM2());
    assertEquals(priceHuf, Math.round(obs.getPricePerM2() * areaSqm));

    // Edge case: Odd division
    long oddPrice = 45_555_555L;
    double oddArea = 63.7;
    long calculatedPerM2 = Math.round((double) oddPrice / oddArea);
    obs.setPriceHuf(oddPrice);
    obs.setPricePerM2(calculatedPerM2);

    assertEquals(715_158L, obs.getPricePerM2());
  }
}
