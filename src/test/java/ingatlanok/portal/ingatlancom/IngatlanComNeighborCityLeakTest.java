package ingatlanok.portal.ingatlancom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import ingatlanok.common.CityUtils;
import ingatlanok.model.IngatlanItem;
import ingatlanok.model.SearchCriteria;

/** Regression tests for issue #495 (neighboring settlements leaking into target city results). */
class IngatlanComNeighborCityLeakTest {

  private static final String TARGET = "Hajdúszoboszló";

  @Test
  void sidecarKeepsRealSettlementWhenTitleStartsWithDifferentSettlement() {
    assertEquals("Ebes, Kertész utca 4.",
        IngatlanComSidecarStrategy.resolveLocationText("Ebes, Kertész utca 4.", TARGET));
    assertEquals("Nagyhegyes, Ipari út 8.",
        IngatlanComSidecarStrategy.resolveLocationText("Nagyhegyes, Ipari út 8.", TARGET));
  }

  @Test
  void sidecarFallsBackToSearchedCityForMatchingOrUnstructuredTitles() {
    assertEquals(TARGET,
        IngatlanComSidecarStrategy.resolveLocationText("Hajdúszoboszló, Szép utca 1.", TARGET));
    assertEquals(TARGET, IngatlanComSidecarStrategy.resolveLocationText("Családi ház", TARGET));
    assertEquals(TARGET, IngatlanComSidecarStrategy.resolveLocationText(null, TARGET));
    assertNull(IngatlanComSidecarStrategy.resolveLocationText("Ebes, Fő utca 1.", null));
  }

  @Test
  void cityFilterRejectsUnknownNeighboringSettlementPrefix() {
    assertFalse(SearchCriteria.matchesCity(item("Nagyhegyes, Ipari út 8.", "Nagyhegyes"), TARGET));
    assertFalse(SearchCriteria.matchesCity(item("Ebes, Hajdú-Bihar megye", "Ebes"), TARGET));
  }

  @Test
  void cityFilterKeepsTargetCityAndNonSettlementPrefixes() {
    assertTrue(SearchCriteria.matchesCity(item("Hajdúszoboszló, Fő utca 2.", TARGET), TARGET));
    assertTrue(SearchCriteria.matchesCity(item("Kertész utca 4, Hajdúszoboszló", TARGET), TARGET));
    assertTrue(SearchCriteria.matchesCity(item("Hajdú-Bihar megye, Hajdúszoboszló", TARGET), TARGET));
    assertTrue(SearchCriteria.matchesCity(item("Belváros, Hajdúszoboszló", TARGET), TARGET));
  }

  @Test
  void extractLeadingSettlementBoundaries() {
    assertEquals("Ebes", CityUtils.extractLeadingSettlement("Ebes, Kertész utca 4."));
    assertNull(CityUtils.extractLeadingSettlement("Ebes"));
    assertNull(CityUtils.extractLeadingSettlement(null));
    assertNull(CityUtils.extractLeadingSettlement(", Ebes"));
    assertNull(CityUtils.extractLeadingSettlement("XIII. kerület, Budapest"));
    assertNull(CityUtils.extractLeadingSettlement("Kossuth utca 12, Ebes"));
    assertNull(CityUtils.extractLeadingSettlement("Eladó családi ház, Debrecen"));
    assertNull(CityUtils.extractLeadingSettlement("Egy nagyon hosszu nev itt, Debrecen"));
  }

  private static IngatlanItem item(String location, String city) {
    IngatlanItem item = new IngatlanItem();
    item.setLocation(location);
    item.setCity(city);
    item.setTitle(location);
    item.setUrl("https://ingatlan.com/35319732");
    return item;
  }
}
