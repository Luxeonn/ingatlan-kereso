package ingatlanok.persistence;

import ingatlanok.dto.ListingDto;
import ingatlanok.dto.ListingFilterDto;
import ingatlanok.model.IngatlanItem;
import ingatlanok.model.PriceInfo;
import ingatlanok.model.PropertyDetails;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class NitriteListingCriteriaTest {

    @TempDir
    Path tempDir;

    private NitriteDatabaseManager dbManager;
    private NitriteListingStorage storage;

    @BeforeEach
    void setUp() {
        Path dbFile = tempDir.resolve("test_criteria.db");
        dbManager = new NitriteDatabaseManager(dbFile);
        storage = new NitriteListingStorage(dbManager);

        IngatlanItem item1 = new IngatlanItem("1", "Duna House", "https://dh.hu/item1", "Olcsó garzon Debrecenben", "30 M Ft", "Debrecen", "Debrecen", "35 m² 1 szoba", "2026-10-14 10:00:00", true);
        item1.setPriceInfo(new PriceInfo("30 M Ft", 30_000_000L, 30.0));
        item1.setDetailsInfo(new PropertyDetails("35 m² 1 szoba", 35.0, 1.0, 857_142L));

        IngatlanItem item2 = new IngatlanItem("2", "Otthon Centrum", "https://oc.hu/item2", "Tágas lakás Debrecenben", "60 M Ft", "Debrecen", "Debrecen", "80 m² 3 szoba", "2026-10-14 11:00:00", true);
        item2.setPriceInfo(new PriceInfo("60 M Ft", 60_000_000L, 60.0));
        item2.setDetailsInfo(new PropertyDetails("80 m² 3 szoba", 80.0, 3.0, 750_000L));

        IngatlanItem item3 = new IngatlanItem("3", "Jófogás", "https://jofogas.hu/item3", "Luxus villa Budapesten", "150 M Ft", "Budapest", "Budapest", "200 m² 5 szoba", "2026-10-14 12:00:00", true);
        item3.setPriceInfo(new PriceInfo("150 M Ft", 150_000_000L, 150.0));
        item3.setDetailsInfo(new PropertyDetails("200 m² 5 szoba", 200.0, 5.0, 750_000L));

        IngatlanItem item4 = new IngatlanItem("4", "Költözzbe", "https://koltozzbe.hu/item4", "Kertes ház Szeged", "70 M Ft", "Szeged", "Szeged", "100 m² 3 szoba", "2026-10-14 13:00:00", true);
        item4.setPriceInfo(new PriceInfo("70 M Ft", 70_000_000L, 70.0));
        item4.setDetailsInfo(new PropertyDetails("100 m² 3 szoba", 100.0, 3.0, 700_000L));

        storage.saveAll(List.of(item1, item2, item3, item4));

        // Árcsökkenés az item2-re: 60M -> 55M
        IngatlanItem item2Drop = new IngatlanItem("2", "Otthon Centrum", "https://oc.hu/item2", "Tágas lakás Debrecenben", "55 M Ft", "Debrecen", "Debrecen", "80 m² 3 szoba", "2026-10-14 14:00:00", false);
        item2Drop.setPriceInfo(new PriceInfo("55 M Ft", 55_000_000L, 55.0));
        storage.saveAll(List.of(item2Drop));

        // Megtekintettként megjelölve: item1
        storage.markAsViewed("https://dh.hu/item1");
    }

    @AfterEach
    void tearDown() {
        if (dbManager != null) {
            dbManager.close();
        }
    }

    @Test
    @DisplayName("Szűrés város alapján NoSQL Criteria szűréssel")
    void testFilterByCity() {
        ListingFilterDto filter = ListingFilterDto.byCity("Debrecen");
        List<ListingDto> results = storage.findByCriteria(filter);

        assertEquals(2, results.size());
        assertTrue(results.stream().allMatch(dto -> "Debrecen".equalsIgnoreCase(dto.city())));
    }

    @Test
    @DisplayName("Szűrés ársáv alapján NoSQL Criteria szűréssel")
    void testFilterByPriceRange() {
        ListingFilterDto filter = new ListingFilterDto(
                null, null, 50.0, 100.0,
                null, null, null, null,
                false, false, "PRICE_ASC", null, null
        );
        List<ListingDto> results = storage.findByCriteria(filter);

        assertEquals(2, results.size());
        assertEquals("https://oc.hu/item2", results.get(0).url());
        assertEquals("https://koltozzbe.hu/item4", results.get(1).url());
    }

    @Test
    @DisplayName("Szűrés alapterület és szobaszám alapján")
    void testFilterByAreaAndRooms() {
        ListingFilterDto filter = new ListingFilterDto(
                null, null, null, null,
                70.0, 150.0, 2.5, 4.0,
                false, false, "AREA_ASC", null, null
        );
        List<ListingDto> results = storage.findByCriteria(filter);

        assertEquals(2, results.size());
        assertEquals("https://oc.hu/item2", results.get(0).url());
        assertEquals("https://koltozzbe.hu/item4", results.get(1).url());
    }

    @Test
    @DisplayName("Megtekintett ingatlanok kizárása")
    void testExcludeViewed() {
        ListingFilterDto filter = new ListingFilterDto(
                List.of("Debrecen"), null, null, null,
                null, null, null, null,
                true, false, "DISCOVERED_DESC", null, null
        );
        List<ListingDto> results = storage.findByCriteria(filter);

        assertEquals(1, results.size());
        assertEquals("https://oc.hu/item2", results.get(0).url());
        assertFalse(results.get(0).viewed());
    }

    @Test
    @DisplayName("Kizárólag leárazott ingatlanok szűrése")
    void testOnlyDiscounted() {
        ListingFilterDto filter = ListingFilterDto.discounted("Debrecen");
        List<ListingDto> results = storage.findByCriteria(filter);

        assertEquals(1, results.size());
        ListingDto item = results.get(0);
        assertEquals("https://oc.hu/item2", item.url());
        assertNotNull(item.priceDelta());
        assertTrue(item.priceDelta() < 0);
    }

    @Test
    @DisplayName("Lapozás (Pagination) offset és limit támogatással")
    void testPagination() {
        ListingFilterDto filter = new ListingFilterDto(
                null, null, null, null,
                null, null, null, null,
                false, false, "PRICE_ASC", 2, 1
        );
        List<ListingDto> results = storage.findByCriteria(filter);

        assertEquals(2, results.size());
    }
}
