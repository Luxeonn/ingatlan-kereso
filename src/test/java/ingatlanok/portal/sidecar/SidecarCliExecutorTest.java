package ingatlanok.portal.sidecar;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ingatlanok.common.JsonSupport;
import ingatlanok.model.IngatlanItem;
import ingatlanok.model.PortalSource;
import ingatlanok.model.SearchCriteria;

class SidecarCliExecutorUnitTest {

  @AfterEach
  void tearDown() {
    SidecarCliExecutor.resetCustomExecutable();
  }

  @Test
  @DisplayName("SidecarCliExecutor handles null inputs safely")
  void testExecuteWithNullInputs() {
    SearchCriteria criteria = new SearchCriteria();
    Optional<SidecarResponse> resp1 = SidecarCliExecutor.execute(null, criteria);
    assertTrue(resp1.isEmpty());

    Optional<SidecarResponse> resp2 = SidecarCliExecutor.execute("dh", null);
    assertTrue(resp2.isEmpty());
  }

  @Test
  @DisplayName("SidecarCliExecutor custom executable configuration")
  void testCustomExecutable() {
    SidecarCliExecutor.setCustomExecutable("test-fake-sidecar");
    assertTrue(SidecarCliExecutor.isAvailable());

    SidecarCliExecutor.resetCustomExecutable();
  }

  @Test
  @DisplayName("SidecarResponse and SidecarItem model deserialization and conversion")
  void testSidecarResponseMapping() {
    String json =
        """
        {
            "status": "SUCCESS",
            "portalId": "ingatlancom",
            "items": [
                {
                    "id": "12345",
                    "url": "https://ingatlan.com/12345",
                    "title": "Szép lakás",
                    "priceRaw": "65 M Ft",
                    "priceMillion": 65.0,
                    "areaSquareMeters": 72.0,
                    "roomCount": 2.5,
                    "imageUrl": "https://ingatlan.com/photo.jpg"
                }
            ],
            "errorMessage": null
        }
        """;

    SidecarResponse response = JsonSupport.mapper().readValue(json, SidecarResponse.class);
    assertNotNull(response);
    assertTrue(response.isSuccess());
    assertEquals("ingatlancom", response.getPortalId());
    assertEquals(1, response.getItems().size());

    SidecarResponse.SidecarItem item = response.getItems().get(0);
    assertEquals("12345", item.getId());
    assertEquals("https://ingatlan.com/12345", item.getUrl());
    assertEquals(72.0, item.getAreaSquareMeters());
    assertEquals(2.5, item.getRoomCount());

    IngatlanItem domainItem =
        item.toIngatlanItem(
            PortalSource.of("Ingatlan.com"), "Debrecen", "2026-10-13 12:00:00", true);
    assertNotNull(domainItem);
    assertEquals("12345", domainItem.getId());
    assertEquals("https://ingatlan.com/12345", domainItem.getUrl());
    assertTrue(domainItem.isNew());
    assertNotNull(domainItem.getPriceInfo());
    assertEquals(65.0, domainItem.getPriceInfo().getAmountMillion());
    assertNotNull(domainItem.getDetailsInfo());
    assertEquals(72.0, domainItem.getDetailsInfo().getAreaSquareMeters());
    assertEquals(2.5, domainItem.getDetailsInfo().getRoomCount());
  }

  @Test
  @DisplayName("SidecarItem conversion handles missing price and property details")
  void testSidecarItemConversionFallbacks() {
    SidecarResponse.SidecarItem withoutPrice =
        new SidecarResponse.SidecarItem("1", "https://example.test/1", " ", null, null, null, null);
    IngatlanItem unavailable = withoutPrice.toIngatlanItem(null, "Debrecen", "today", false);
    assertEquals("Debrecen - Ingatlan (1)", unavailable.getTitle());
    assertEquals("Ár a hirdetésben", unavailable.getPriceInfo().getRawText());
    assertEquals(0, unavailable.getDetailsInfo().getRawText().length());

    SidecarResponse.SidecarItem withRawPrice =
        new SidecarResponse.SidecarItem("2", "https://example.test/2", "Ház", null, "12 M Ft", null, null);
    IngatlanItem rawPrice = withRawPrice.toIngatlanItem(null, "Szeged", "today", false);
    assertEquals("12 M Ft", rawPrice.getPriceInfo().getRawText());
    assertEquals("Szeged", rawPrice.getCity());
  }
}
