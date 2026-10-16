package ingatlanok.dto;

import ingatlanok.model.IngatlanItem;
import ingatlanok.model.PriceInfo;
import ingatlanok.model.PropertyDetails;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DtoCoverageTest {

    @Test
    @DisplayName("ListingDto toItem és fromItem rekord ellenőrzése")
    void testListingDto() {
        PriceInfoDto price = new PriceInfoDto("100 M Ft", 100_000_000L, 100.0);
        PropertyDetailsDto details = new PropertyDetailsDto("100 m²", 100.0, 3.0, 1_000_000L);

        ListingDto dto = new ListingDto(
                "test-id", "dh", "https://teszt.hu/1", "Teszt Ház", "100 M Ft",
                "Budapest, V. kerület", "Budapest", "100 m²", "2026-10-16",
                true, true, "2026-10-16", "https://img.hu/1.jpg", "ACTIVE",
                -1_000_000L, -1.0, "2026-10-16", price, details
        );

        assertEquals("test-id", dto.id());
        assertEquals("dh", dto.portal());
        assertEquals("https://teszt.hu/1", dto.url());
        assertEquals("Teszt Ház", dto.title());
        assertEquals("Budapest", dto.city());
        assertEquals("2026-10-16", dto.discoveredAt());
        assertTrue(dto.isNew());
        assertTrue(dto.viewed());
        assertEquals("ACTIVE", dto.status());
        assertEquals(-1_000_000L, dto.priceDelta());
        assertEquals(-1.0, dto.priceChangePercent());
        assertEquals(price, dto.priceInfo());
        assertEquals(details, dto.detailsInfo());

        IngatlanItem item = dto.toItem();
        assertNotNull(item);
        assertEquals("test-id", item.getId());
        assertEquals("dh", item.getPortal());

        ListingDto converted = ListingDto.fromItem(item);
        assertNotNull(converted);
        assertEquals("test-id", converted.id());
    }

    @Test
    @DisplayName("PriceHistoryDto és StatusLogDto ellenőrzése")
    void testPriceHistoryAndStatusLogDto() {
        PriceHistoryDto ph = new PriceHistoryDto(1, "p1", "2026-10-16", "50 M Ft", 50_000_000L, 50.0, 1_000_000L, null, null, null);
        assertEquals(1, ph.id());
        assertEquals("p1", ph.propertyId());
        assertEquals("2026-10-16", ph.recordedAt());
        assertEquals("50 M Ft", ph.priceRaw());
        assertEquals(50_000_000L, ph.priceAmount());
        assertEquals(50.0, ph.priceMillions());
        assertNotNull(ph.toModel());

        StatusLogDto sl = new StatusLogDto(1, "p1", "ACTIVE", "2026-10-16", "Új ingatlan");
        assertEquals(1, sl.id());
        assertEquals("p1", sl.propertyId());
        assertEquals("ACTIVE", sl.status());
        assertEquals("2026-10-16", sl.changedAt());
        assertEquals("Új ingatlan", sl.note());
        assertNotNull(sl.toModel());
    }

    @Test
    @DisplayName("ListingFilterDto rekord mezők és gyári metódusok ellenőrzése")
    void testListingFilterDto() {
        ListingFilterDto filter = new ListingFilterDto(
                List.of("Budapest"), List.of("dh"), 10.0, 100.0,
                30.0, 150.0, 1.0, 4.0, true, false, "PRICE_ASC", 10, 0
        );
        assertEquals(List.of("Budapest"), filter.cities());
        assertEquals(List.of("dh"), filter.portals());
        assertEquals(10.0, filter.minPriceMillion());
        assertEquals(100.0, filter.maxPriceMillion());
        assertEquals(30.0, filter.minArea());
        assertEquals(150.0, filter.maxArea());
        assertEquals(1.0, filter.minRooms());
        assertEquals(4.0, filter.maxRooms());
        assertTrue(filter.excludeViewed());
        assertFalse(filter.onlyDiscounted());
        assertEquals("PRICE_ASC", filter.sortBy());
        assertEquals(10, filter.limit());
        assertEquals(0, filter.offset());

        assertNotNull(ListingFilterDto.all());
        assertNotNull(ListingFilterDto.byCity("Debrecen"));
        assertNotNull(ListingFilterDto.discounted("Debrecen"));
    }

    @Test
    @DisplayName("PriceInfoDto és PropertyDetailsDto konverziók")
    void testPriceAndDetailsDto() {
        PriceInfo pi = new PriceInfo("60 M Ft", 60_000_000L, 60.0);
        PriceInfoDto piDto = PriceInfoDto.fromModel(pi);
        assertNotNull(piDto);
        assertEquals(60.0, piDto.amountMillion());
        assertNotNull(piDto.toModel());
        assertNull(PriceInfoDto.fromModel(null));

        PropertyDetails pd = new PropertyDetails("60 m²", 60.0, 2.0, 1_000_000L);
        PropertyDetailsDto pdDto = PropertyDetailsDto.fromModel(pd);
        assertNotNull(pdDto);
        assertEquals(60.0, pdDto.areaSquareMeters());
        assertNotNull(pdDto.toModel());
        assertNull(PropertyDetailsDto.fromModel(null));
    }
}
