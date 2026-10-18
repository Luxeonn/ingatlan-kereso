package ingatlanok.persistence.service;

import ingatlanok.persistence.service.PriceHistoryTracker.PriceDeltaCalculation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("PriceHistoryTracker Unit Tests")
class PriceHistoryTrackerTest {

    private PriceHistoryTracker tracker;

    @BeforeEach
    void setUp() {
        tracker = new PriceHistoryTracker();
    }

    @Test
    @DisplayName("Initial discovery records new price with delta 0")
    void testInitialPriceDiscovery() {
        PriceDeltaCalculation calc = tracker.calculate(
                true, null, 50_000_000L, null, null, null, "ACTIVE", "Családi ház", "2026-03-01 10:00:00"
        );

        assertTrue(calc.isNewPriceRecord());
        assertFalse(calc.priceChanged());
        assertEquals(0L, calc.delta());
        assertEquals(0.0, calc.changePercent());
        assertNull(calc.lastPriceChangeAt());
        assertEquals("ACTIVE", calc.resolvedStatus());
        assertNull(calc.previousPriceAmount());
    }

    @Test
    @DisplayName("Price drop correctly calculates negative delta and PRICE_DROP status")
    void testPriceDrop() {
        PriceDeltaCalculation calc = tracker.calculate(
                false, 60_000_000L, 54_000_000L, 0L, 0.0, null, "ACTIVE", "Családi ház", "2026-03-02 12:00:00"
        );

        assertFalse(calc.isNewPriceRecord());
        assertTrue(calc.priceChanged());
        assertEquals(-6_000_000L, calc.delta());
        assertEquals(-10.0, calc.changePercent());
        assertEquals("PRICE_DROP", calc.resolvedStatus());
        assertEquals("2026-03-02 12:00:00", calc.lastPriceChangeAt());
        assertEquals(60_000_000L, calc.previousPriceAmount());
    }

    @Test
    @DisplayName("Price increase correctly calculates positive delta and PRICE_INCREASE status")
    void testPriceIncrease() {
        PriceDeltaCalculation calc = tracker.calculate(
                false, 50_000_000L, 55_000_000L, 0L, 0.0, null, "ACTIVE", "Családi ház", "2026-03-02 12:00:00"
        );

        assertFalse(calc.isNewPriceRecord());
        assertTrue(calc.priceChanged());
        assertEquals(5_000_000L, calc.delta());
        assertEquals(10.0, calc.changePercent());
        assertEquals("PRICE_INCREASE", calc.resolvedStatus());
        assertEquals("2026-03-02 12:00:00", calc.lastPriceChangeAt());
    }

    @Test
    @DisplayName("Unchanged price preserves existing delta and status")
    void testNoPriceChange() {
        PriceDeltaCalculation calc = tracker.calculate(
                false, 50_000_000L, 50_000_000L, -2_000_000L, -3.85, "2026-03-01 10:00:00",
                "PRICE_DROP", "Családi ház", "2026-03-03 14:00:00"
        );

        assertFalse(calc.isNewPriceRecord());
        assertFalse(calc.priceChanged());
        assertEquals(-2_000_000L, calc.delta());
        assertEquals(-3.85, calc.changePercent());
        assertEquals("2026-03-01 10:00:00", calc.lastPriceChangeAt());
        assertEquals("PRICE_DROP", calc.resolvedStatus());
    }

    @Test
    @DisplayName("Null or zero new price safely handled without arithmetic exception")
    void testNullOrZeroPrice() {
        PriceDeltaCalculation calcNull = tracker.calculate(
                false, 50_000_000L, null, null, null, null, null, "Családi ház", "2026-03-01 10:00:00"
        );
        assertFalse(calcNull.priceChanged());
        assertEquals("ACTIVE", calcNull.resolvedStatus());

        PriceDeltaCalculation calcZero = tracker.calculate(
                false, 50_000_000L, 0L, null, null, null, null, "Családi ház", "2026-03-01 10:00:00"
        );
        assertFalse(calcZero.priceChanged());
        assertEquals("ACTIVE", calcZero.resolvedStatus());
    }

    @Test
    @DisplayName("Percentage rounding rounded to 2 decimal places")
    void testPercentageRounding() {
        PriceDeltaCalculation calc = tracker.calculate(
                false, 30_000_000L, 29_000_000L, 0L, 0.0, null, "ACTIVE", "Lakás", "2026-03-02 12:00:00"
        );
        assertEquals(-1_000_000L, calc.delta());
        assertEquals(-3.33, calc.changePercent());
    }
}
