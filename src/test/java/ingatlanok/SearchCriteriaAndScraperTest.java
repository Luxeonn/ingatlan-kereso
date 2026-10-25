package ingatlanok;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import ingatlanok.model.IngatlanItem;
import ingatlanok.model.SearchCriteria;

class SearchCriteriaAndScraperTest {

  @Test
  void testSearchCriteriaDefaultsAndPortals() {
    SearchCriteria criteria = new SearchCriteria();

    assertEquals("Hajdúszoboszló", criteria.getCity());
    assertEquals("haz", criteria.getPropertyType());
    assertEquals(70, criteria.getMinPriceMillion());
    assertEquals(120, criteria.getMaxPriceMillion());
    assertEquals(90, criteria.getMinArea());
    assertEquals(200, criteria.getMaxArea());
    assertTrue(criteria.isPortalEnabled("dh"));
    assertTrue(criteria.isPortalEnabled("oc"));
    assertTrue(criteria.isPortalEnabled("jofogas"));
    assertTrue(
        criteria.isPortalEnabled("ingatlancom"), "Ingatlan.com should be enabled by default");

    criteria.setMinPriceMillion(30);
    criteria.setMaxPriceMillion(60);
    criteria.setMinArea(80);
    criteria.setMaxArea(150);
    criteria.setPropertyType("telek");
    criteria.setMinRooms(2);

    assertEquals(30, criteria.getMinPriceMillion());
    assertEquals(60, criteria.getMaxPriceMillion());
    assertEquals(80, criteria.getMinArea());
    assertEquals(150, criteria.getMaxArea());
    assertEquals("telek", criteria.getPropertyType());
    assertEquals(2, criteria.getMinRooms());

    criteria.setPortalEnabled("custom", true);
    assertTrue(criteria.isPortalEnabled("custom"));
    criteria.setPortalEnabled("custom", false);
    assertFalse(criteria.isPortalEnabled("custom"));

    criteria.setPortals(null);
    criteria.setPortalEnabled("dh", true);
    assertTrue(criteria.isPortalEnabled("dh"));

    criteria.setCity(null);
    criteria.setCities(null);
    assertEquals("Hajdúszoboszló", criteria.getCity());

    criteria.setCity("Szeged");
    assertEquals("Szeged", criteria.getCity());

    assertTrue(SearchCriteria.matchesCity(null, "Debrecen"));
    assertTrue(SearchCriteria.matchesCity(new IngatlanItem(), null));
    assertTrue(SearchCriteria.matchesCity(new IngatlanItem(), ""));

    IngatlanItem item = new IngatlanItem();
    item.setCity("Miskolc");
    item.setUrl("https://test.hu/miskolc-lakas");
    assertTrue(SearchCriteria.matchesCity(item, "Miskolc"));

    item.setCity("Eger");
    item.setUrl(null);
    item.setTitle("Eladó lakás Eger belvárosában");
    assertTrue(SearchCriteria.matchesCity(item, "Eger"));
  }

}
