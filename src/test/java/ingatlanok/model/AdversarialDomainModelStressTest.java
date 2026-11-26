package ingatlanok.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Adversarial Domain Model Stress Tests")
class AdversarialDomainModelStressTest {

  @Test
  @DisplayName("2.1 SourceListing status case sensitivity and null handling")
  void testSourceListingStatusEdgeCases() {
    SourceListing source = new SourceListing();

    // Default status
    assertEquals(ingatlanok.model.ListingStatus.ACTIVE, source.getStatus());
    assertTrue(source.isActive());

    // Case insensitivity
    source.setStatus("active");
    assertTrue(source.isActive());
    assertEquals(ingatlanok.model.ListingStatus.ACTIVE, source.getStatus());

    source.setStatus("Active");
    assertTrue(source.isActive());

    source.setStatus("INACTIVE");
    assertFalse(source.isActive());

    source.setStatus("archived");
    assertFalse(source.isActive());

    // Status null edge case: getStatus() returns "ACTIVE", but isActive() returns false!
    source.setStatus((ingatlanok.model.ListingStatus) null);
    assertEquals(ingatlanok.model.ListingStatus.ACTIVE, source.getStatus(), "getStatus() defaults null to ACTIVE");
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
