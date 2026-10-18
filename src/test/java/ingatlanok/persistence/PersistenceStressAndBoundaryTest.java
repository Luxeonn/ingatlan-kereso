package ingatlanok.persistence;

import ingatlanok.common.AppLogger;
import ingatlanok.dto.ListingDto;
import ingatlanok.dto.ListingFilterDto;
import ingatlanok.model.IngatlanItem;
import ingatlanok.model.PriceHistoryEntry;
import ingatlanok.model.PriceInfo;
import ingatlanok.model.PropertyDetails;
import ingatlanok.model.StatusLogEntry;
import ingatlanok.persistence.entity.PriceHistoryEntity;
import ingatlanok.persistence.entity.PropertyEntity;
import ingatlanok.persistence.entity.StatusLogEntity;
import ingatlanok.persistence.mapper.JpaEntityMapper;
import ingatlanok.persistence.service.ListingFilterService;
import ingatlanok.persistence.service.PriceHistoryTracker;
import ingatlanok.persistence.service.PriceHistoryTracker.PriceDeltaCalculation;
import ingatlanok.persistence.service.StatusLogTracker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Persistence God-Class Decomposition — Empirical Adversarial Stress & Boundary Tests")
class PersistenceStressAndBoundaryTest {

    @Nested
    @DisplayName("1. ListingFilterService Adversarial Challenges")
    class ListingFilterServiceChallenges {

        private ListingFilterService filterService;
        private List<IngatlanItem> sampleItems;

        @BeforeEach
        void setUp() {
            filterService = new ListingFilterService();

            IngatlanItem i1 = new IngatlanItem();
            i1.setId("item_1");
            i1.setUrl("https://portal1.hu/1");
            i1.setPortal("PortalA");
            i1.setCity("Budapest");
            i1.setTitle("Belvárosi garzon");
            i1.setDiscoveredAt("2026-03-01 10:00:00");
            i1.setPriceInfo(new PriceInfo("45 M Ft", 45_000_000L, 45.0));
            i1.setDetailsInfo(new PropertyDetails("40 m2, 1 szoba", 40.0, 1.0, 1_125_000L));
            i1.setViewed(false);
            i1.setPriceDelta(-3_000_000L);

            IngatlanItem i2 = new IngatlanItem();
            i2.setId("item_2");
            i2.setUrl("https://portal2.hu/2");
            i2.setPortal("PortalB");
            i2.setCity("Debrecen");
            i2.setTitle("Kertes családi ház");
            i2.setDiscoveredAt("2026-03-02 12:00:00");
            i2.setPriceInfo(new PriceInfo("95 M Ft", 95_000_000L, 95.0));
            i2.setDetailsInfo(new PropertyDetails("150 m2, 5 szoba", 150.0, 5.0, 633_333L));
            i2.setViewed(true);
            i2.setPriceDelta(0L);

            IngatlanItem i3 = new IngatlanItem();
            i3.setId("item_3");
            i3.setUrl("https://portal1.hu/3");
            i3.setPortal("PortalA");
            i3.setCity("Szeged");
            i3.setTitle("Panellakás");
            i3.setDiscoveredAt("2026-03-03 14:00:00");
            i3.setPriceInfo(new PriceInfo("35 M Ft", 35_000_000L, 35.0));
            i3.setDetailsInfo(new PropertyDetails("55 m2, 2 szoba", 55.0, 2.0, 636_363L));
            i3.setViewed(false);
            i3.setPriceDelta(2_000_000L);

            sampleItems = List.of(i1, i2, i3);
        }

        @Test
        @DisplayName("Challenge 1.1: Null filter, null collection, and empty collections")
        void testNullAndEmptyInputs() {
            assertTrue(filterService.filter(null, null).isEmpty());
            assertTrue(filterService.filter(Collections.emptyList(), null).isEmpty());
            assertTrue(filterService.filterItems(null, null).isEmpty());
            assertTrue(filterService.filterItems(Collections.emptyList(), null).isEmpty());

            List<ListingDto> dtoList = filterService.filter(sampleItems, null);
            assertEquals(3, dtoList.size());

            List<IngatlanItem> itemList = filterService.filterItems(sampleItems, null);
            assertEquals(3, itemList.size());
        }

        @Test
        @DisplayName("Challenge 1.2: Empty, blank, and non-existent cities and portals")
        void testEmptyAndNonExistentCitiesAndPortals() {
            ListingFilterDto emptyCityListFilter = new ListingFilterDto(
                    Collections.emptyList(), null, null, null, null, null, null, null, false, false, null, null, null
            );
            assertEquals(3, filterService.filter(sampleItems, emptyCityListFilter).size());

            ListingFilterDto emptyPortalListFilter = new ListingFilterDto(
                    null, Collections.emptyList(), null, null, null, null, null, null, false, false, null, null, null
            );
            assertEquals(3, filterService.filter(sampleItems, emptyPortalListFilter).size());

            ListingFilterDto nonExistentCityFilter = new ListingFilterDto(
                    List.of("NonExistentAtlantis_123"), null, null, null, null, null, null, null, false, false, null, null, null
            );
            assertTrue(filterService.filter(sampleItems, nonExistentCityFilter).isEmpty());

            ListingFilterDto nonExistentPortalFilter = new ListingFilterDto(
                    null, List.of("UnknownPortal_XYZ"), null, null, null, null, null, null, false, false, null, null, null
            );
            assertTrue(filterService.filter(sampleItems, nonExistentPortalFilter).isEmpty());

            IngatlanItem nullPortalCityItem = new IngatlanItem();
            nullPortalCityItem.setId("item_nulls");
            nullPortalCityItem.setUrl("https://portal.hu/nulls");
            nullPortalCityItem.setPortal(null);
            nullPortalCityItem.setCity(null);

            ListingFilterDto portalFilter = new ListingFilterDto(
                    null, List.of("PortalA"), null, null, null, null, null, null, false, false, null, null, null
            );
            assertDoesNotThrow(() -> filterService.filter(List.of(nullPortalCityItem), portalFilter));
            assertTrue(filterService.filter(List.of(nullPortalCityItem), portalFilter).isEmpty());
        }

        @Test
        @DisplayName("Challenge 1.3: Extreme min/max price, area, rooms and inverted boundaries")
        void testExtremeAndInvertedBoundaries() {
            ListingFilterDto extremeMinPrice = new ListingFilterDto(
                    null, null, Double.MAX_VALUE, null, null, null, null, null, false, false, null, null, null
            );
            assertTrue(filterService.filter(sampleItems, extremeMinPrice).isEmpty());

            ListingFilterDto extremeMaxPrice = new ListingFilterDto(
                    null, null, null, -100.0, null, null, null, null, false, false, null, null, null
            );
            assertTrue(filterService.filter(sampleItems, extremeMaxPrice).isEmpty());

            ListingFilterDto invertedPrice = new ListingFilterDto(
                    null, null, 100.0, 30.0, null, null, null, null, false, false, null, null, null
            );
            assertTrue(filterService.filter(sampleItems, invertedPrice).isEmpty());

            ListingFilterDto nanPrice = new ListingFilterDto(
                    null, null, Double.NaN, Double.NaN, null, null, null, null, false, false, null, null, null
            );
            assertTrue(filterService.filter(sampleItems, nanPrice).isEmpty());

            ListingFilterDto extremeMinArea = new ListingFilterDto(
                    null, null, null, null, 999_999.0, null, null, null, false, false, null, null, null
            );
            assertTrue(filterService.filter(sampleItems, extremeMinArea).isEmpty());

            ListingFilterDto invertedArea = new ListingFilterDto(
                    null, null, null, null, 200.0, 30.0, null, null, false, false, null, null, null
            );
            assertTrue(filterService.filter(sampleItems, invertedArea).isEmpty());

            ListingFilterDto extremeRooms = new ListingFilterDto(
                    null, null, null, null, null, null, 100.0, null, false, false, null, null, null
            );
            assertTrue(filterService.filter(sampleItems, extremeRooms).isEmpty());
        }

        @Test
        @DisplayName("Challenge 1.4: Non-existent sorting keys, casing variations, and missing field comparators")
        void testSortingKeysAndMissingFieldComparators() {
            ListingFilterDto unknownSort = new ListingFilterDto(
                    null, null, null, null, null, null, null, null, false, false, "NON_EXISTENT_KEY", null, null
            );
            List<ListingDto> defaultSorted = filterService.filter(sampleItems, unknownSort);
            assertEquals(3, defaultSorted.size());
            assertEquals("item_3", defaultSorted.get(0).id());

            ListingFilterDto lowerCaseSort = new ListingFilterDto(
                    null, null, null, null, null, null, null, null, false, false, "price_asc", null, null
            );
            List<ListingDto> ascSorted = filterService.filter(sampleItems, lowerCaseSort);
            assertEquals("item_3", ascSorted.get(0).id()); // 35M lowest
            assertEquals("item_2", ascSorted.get(2).id()); // 95M highest

            IngatlanItem noPriceItem = new IngatlanItem();
            noPriceItem.setId("no_price");
            noPriceItem.setUrl("https://x.hu/np");
            noPriceItem.setDiscoveredAt("2026-03-04 10:00:00");

            List<IngatlanItem> itemsWithNoPrice = List.of(sampleItems.get(0), noPriceItem);

            List<ListingDto> sortedAsc = filterService.filter(itemsWithNoPrice, new ListingFilterDto(
                    null, null, null, null, null, null, null, null, false, false, "PRICE_ASC", null, null));
            assertEquals("item_1", sortedAsc.get(0).id());
            assertEquals("no_price", sortedAsc.get(1).id());

            List<ListingDto> sortedDesc = filterService.filter(itemsWithNoPrice, new ListingFilterDto(
                    null, null, null, null, null, null, null, null, false, false, "PRICE_DESC", null, null));
            assertEquals("item_1", sortedDesc.get(0).id());
            assertEquals("no_price", sortedDesc.get(1).id());
        }

        @Test
        @DisplayName("Challenge 1.5: Negative and extreme offset/limit values")
        void testNegativeAndExtremeOffsetLimit() {
            ListingFilterDto negativeFilter = new ListingFilterDto(
                    null, null, null, null, null, null, null, null, false, false, "PRICE_ASC", -5, -10
            );
            List<ListingDto> resultNeg = filterService.filter(sampleItems, negativeFilter);
            assertEquals(3, resultNeg.size());

            ListingFilterDto zeroFilter = new ListingFilterDto(
                    null, null, null, null, null, null, null, null, false, false, "PRICE_ASC", 0, 0
            );
            List<ListingDto> resultZero = filterService.filter(sampleItems, zeroFilter);
            assertEquals(3, resultZero.size());

            ListingFilterDto hugeOffset = new ListingFilterDto(
                    null, null, null, null, null, null, null, null, false, false, "PRICE_ASC", 10, 1000
            );
            assertTrue(filterService.filter(sampleItems, hugeOffset).isEmpty());

            ListingFilterDto hugeLimit = new ListingFilterDto(
                    null, null, null, null, null, null, null, null, false, false, "PRICE_ASC", 1000, 1
            );
            assertEquals(2, filterService.filter(sampleItems, hugeLimit).size());

            ListingFilterDto normalLimit = new ListingFilterDto(
                    null, null, null, null, null, null, null, null, false, false, "PRICE_ASC", 1000, null
            );
            assertEquals(3, filterService.filter(sampleItems, normalLimit).size());
        }
    }

    @Nested
    @DisplayName("2. PriceHistoryTracker Adversarial Challenges")
    class PriceHistoryTrackerChallenges {

        private PriceHistoryTracker tracker;

        @BeforeEach
        void setUp() {
            tracker = new PriceHistoryTracker();
        }

        @Test
        @DisplayName("Challenge 2.1: Zero and negative new prices handled safely without crash")
        void testZeroAndNegativeNewPrice() {
            PriceDeltaCalculation calcZero = tracker.calculate(
                    false, 50_000_000L, 0L, -500_000L, -1.0, "2026-03-01", "ACTIVE", "Ház", "2026-03-02"
            );
            assertFalse(calcZero.priceChanged());
            assertFalse(calcZero.isNewPriceRecord());
            assertEquals(-500_000L, calcZero.delta());
            assertEquals(-1.0, calcZero.changePercent());
            assertEquals("ACTIVE", calcZero.resolvedStatus());

            PriceDeltaCalculation calcNeg = tracker.calculate(
                    false, 50_000_000L, -10_000_000L, 0L, 0.0, null, "ACTIVE", "Ház", "2026-03-02"
            );
            assertFalse(calcNeg.priceChanged());
            assertFalse(calcNeg.isNewPriceRecord());
            assertEquals(0L, calcNeg.delta());

            PriceDeltaCalculation calcNull = tracker.calculate(
                    false, 50_000_000L, null, 0L, 0.0, null, "ACTIVE", "Ház", "2026-03-02"
            );
            assertFalse(calcNull.priceChanged());
        }

        @Test
        @DisplayName("Challenge 2.2: Division by zero edge cases (prevAmount <= 0 or null)")
        void testDivisionByZeroEdgeCases() {
            assertDoesNotThrow(() -> {
                PriceDeltaCalculation calcZeroPrev = tracker.calculate(
                        false, 0L, 50_000_000L, 0L, 0.0, null, "ACTIVE", "Ház", "2026-03-02"
                );
                assertFalse(calcZeroPrev.priceChanged());
                assertFalse(Double.isInfinite(calcZeroPrev.changePercent()));
                assertFalse(Double.isNaN(calcZeroPrev.changePercent()));
            });

            assertDoesNotThrow(() -> {
                PriceDeltaCalculation calcNegPrev = tracker.calculate(
                        false, -5_000_000L, 50_000_000L, 0L, 0.0, null, "ACTIVE", "Ház", "2026-03-02"
                );
                assertFalse(calcNegPrev.priceChanged());
            });

            assertDoesNotThrow(() -> {
                PriceDeltaCalculation calcNullPrev = tracker.calculate(
                        false, null, 50_000_000L, 0L, 0.0, null, "ACTIVE", "Ház", "2026-03-02"
                );
                assertFalse(calcNullPrev.priceChanged());
            });
        }

        @Test
        @DisplayName("Challenge 2.3: Extreme price jumps (1 Ft to 1 Billion Ft and vice-versa)")
        void testExtremePriceJumps() {
            PriceDeltaCalculation skyRocket = tracker.calculate(
                    false, 1L, 1_000_000_000L, 0L, 0.0, null, "ACTIVE", "Ultra Mansion", "2026-03-02"
            );
            assertTrue(skyRocket.priceChanged());
            assertEquals(999_999_999L, skyRocket.delta());
            assertEquals("PRICE_INCREASE", skyRocket.resolvedStatus());
            assertEquals(99_999_999_900.0, skyRocket.changePercent());

            PriceDeltaCalculation totalCrash = tracker.calculate(
                    false, 1_000_000_000L, 1L, 0L, 0.0, null, "ACTIVE", "Ruined Estate", "2026-03-02"
            );
            assertTrue(totalCrash.priceChanged());
            assertEquals(-999_999_999L, totalCrash.delta());
            assertEquals("PRICE_DROP", totalCrash.resolvedStatus());
            assertEquals(-100.0, totalCrash.changePercent());
        }

        @Test
        @DisplayName("Challenge 2.4: Unchanged prices preserve previous status and metrics")
        void testUnchangedPrice() {
            PriceDeltaCalculation unchanged = tracker.calculate(
                    false, 45_000_000L, 45_000_000L, -2_000_000L, -4.26, "2026-03-01 10:00:00",
                    "PRICE_DROP", "Lakás", "2026-03-02 12:00:00"
            );
            assertFalse(unchanged.priceChanged());
            assertFalse(unchanged.isNewPriceRecord());
            assertEquals(-2_000_000L, unchanged.delta());
            assertEquals(-4.26, unchanged.changePercent());
            assertEquals("2026-03-01 10:00:00", unchanged.lastPriceChangeAt());
            assertEquals("PRICE_DROP", unchanged.resolvedStatus());
        }

        @Test
        @DisplayName("Challenge 2.5: Null and blank metadata resilience (null title, null now, blank status)")
        void testNullAndBlankMetadata() {
            PriceDeltaCalculation res = tracker.calculate(
                    false, 40_000_000L, 42_000_000L, 0L, 0.0, null, "   ", null, null
            );
            assertTrue(res.priceChanged());
            assertEquals("PRICE_INCREASE", res.resolvedStatus());
            assertNull(res.lastPriceChangeAt());
        }
    }

    @Nested
    @DisplayName("3. JpaEntityMapper Adversarial Challenges")
    class MapperChallenges {

        private JpaEntityMapper jpaMapper;

        @BeforeEach
        void setUp() {
            jpaMapper = new JpaEntityMapper();
        }

        @Test
        @DisplayName("Challenge 3.1: JpaEntityMapper handling of null and corrupted entities")
        void testJpaEntityMapperNullSafety() {
            assertNull(jpaMapper.toItem(null));
            assertTrue(jpaMapper.toItems(null).isEmpty());
            assertTrue(jpaMapper.toItems(Collections.emptyList()).isEmpty());

            PropertyEntity emptyEntity = new PropertyEntity();
            IngatlanItem mappedItem = jpaMapper.toItem(emptyEntity);
            assertNotNull(mappedItem);
            assertNull(mappedItem.getPriceInfo());
            assertNull(mappedItem.getDetailsInfo());
            assertNull(mappedItem.getId());
            assertFalse(mappedItem.isNew());
            assertFalse(mappedItem.isViewed());

            PropertyEntity partialPriceEntity = new PropertyEntity();
            partialPriceEntity.setPriceAmount(30_000_000L);
            partialPriceEntity.setPriceMillions(null);
            IngatlanItem partialPriceItem = jpaMapper.toItem(partialPriceEntity);
            assertNotNull(partialPriceItem.getPriceInfo());
            assertEquals(30_000_000L, partialPriceItem.getPriceInfo().getAmountHuf());
            assertEquals(0.0, partialPriceItem.getPriceInfo().getAmountMillion());

            assertNull(jpaMapper.toPriceHistoryEntry(null));
            assertNull(jpaMapper.toStatusLogEntry(null));

            PriceHistoryEntity emptyPh = new PriceHistoryEntity();
            PriceHistoryEntry phEntry = jpaMapper.toPriceHistoryEntry(emptyPh);
            assertNotNull(phEntry);
            assertEquals(0, phEntry.id());
            assertEquals(0L, phEntry.priceAmount());
            assertEquals(0.0, phEntry.priceMillions());

            StatusLogEntity emptySl = new StatusLogEntity();
            StatusLogEntry slEntry = jpaMapper.toStatusLogEntry(emptySl);
            assertNotNull(slEntry);
            assertEquals(0, slEntry.id());
        }

        @Test
        @DisplayName("Challenge 3.2: JpaEntityMapper toEntity with missing fields on IngatlanItem")
        void testJpaEntityMapperToEntityMissingFields() {
            IngatlanItem minimalItem = new IngatlanItem();
            minimalItem.setUrl("https://test.hu/1");

            PropertyEntity entity = jpaMapper.toEntity(
                    minimalItem, null, "2026-03-01 10:00:00", 0L, 0.0, null, "ACTIVE"
            );
            assertNotNull(entity);
            assertEquals("Egyéb", entity.getPortal());
            assertEquals("Debrecen", entity.getCity());
            assertEquals("debrecen", entity.getNormalizedCity());
            assertEquals("", entity.getTitle());
            assertEquals("", entity.getPriceRaw());
            assertNull(entity.getPriceAmount());
            assertNull(entity.getAreaSqm());

            minimalItem.setCity("Ismeretlen");
            PropertyEntity entityWithExplicitCity = jpaMapper.toEntity(
                    minimalItem, null, "2026-03-01 10:00:00", 0L, 0.0, null, "ACTIVE"
            );
            assertEquals("Ismeretlen", entityWithExplicitCity.getCity());
            assertEquals("ismeretlen", entityWithExplicitCity.getNormalizedCity());

            assertThrows(NullPointerException.class, () ->
                    jpaMapper.toEntity(null, null, "2026-03-01", 0L, 0.0, null, "ACTIVE")
            );
        }
    }

    @Nested
    @DisplayName("4. High-Volume & Concurrency Stress Verification")
    class ConcurrencyAndVolumeStress {

        @Test
        @DisplayName("Challenge 4.1: High-throughput multithreaded filtering on 10,000 items")
        void testConcurrentHighThroughputFiltering() throws Exception {
            ListingFilterService service = new ListingFilterService();

            List<IngatlanItem> largeBatch = new ArrayList<>(10_000);
            for (int i = 0; i < 10_000; i++) {
                IngatlanItem item = new IngatlanItem();
                item.setId("item_" + i);
                item.setUrl("https://portal.hu/" + i);
                item.setPortal(i % 2 == 0 ? "PortalA" : "PortalB");
                item.setCity(i % 3 == 0 ? "Budapest" : (i % 3 == 1 ? "Debrecen" : "Szeged"));
                item.setPriceInfo(new PriceInfo((20 + (i % 80)) + " M Ft", (long) (20 + (i % 80)) * 1_000_000L, (double) (20 + (i % 80))));
                item.setDetailsInfo(new PropertyDetails((30 + (i % 100)) + " m2", (double) (30 + (i % 100)), (double) (1 + (i % 5)), 500_000L));
                item.setDiscoveredAt("2026-03-01 10:00:00");
                item.setViewed(i % 5 == 0);
                item.setPriceDelta(i % 4 == 0 ? -1_000_000L : 0L);
                largeBatch.add(item);
            }

            int threads = 8;
            ExecutorService executor = Executors.newFixedThreadPool(threads);
            try {
                List<Callable<Integer>> tasks = IntStream.range(0, 50).mapToObj(runIdx -> (Callable<Integer>) () -> {
                    ListingFilterDto filter = new ListingFilterDto(
                            List.of("Budapest"),
                            List.of("PortalA"),
                            30.0, 70.0,
                            40.0, 90.0,
                            1.0, 4.0,
                            true,
                            runIdx % 2 == 0,
                            "PRICE_DESC",
                            20,
                            0
                    );
                    List<ListingDto> result = service.filter(largeBatch, filter);
                    return result.size();
                }).toList();

                List<Future<Integer>> futures = executor.invokeAll(tasks);
                for (Future<Integer> f : futures) {
                    Integer count = f.get(5, TimeUnit.SECONDS);
                    assertNotNull(count);
                    assertTrue(count <= 20);
                }
            } finally {
                executor.shutdownNow();
            }
        }

        @Test
        @DisplayName("Challenge 4.2: High-volume price delta computations across 100,000 randomized records")
        void testHighVolumePriceDeltaComputations() {
            PriceHistoryTracker tracker = new PriceHistoryTracker();
            Random rng = new Random(42);

            // Temporarily disable synchronous file I/O to benchmark pure computation throughput
            AppLogger.setFileLoggingEnabled(false);
            AppLogger.setConsoleLoggingEnabled(false);
            try {
                long start = System.nanoTime();
                for (int i = 0; i < 100_000; i++) {
                    boolean isNew = (i % 10 == 0);
                    Long prev = isNew ? null : (long) (10_000_000 + rng.nextInt(90_000_000));
                    Long next = (long) (10_000_000 + rng.nextInt(90_000_000));
                    PriceDeltaCalculation calc = tracker.calculate(
                            isNew, prev, next, 0L, 0.0, null, "ACTIVE", "Batch #" + i, "2026-03-01"
                    );
                    assertNotNull(calc);
                    if (isNew) {
                        assertTrue(calc.isNewPriceRecord());
                        assertEquals(0L, calc.delta());
                    } else if (!prev.equals(next)) {
                        assertTrue(calc.priceChanged());
                        assertEquals(next - prev, calc.delta());
                    }
                }
                long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
                assertTrue(elapsedMs < 2000, "100k pure computations took " + elapsedMs + "ms, expected < 2000ms");
            } finally {
                AppLogger.setFileLoggingEnabled(true);
                AppLogger.setConsoleLoggingEnabled(true);
            }
        }
    }
}
