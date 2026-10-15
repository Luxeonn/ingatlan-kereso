package ingatlanok.persistence.builder;

import ingatlanok.dto.ListingDto;
import ingatlanok.dto.ListingFilterDto;
import ingatlanok.dto.PriceInfoDto;
import ingatlanok.dto.PropertyDetailsDto;
import ingatlanok.model.IngatlanItem;
import ingatlanok.model.PriceInfo;
import ingatlanok.model.PropertyDetails;
import ingatlanok.persistence.SqliteDatabaseManager;
import ingatlanok.persistence.SqliteListingStorage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ListingCriteriaQueryBuilderTest {

    @TempDir
    Path tempDir;

    private SqliteDatabaseManager dbManager;
    private SqliteListingStorage storage;

    @BeforeEach
    void setUp() {
        Path dbFile = tempDir.resolve("test_criteria_builder.db");
        dbManager = new SqliteDatabaseManager(dbFile);
        storage = new SqliteListingStorage(dbManager);

        // Test dataset: 4 listings across different portals, cities, price and area ranges
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

        // Generate a price drop on item2: 60 M Ft -> 55 M Ft
        IngatlanItem item2Drop = new IngatlanItem("2", "Otthon Centrum", "https://oc.hu/item2", "Tágas lakás Debrecenben", "55 M Ft", "Debrecen", "Debrecen", "80 m² 3 szoba", "2026-10-14 14:00:00", false);
        item2Drop.setPriceInfo(new PriceInfo("55 M Ft", 55_000_000L, 55.0));
        storage.saveAll(List.of(item2Drop));

        // Mark item1 as viewed
        storage.markAsViewed("https://dh.hu/item1");
    }

    @AfterEach
    void tearDown() {
        if (dbManager != null) {
            dbManager.close();
        }
    }

    @Test
    @DisplayName("Szűrés város alapján Criteria Builderrel")
    void testFilterByCity() {
        ListingFilterDto filter = ListingFilterDto.byCity("Debrecen");
        List<ListingDto> results = storage.findByCriteria(filter);

        assertEquals(2, results.size());
        assertTrue(results.stream().allMatch(dto -> "Debrecen".equalsIgnoreCase(dto.city())));
    }

    @Test
    @DisplayName("Szűrés ársáv alapján Criteria Builderrel")
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
                70.0, 150.0, 3.0, 4.0,
                false, false, "AREA_DESC", null, null
        );
        List<ListingDto> results = storage.findByCriteria(filter);

        assertEquals(2, results.size());
        assertEquals("https://koltozzbe.hu/item4", results.get(0).url()); // 100 m²
        assertEquals("https://oc.hu/item2", results.get(1).url());     // 80 m²
    }

    @Test
    @DisplayName("Szűrés portálok alapján")
    void testFilterByPortals() {
        ListingFilterDto filter = new ListingFilterDto(
                null, List.of("dh", "koltozzbe"), null, null,
                null, null, null, null,
                false, false, "DISCOVERED_DESC", null, null
        );
        List<ListingDto> results = storage.findByCriteria(filter);

        assertEquals(2, results.size());
        assertTrue(results.stream().anyMatch(d -> d.url().contains("dh.hu")));
        assertTrue(results.stream().anyMatch(d -> d.url().contains("koltozzbe.hu")));
    }

    @Test
    @DisplayName("Megtekintett elemek kizárása (excludeViewed)")
    void testExcludeViewed() {
        ListingFilterDto filter = new ListingFilterDto(
                null, null, null, null,
                null, null, null, null,
                true, false, "DISCOVERED_DESC", null, null
        );
        List<ListingDto> results = storage.findByCriteria(filter);

        assertEquals(3, results.size());
        assertFalse(results.stream().anyMatch(d -> d.url().equals("https://dh.hu/item1")));
    }

    @Test
    @DisplayName("Kizárólag leárazott elemek lekérdezése (onlyDiscounted)")
    void testOnlyDiscounted() {
        ListingFilterDto filter = ListingFilterDto.discounted(null);
        List<ListingDto> results = storage.findByCriteria(filter);

        assertEquals(1, results.size());
        ListingDto discounted = results.get(0);
        assertEquals("https://oc.hu/item2", discounted.url());
        assertNotNull(discounted.priceDelta());
        assertTrue(discounted.priceDelta() < 0);
        assertEquals(-5_000_000L, discounted.priceDelta());
    }

    @Test
    @DisplayName("Lapozás (limit és offset) vizsgálata")
    void testPagination() {
        ListingFilterDto page1 = new ListingFilterDto(
                null, null, null, null,
                null, null, null, null,
                false, false, "PRICE_ASC", 2, 0
        );
        List<ListingDto> r1 = storage.findByCriteria(page1);
        assertEquals(2, r1.size());

        ListingFilterDto page2 = new ListingFilterDto(
                null, null, null, null,
                null, null, null, null,
                false, false, "PRICE_ASC", 2, 2
        );
        List<ListingDto> r2 = storage.findByCriteria(page2);
        assertEquals(2, r2.size());

        assertNotEquals(r1.get(0).url(), r2.get(0).url());
    }

    @Test
    @DisplayName("DTO konverzió modellre és vissza")
    void testDtoModelConversion() {
        PriceInfoDto priceDto = new PriceInfoDto("50 M Ft", 50_000_000L, 50.0);
        PriceInfo priceModel = priceDto.toModel();
        assertEquals(50_000_000L, priceModel.getAmountHuf());
        PriceInfoDto roundTripPrice = PriceInfoDto.fromModel(priceModel);
        assertEquals(priceDto, roundTripPrice);

        PropertyDetailsDto detailsDto = new PropertyDetailsDto("60 m²", 60.0, 2.0, 833_333L);
        PropertyDetails detailsModel = detailsDto.toModel();
        assertEquals(60.0, detailsModel.getAreaSquareMeters());
        PropertyDetailsDto roundTripDetails = PropertyDetailsDto.fromModel(detailsModel);
        assertEquals(detailsDto, roundTripDetails);

        IngatlanItem item = new IngatlanItem("x1", "Portal", "https://portal.hu/x1", "Cím", "50 M Ft", "Város", "Város", "60 m²", "2026-10-14 10:00:00", true);
        item.setPriceInfo(priceModel);
        item.setDetailsInfo(detailsModel);
        ListingDto listingDto = ListingDto.fromItem(item);
        assertNotNull(listingDto);
        assertEquals("https://portal.hu/x1", listingDto.url());

        IngatlanItem convertedBack = listingDto.toItem();
        assertEquals(item.getUrl(), convertedBack.getUrl());
        assertEquals(item.getPrice(), convertedBack.getPrice());
    }
}
