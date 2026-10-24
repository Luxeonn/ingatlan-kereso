package ingatlanok.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ModelClassesTest {

  @Test
  @DisplayName("PortalSource mappings and metadata")
  void testPortalSource() {
    PortalSource dh = PortalSource.of("Duna House");
    assertEquals("dh", dh.getPortalId());
    assertEquals("Duna House", dh.getPortalName());
    assertTrue(dh.isDirectListing());
    assertNotNull(dh.getThemeColor());

    PortalSource oc = PortalSource.of("Otthon Centrum");
    assertEquals("oc", oc.getPortalId());
    assertEquals("Otthon Centrum", oc.getPortalName());

    PortalSource jf = PortalSource.of("Jófogás Ingatlan");
    assertEquals("jofogas", jf.getPortalId());

    PortalSource ic = PortalSource.of("ingatlan.com");
    assertEquals("ingatlancom", ic.getPortalId());
    assertFalse(ic.isDirectListing());

    PortalSource ib = PortalSource.of("Ingatlanbazár");
    assertEquals("ingatlanbazar", ib.getPortalId());

    PortalSource unknown = PortalSource.of(null);
    assertEquals("unknown", unknown.getPortalId());
  }

  @Test
  @DisplayName("PriceInfo construction and formatting")
  void testPriceInfo() {
    PriceInfo priceInfo = new PriceInfo("45.5 M Ft", 45_500_000L, 45.5);
    assertEquals(45_500_000L, priceInfo.getAmountHuf());
    assertEquals(45.5, priceInfo.getAmountMillion());
    assertEquals("45.5 M Ft", priceInfo.getRawText());
    assertEquals("HUF", priceInfo.getCurrency());
    assertTrue(priceInfo.toString().contains("45.5"));

    PriceInfo emptyInfo = new PriceInfo();
    assertNull(emptyInfo.getRawText());
  }

  @Test
  @DisplayName("PropertyDetails sqm and room count")
  void testPropertyDetails() {
    PropertyDetails details = new PropertyDetails("60 m²", 60.0, 2.5, 750_000L);
    assertEquals("60 m²", details.getRawText());
    assertEquals(60.0, details.getAreaSquareMeters());
    assertEquals(2.5, details.getRoomCount());
    assertEquals(750_000L, details.getPricePerSquareMeter());
    assertTrue(details.toString().contains("60"));
  }

  @Test
  @DisplayName("IngatlanItem equality, setters, getters, and flags")
  void testIngatlanItem() {
    IngatlanItem item1 = new IngatlanItem();
    item1.setUrl("https://dh.hu/ingatlan/123");
    item1.setTitle("Szép lakás");
    item1.setPrice("50 M Ft");
    item1.setCity("Debrecen");
    item1.setPortal("Duna House");
    item1.setNew(true);
    item1.setDiscoveredAt("2026-10-13");
    item1.setDetails("55 m²");

    assertEquals("https://dh.hu/ingatlan/123", item1.getUrl());
    assertEquals("Szép lakás", item1.getTitle());
    assertEquals("50 M Ft", item1.getPrice());
    assertEquals("Debrecen", item1.getCity());
    assertEquals("Duna House", item1.getPortal());
    assertTrue(item1.isNew());
    assertEquals("2026-10-13", item1.getDiscoveredAt());
    assertEquals("55 m²", item1.getDetails());

    IngatlanItem item2 = new IngatlanItem();
    item2.setUrl("https://dh.hu/ingatlan/123");

    assertEquals(item1, item2);
    assertEquals(item1.hashCode(), item2.hashCode());
    assertNotNull(item1.toString());

    PriceInfo priceInfo = new PriceInfo("50 M Ft", 50_000_000L, 50.0);
    PropertyDetails details = new PropertyDetails("55 m²", 55.0, 2.0, 909_090L);
    item1.setPriceInfo(priceInfo);
    item1.setDetailsInfo(details);
    item1.setViewed(true);
    item1.setViewedAt("2026-10-13 22:00:00");

    assertEquals(priceInfo, item1.getPriceInfo());
    assertEquals(details, item1.getDetailsInfo());
    assertTrue(item1.isViewed());
    assertEquals("2026-10-13 22:00:00", item1.getViewedAt());
  }

  @Test
  @DisplayName("SearchResult aggregation and portal statistics")
  void testSearchResult() {
    IngatlanItem item1 = new IngatlanItem();
    item1.setUrl("https://dh.hu/1");
    item1.setPortal("Duna House");
    item1.setNew(true);

    IngatlanItem item2 = new IngatlanItem();
    item2.setUrl("https://oc.hu/2");
    item2.setPortal("Otthon Centrum");
    item2.setNew(false);

    SearchResult result = new SearchResult();
    result.setItems(List.of(item1, item2));
    result.setPortalCounts(Map.of("dh", 1, "oc", 1));
    result.setTotalFound(2);
    result.setNewCount(1);

    assertEquals(2, result.getItems().size());
    assertEquals(2, result.getTotalFound());
    assertEquals(1, result.getNewCount());
    assertEquals(1, result.getPortalCounts().get("dh"));
    assertEquals(1, result.getPortalCounts().get("oc"));
  }

  @Test
  @DisplayName("SearchCriteria area and price defaults and clone")
  void testSearchCriteriaModel() {
    SearchCriteria criteria = new SearchCriteria();
    assertEquals(70, criteria.getMinPriceMillion());
    assertEquals(120, criteria.getMaxPriceMillion());
    assertEquals(90, criteria.getMinArea());
    assertEquals(200, criteria.getMaxArea());

    criteria.setMinArea(60);
    criteria.setMaxArea(180);
    criteria.setExcludeViewed(true);
    assertEquals(60, criteria.getMinArea());
    assertEquals(180, criteria.getMaxArea());
    assertTrue(criteria.isExcludeViewed());

    SearchCriteria clone = criteria.cloneForCity("Miskolc");
    assertEquals("Miskolc", clone.getCity());
    assertEquals(60, clone.getMinArea());
    assertEquals(180, clone.getMaxArea());
    assertEquals(70, clone.getMinPriceMillion());
    assertEquals(120, clone.getMaxPriceMillion());
    assertTrue(clone.isExcludeViewed());
  }

  @Test
  @DisplayName("IngatlanItem and SidecarItem imageUrl getter setter and mapping")
  void testImageUrlHandling() {
    IngatlanItem item = new IngatlanItem();
    assertNull(item.getImageUrl());
    item.setImageUrl("https://example.com/photo.jpg");
    assertEquals("https://example.com/photo.jpg", item.getImageUrl());
    item.setViewedAt("2026-10-13 22:00:00");
    assertEquals("2026-10-13 22:00:00", item.getViewedAt());

    ingatlanok.portal.sidecar.SidecarResponse.SidecarItem sidecarItem =
        new ingatlanok.portal.sidecar.SidecarResponse.SidecarItem(
            "sc1",
            "https://example.com/prop",
            "Szép lakás",
            85.0,
            "85 M Ft",
            100.0,
            3.0,
            "https://example.com/img.jpg");
    assertEquals("https://example.com/img.jpg", sidecarItem.getImageUrl());

    IngatlanItem converted =
        sidecarItem.toIngatlanItem(PortalSource.of("dh"), "Debrecen", "2026-10-13", true);
    assertEquals("https://example.com/img.jpg", converted.getImageUrl());

    sidecarItem.setImageUrl("https://example.com/updated.jpg");
    assertEquals("https://example.com/updated.jpg", sidecarItem.getImageUrl());
  }

  @Test
  @DisplayName("IngatlanItem propertyType and overloaded constructor")
  void testIngatlanItemPropertyType() {
    IngatlanItem item = new IngatlanItem();
    assertEquals(PropertyType.EGYEB_ISMERETLEN, item.getPropertyType());

    item.setPropertyType(PropertyType.HAZ);
    assertEquals(PropertyType.HAZ, item.getPropertyType());

    item.setPropertyType("telek");
    assertEquals(PropertyType.TELEK, item.getPropertyType());

    item.setPropertyType((PropertyType) null);
    assertEquals(PropertyType.EGYEB_ISMERETLEN, item.getPropertyType());

    IngatlanItem overloaded = new IngatlanItem(
        "id1",
        PortalSource.of("dh"),
        "https://dh.hu/1",
        "Családi ház",
        new PriceInfo("80 M Ft", 80_000_000L, 80.0),
        "Debrecen",
        new PropertyDetails("120 m²", 120.0, 4.0, 666_667L),
        "2026-10-24 10:00:00",
        true,
        PropertyType.HAZ
    );
    assertEquals(PropertyType.HAZ, overloaded.getPropertyType());
    assertEquals("Családi ház", overloaded.getTitle());
    assertTrue(overloaded.isNew());
  }

  @Test
  @DisplayName("PriceInfo and PropertyDetails setters and edge cases")
  void testPriceInfoAndPropertyDetailsCoverage() {
    PriceInfo priceInfo = new PriceInfo();
    priceInfo.setAmountHuf(60_000_000L);
    priceInfo.setAmountMillion(60.0);
    priceInfo.setRawText("60 M Ft");
    priceInfo.setCurrency("HUF");

    assertEquals(60_000_000L, priceInfo.getAmountHuf());
    assertEquals(60.0, priceInfo.getAmountMillion());
    assertEquals("60 M Ft", priceInfo.getRawText());
    assertEquals("HUF", priceInfo.getCurrency());

    PropertyDetails details = new PropertyDetails();
    details.setAreaSquareMeters(70.0);
    details.setRoomCount(3.0);
    details.setPricePerSquareMeter(857_143L);
    details.setRawText("70 m² 3 szoba");

    assertEquals(70.0, details.getAreaSquareMeters());
    assertEquals(3.0, details.getRoomCount());
    assertEquals(857_143L, details.getPricePerSquareMeter());
    assertEquals("70 m² 3 szoba", details.getRawText());
  }

  @Test
  @DisplayName("IngatlanItem qualityScore, qualityDeductions and getRawCity coverage")
  void testQualityScoreAndCityMethods() {
    IngatlanItem item = new IngatlanItem();
    assertNull(item.getQualityScore());
    assertNotNull(item.getQualityDeductions());
    assertTrue(item.getQualityDeductions().isEmpty());
    assertNull(item.getRawCity());
    assertEquals("Debrecen", item.getCity());

    item.setCity("Szeged");
    assertEquals("Szeged", item.getRawCity());
    assertEquals("Szeged", item.getCity());

    item.setQualityScore(85);
    assertEquals(85, item.getQualityScore());

    item.setQualityDeductions(List.of("Deduction 1", "Deduction 2"));
    assertEquals(2, item.getQualityDeductions().size());
    assertEquals("Deduction 1", item.getQualityDeductions().getFirst());

    item.setQualityDeductions(null);
    assertNotNull(item.getQualityDeductions());
    assertTrue(item.getQualityDeductions().isEmpty());
  }
}
