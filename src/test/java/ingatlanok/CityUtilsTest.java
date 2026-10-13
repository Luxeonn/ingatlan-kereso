package ingatlanok;

import ingatlanok.common.CityUtils;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CityUtilsTest {

    @Test
    void testNormalizeCityKey() {
        assertEquals("debrecen", CityUtils.normalizeCityKey("Debrecen"));
        assertEquals("hajduszoboszlo", CityUtils.normalizeCityKey("Hajdúszoboszló"));
        assertEquals("hajduboszormeny", CityUtils.normalizeCityKey("Hajdúböszörmény"));
        assertEquals("balmazujvaros", CityUtils.normalizeCityKey("Balmazújváros"));
        assertEquals("bekescsaba", CityUtils.normalizeCityKey("  Békéscsaba  "));
        assertEquals("debrecen-jozsa", CityUtils.normalizeCityKey("Debrecen - Józsa"));
        assertEquals("debrecen", CityUtils.normalizeCityKey(null));
        assertEquals("debrecen", CityUtils.normalizeCityKey("   "));
    }

    @Test
    void testPredefinedCities() {
        List<Map<String, String>> cities = CityUtils.getPredefinedCities();
        assertNotNull(cities);
        assertFalse(cities.isEmpty());

        boolean hasDebrecen = cities.stream().anyMatch(c -> "Debrecen".equals(c.get("name")));
        boolean hasHajduszoboszlo = cities.stream().anyMatch(c -> "Hajdúszoboszló".equals(c.get("name")));
        boolean hasBudapest = cities.stream().anyMatch(c -> "Budapest".equals(c.get("name")));

        assertTrue(hasDebrecen, "Predefined listának tartalmaznia kell Debrecent");
        assertTrue(hasHajduszoboszlo, "Predefined listának tartalmaznia kell Hajdúszoboszlót");
        assertTrue(hasBudapest, "Predefined listának tartalmaznia kell Budapestet");
    }

    @Test
    void testPresets() {
        Map<String, List<String>> presets = CityUtils.getPresets();
        assertNotNull(presets);
        assertTrue(presets.containsKey("Debrecen és környéke"));
        assertTrue(presets.get("Debrecen és környéke").contains("Debrecen"));
        assertTrue(presets.get("Debrecen és környéke").contains("Hajdúszoboszló"));
    }

    @Test
    void testCountyLookup() {
        assertEquals("hajdu-bihar", CityUtils.getCountySlug("Hajdúszoboszló").orElse(null));
        assertEquals("hajdu-bihar", CityUtils.getCountySlug("Debrecen").orElse(null));
        assertEquals("baranya", CityUtils.getCountySlug("Pécs").orElse(null));
        assertEquals("heves", CityUtils.getCountySlug("Kisnána").orElse(null));
        assertEquals("pest", CityUtils.getCountySlug("Cegléd").orElse(null));
        assertEquals("csongrad-csanad", CityUtils.getCountySlug("Szeged").orElse(null));
        assertEquals("szabolcs-szatmar-bereg", CityUtils.getCountySlug("Nyíregyháza").orElse(null));

        assertEquals("Hajdú-Bihar", CityUtils.getCountyName("Hajdúszoboszló").orElse(null));
        assertEquals("Baranya", CityUtils.getCountyName("Pécs").orElse(null));
    }

    @Test
    void testIsSameOrSubLocation() {
        assertTrue(CityUtils.isSameOrSubLocation("Hajdúszoboszló", "hajduszoboszlo"));
        assertTrue(CityUtils.isSameOrSubLocation("Hajdúszoboszló, Kertváros", "Hajdúszoboszló"));
        assertTrue(CityUtils.isSameOrSubLocation("Debrecen - Józsa", "Debrecen"));
        assertFalse(CityUtils.isSameOrSubLocation("Kisnána", "Hajdúszoboszló"));
        assertFalse(CityUtils.isSameOrSubLocation("Cegléd", "Hajdúszoboszló"));
        assertFalse(CityUtils.isSameOrSubLocation("Pécs", "Hajdúszoboszló"));
    }

    @Test
    void testContainsDifferentKnownCity() {
        // Kisnána URL vagy leírás a cél Hajdúszoboszló keresésnél
        assertTrue(CityUtils.containsDifferentKnownCity("elado-haz-heves-megye-kisnana-falu", "hajduszoboszlo"));
        assertTrue(CityUtils.containsDifferentKnownCity("elado-haz-pest-megye-cegled-oroszlan-utca", "hajduszoboszlo"));
        assertTrue(CityUtils.containsDifferentKnownCity("elado-haz-baranya-megye-pecs-patacs", "hajduszoboszlo"));

        // Ha a célváros benne van, nem minősül más városnak
        assertFalse(CityUtils.containsDifferentKnownCity("elado-haz-hajdu-bihar-megye-hajduszoboszlo-kertvaros", "hajduszoboszlo"));
    }
}
