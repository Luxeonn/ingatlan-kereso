package ingatlanok;

import ingatlanok.helper.CityUtils;
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
}
