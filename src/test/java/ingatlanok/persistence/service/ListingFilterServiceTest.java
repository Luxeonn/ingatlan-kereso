package ingatlanok.persistence.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ingatlanok.dto.ListingDto;
import ingatlanok.dto.ListingFilterDto;
import ingatlanok.model.IngatlanItem;
import ingatlanok.model.PriceInfo;
import ingatlanok.model.PropertyDetails;

@DisplayName("ListingFilterService Unit Tests")
class ListingFilterServiceTest {

  private ListingFilterService filterService;
  private IngatlanItem item1;
  private IngatlanItem item2;
  private IngatlanItem item3;

  @BeforeEach
  void setUp() {
    filterService = new ListingFilterService();

    item1 = new IngatlanItem();
    item1.setId("dh_101");
    item1.setUrl("https://dh.hu/101");
    item1.setPortal("Duna House");
    item1.setCity("Budapest");
    item1.setTitle("Szép lakás Budapesten");
    item1.setDiscoveredAt("2026-03-01 10:00:00");
    item1.setPriceInfo(new PriceInfo("50 M Ft", 50_000_000L, 50.0));
    item1.setDetailsInfo(new PropertyDetails("60 m2, 2 szoba", 60.0, 2.0, 833_333L));
    item1.setViewed(false);
    item1.setPriceDelta(-2_000_000L);

    item2 = new IngatlanItem();
    item2.setId("oc_202");
    item2.setUrl("https://oc.hu/202");
    item2.setPortal("Otthon Centrum");
    item2.setCity("Szeged");
    item2.setTitle("Modern ház Szegeden");
    item2.setDiscoveredAt("2026-03-02 12:00:00");
    item2.setPriceInfo(new PriceInfo("80 M Ft", 80_000_000L, 80.0));
    item2.setDetailsInfo(new PropertyDetails("120 m2, 4 szoba", 120.0, 4.0, 666_666L));
    item2.setViewed(true);
    item2.setPriceDelta(0L);

    item3 = new IngatlanItem();
    item3.setId("jofogas_303");
    item3.setUrl("https://jofogas.hu/303");
    item3.setPortal("Jófogás");
    item3.setCity("Győr");
    item3.setTitle("Garázs Győrben");
    item3.setDiscoveredAt("2026-03-03 14:00:00");
    item3.setPriceInfo(new PriceInfo("20 M Ft", 20_000_000L, 20.0));
    item3.setDetailsInfo(new PropertyDetails("30 m2, 1 szoba", 30.0, 1.0, 666_666L));
    item3.setViewed(false);
    item3.setPriceDelta(1_000_000L);
  }

  @Test
  @DisplayName("Null or empty input items returns empty list")
  void testNullOrEmptyItems() {
    assertTrue(filterService.filter(null, null).isEmpty());
    assertTrue(filterService.filter(Collections.emptyList(), null).isEmpty());
    assertTrue(filterService.filterItems(null, null).isEmpty());
    assertTrue(filterService.filterItems(Collections.emptyList(), null).isEmpty());
  }

  @Test
  @DisplayName("Filter by city with diacritics and case tolerance")
  void testFilterByCity() {
    ListingFilterDto filter =
        new ListingFilterDto(
            List.of("budapest", "GYOR"),
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            false,
            false,
            null,
            null,
            null);
    List<ListingDto> result = filterService.filter(List.of(item1, item2, item3), filter);
    assertEquals(2, result.size());
    assertTrue(result.stream().anyMatch(dto -> dto.city().equals("Budapest")));
    assertTrue(result.stream().anyMatch(dto -> dto.city().equals("Győr")));
  }

  @Test
  @DisplayName("Filter by portal case-insensitive")
  void testFilterByPortal() {
    ListingFilterDto filter =
        new ListingFilterDto(
            null,
            List.of("duna house"),
            null,
            null,
            null,
            null,
            null,
            null,
            false,
            false,
            null,
            null,
            null);
    List<ListingDto> result = filterService.filter(List.of(item1, item2, item3), filter);
    assertEquals(1, result.size());
    assertEquals("dh_101", result.get(0).id());
  }

  @Test
  @DisplayName("Filter by price range")
  void testFilterByPriceRange() {
    ListingFilterDto filter =
        new ListingFilterDto(
            null, null, 30.0, 70.0, null, null, null, null, false, false, null, null, null);
    List<ListingDto> result = filterService.filter(List.of(item1, item2, item3), filter);
    assertEquals(1, result.size());
    assertEquals("dh_101", result.get(0).id());
  }

  @Test
  @DisplayName("Filter by area and rooms")
  void testFilterByAreaAndRooms() {
    ListingFilterDto filter =
        new ListingFilterDto(
            null, null, null, null, 50.0, 150.0, 2.0, 4.0, false, false, null, null, null);
    List<ListingDto> result = filterService.filter(List.of(item1, item2, item3), filter);
    assertEquals(2, result.size());
  }

  @Test
  @DisplayName("Filter exclude viewed")
  void testFilterExcludeViewed() {
    ListingFilterDto filter =
        new ListingFilterDto(
            null, null, null, null, null, null, null, null, true, false, null, null, null);
    List<ListingDto> result = filterService.filter(List.of(item1, item2, item3), filter);
    assertEquals(2, result.size());
    assertTrue(result.stream().noneMatch(dto -> dto.id().equals("oc_202")));
  }

  @Test
  @DisplayName("Filter only discounted")
  void testFilterOnlyDiscounted() {
    ListingFilterDto filter =
        new ListingFilterDto(
            null, null, null, null, null, null, null, null, false, true, null, null, null);
    List<ListingDto> result = filterService.filter(List.of(item1, item2, item3), filter);
    assertEquals(1, result.size());
    assertEquals("dh_101", result.get(0).id());
  }

  @Test
  @DisplayName(
      "Sorting combinations (PRICE_ASC, PRICE_DESC, AREA_ASC, AREA_DESC, DISCOUNT_DESC,"
          + " DISCOVERED_DESC)")
  void testSortingComparators() {
    List<IngatlanItem> items = Arrays.asList(item1, item2, item3);

    List<ListingDto> priceAsc =
        filterService.filter(
            items,
            new ListingFilterDto(
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                false,
                false,
                "PRICE_ASC",
                null,
                null));
    assertEquals("jofogas_303", priceAsc.get(0).id()); // 20M
    assertEquals("oc_202", priceAsc.get(2).id()); // 80M

    List<ListingDto> priceDesc =
        filterService.filter(
            items,
            new ListingFilterDto(
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                false,
                false,
                "PRICE_DESC",
                null,
                null));
    assertEquals("oc_202", priceDesc.get(0).id()); // 80M
    assertEquals("jofogas_303", priceDesc.get(2).id()); // 20M

    List<ListingDto> areaAsc =
        filterService.filter(
            items,
            new ListingFilterDto(
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                false,
                false,
                "AREA_ASC",
                null,
                null));
    assertEquals("jofogas_303", areaAsc.get(0).id()); // 30m2
    assertEquals("oc_202", areaAsc.get(2).id()); // 120m2

    List<ListingDto> areaDesc =
        filterService.filter(
            items,
            new ListingFilterDto(
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                false,
                false,
                "AREA_DESC",
                null,
                null));
    assertEquals("oc_202", areaDesc.get(0).id()); // 120m2
    assertEquals("jofogas_303", areaDesc.get(2).id()); // 30m2

    List<ListingDto> discDesc =
        filterService.filter(
            items,
            new ListingFilterDto(
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                false,
                false,
                "DISCOUNT_DESC",
                null,
                null));
    assertEquals("dh_101", discDesc.get(0).id()); // -2M

    List<ListingDto> discovDesc =
        filterService.filter(
            items,
            new ListingFilterDto(
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                false,
                false,
                "DISCOVERED_DESC",
                null,
                null));
    assertEquals("jofogas_303", discovDesc.get(0).id()); // 2026-03-03
  }

  @Test
  @DisplayName("Pagination offset and limit")
  void testPagination() {
    List<IngatlanItem> items = Arrays.asList(item1, item2, item3);
    ListingFilterDto filter =
        new ListingFilterDto(
            null, null, null, null, null, null, null, null, false, false, "PRICE_ASC", 1, 1);
    List<ListingDto> result = filterService.filter(items, filter);
    assertEquals(1, result.size());
    assertEquals("dh_101", result.get(0).id()); // 2nd lowest price (50M)
  }

  @Test
  @DisplayName("filterItems returns IngatlanItem instances")
  void testFilterItems() {
    List<IngatlanItem> result =
        filterService.filterItems(List.of(item1, item2), ListingFilterDto.all());
    assertEquals(2, result.size());
    assertEquals(
        item2, result.get(0)); // default discoveredAt desc: item2 (03-02) before item1 (03-01)
  }
}
