package ingatlanok.portal.ingatlanbazar;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ingatlanok.model.SearchCriteria;

class IngatlanBazarLocationResolverTest {

  @Test
  @DisplayName("Miskolc és Békéscsaba keresési URL helyes property_location kóddal épül fel")
  void testBuildSearchUrlUsesCorrectLocationIdsForMiskolcAndBekescsaba() {
    IngatlanBazarQueryBuilder builder = new IngatlanBazarQueryBuilder();

    SearchCriteria miskolc = new SearchCriteria();
    miskolc.setCity("Miskolc");
    miskolc.setPropertyType("lakas");
    miskolc.setMinPriceMillion(15);
    miskolc.setMaxPriceMillion(30);
    miskolc.setMinArea(33);
    miskolc.setMaxArea(38);

    String miskolcUrl = builder.buildSearchUrl(miskolc);
    assertTrue(miskolcUrl.contains("property_location=896"), "Miskolc URL-ben 896 kell legyen: " + miskolcUrl);
    assertTrue(
        builder.buildApiSearchUrl(miskolc, 1).contains("property_location=896"),
        "Miskolc API URL-ben 896 kell legyen");

    SearchCriteria bekescsaba = new SearchCriteria();
    bekescsaba.setCity("Békéscsaba");
    bekescsaba.setPropertyType("lakas");

    String bekescsabaUrl = builder.buildSearchUrl(bekescsaba);
    assertTrue(
        bekescsabaUrl.contains("property_location=617"),
        "Békéscsaba URL-ben 617 kell legyen: " + bekescsabaUrl);
  }

  @Test
  @DisplayName("Előre regisztrált városok valódi Ingatlanbazár településkódokra oldódnak fel")
  void testPreregisteredCitiesResolveToCanonicalLocationIds() {
    assertEquals(Optional.of(1050), IngatlanBazarLocationResolver.resolveLocationId("Budapest"));
    assertEquals(Optional.of(1435), IngatlanBazarLocationResolver.resolveLocationId("Debrecen"));
    assertEquals(Optional.of(1099), IngatlanBazarLocationResolver.resolveLocationId("Szeged"));
    assertEquals(Optional.of(896), IngatlanBazarLocationResolver.resolveLocationId("Miskolc"));
    assertEquals(Optional.of(404), IngatlanBazarLocationResolver.resolveLocationId("Pécs"));
    assertEquals(Optional.of(1260), IngatlanBazarLocationResolver.resolveLocationId("Győr"));
    assertEquals(Optional.of(2499), IngatlanBazarLocationResolver.resolveLocationId("Nyíregyháza"));
    assertEquals(Optional.of(255), IngatlanBazarLocationResolver.resolveLocationId("Kecskemét"));
    assertEquals(Optional.of(1216), IngatlanBazarLocationResolver.resolveLocationId("Székesfehérvár"));
    assertEquals(Optional.of(2842), IngatlanBazarLocationResolver.resolveLocationId("Szombathely"));
    assertEquals(Optional.of(1654), IngatlanBazarLocationResolver.resolveLocationId("Szolnok"));
    assertEquals(Optional.of(1745), IngatlanBazarLocationResolver.resolveLocationId("Tatabánya"));
    assertEquals(Optional.of(2190), IngatlanBazarLocationResolver.resolveLocationId("Kaposvár"));
    assertEquals(Optional.of(617), IngatlanBazarLocationResolver.resolveLocationId("Békéscsaba"));
    assertEquals(Optional.of(3074), IngatlanBazarLocationResolver.resolveLocationId("Veszprém"));
    assertEquals(Optional.of(3300), IngatlanBazarLocationResolver.resolveLocationId("Zalaegerszeg"));
    assertEquals(Optional.of(1499), IngatlanBazarLocationResolver.resolveLocationId("Eger"));
    assertEquals(Optional.of(1870), IngatlanBazarLocationResolver.resolveLocationId("Salgótarján"));
    assertEquals(Optional.of(2624), IngatlanBazarLocationResolver.resolveLocationId("Szekszárd"));
    assertEquals(Optional.of(1462), IngatlanBazarLocationResolver.resolveLocationId("Hajdúszoboszló"));
    assertEquals(Optional.of(1147), IngatlanBazarLocationResolver.resolveLocationId("Dunaújváros"));
    assertEquals(Optional.of(3213), IngatlanBazarLocationResolver.resolveLocationId("Nagykanizsa"));
    assertEquals(Optional.of(1375), IngatlanBazarLocationResolver.resolveLocationId("Sopron"));
    assertEquals(Optional.of(1939), IngatlanBazarLocationResolver.resolveLocationId("Érd"));
    assertEquals(Optional.of(1902), IngatlanBazarLocationResolver.resolveLocationId("Budaörs"));
    assertEquals(Optional.of(1944), IngatlanBazarLocationResolver.resolveLocationId("Gödöllő"));
    assertEquals(Optional.of(2065), IngatlanBazarLocationResolver.resolveLocationId("Vác"));
    assertEquals(Optional.of(1934), IngatlanBazarLocationResolver.resolveLocationId("Dunakeszi"));
    assertEquals(Optional.of(2033), IngatlanBazarLocationResolver.resolveLocationId("Szentendre"));
    assertEquals(Optional.of(2295), IngatlanBazarLocationResolver.resolveLocationId("Siófok"));
  }

  @Test
  @DisplayName("Dinamikus geo-match JSON elemző a pontos településnév-egyezést részesíti előnyben")
  void testParseLocationIdFromJsonPrefersExactSlugMatch() {
    String gyorMatches = """
        {"data": [
          {"id": 1285, "name": "Gyóró"},
          {"id": 2558, "name": "Györe"},
          {"id": 1260, "name": "Győr"}
        ]}
        """;
    assertEquals(
        Optional.of(1260),
        IngatlanBazarLocationResolver.parseLocationIdFromJson(gyorMatches, "Győr"));

    String erdMatches = """
        {"data": [
          {"id": 1042, "name": "Erdőbénye"},
          {"id": 948, "name": "Erdőhorváti"},
          {"id": 1939, "name": "Érd"}
        ]}
        """;
    assertEquals(
        Optional.of(1939),
        IngatlanBazarLocationResolver.parseLocationIdFromJson(erdMatches, "Érd"));

    String fallbackMatches = """
        {"data": [
          {"id": 777, "name": "Valamifalva"},
          {"id": 888, "name": "Másikfalva"}
        ]}
        """;
    assertEquals(
        Optional.of(777),
        IngatlanBazarLocationResolver.parseLocationIdFromJson(fallbackMatches, "Ismeretlen"));

    assertEquals(Optional.empty(), IngatlanBazarLocationResolver.parseLocationIdFromJson("", "Miskolc"));
    assertEquals(Optional.empty(), IngatlanBazarLocationResolver.parseLocationIdFromJson("{}", "Miskolc"));
  }
}
