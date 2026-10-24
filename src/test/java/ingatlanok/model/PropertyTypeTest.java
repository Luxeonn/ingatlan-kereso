package ingatlanok.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class PropertyTypeTest {

  @Test
  @DisplayName("All 6 canonical property types have non-blank display name and slug")
  void testCanonicalPropertyTypesMetadata() {
    for (PropertyType type : PropertyType.values()) {
      assertFalse(type.getDisplayName().isBlank());
      assertFalse(type.getSlug().isBlank());
    }
    assertEquals(6, PropertyType.values().length);
  }

  @Test
  @DisplayName("isResidential() is true strictly for LAKAS and HAZ")
  void testIsResidential() {
    assertTrue(PropertyType.LAKAS.isResidential());
    assertTrue(PropertyType.HAZ.isResidential());
    assertFalse(PropertyType.TELEK.isResidential());
    assertFalse(PropertyType.GARAZS.isResidential());
    assertFalse(PropertyType.IPARI_KERESKEDELMI.isResidential());
    assertFalse(PropertyType.EGYEB_ISMERETLEN.isResidential());
  }

  @Test
  @DisplayName("isPlot() is true strictly for TELEK")
  void testIsPlot() {
    assertTrue(PropertyType.TELEK.isPlot());
    assertFalse(PropertyType.LAKAS.isPlot());
    assertFalse(PropertyType.HAZ.isPlot());
    assertFalse(PropertyType.GARAZS.isPlot());
    assertFalse(PropertyType.IPARI_KERESKEDELMI.isPlot());
    assertFalse(PropertyType.EGYEB_ISMERETLEN.isPlot());
  }

  @ParameterizedTest
  @CsvSource({
      "LAKAS, LAKAS",
      "lakas, LAKAS",
      "Lakás, LAKAS",
      "flat, LAKAS",
      "apartment, LAKAS",
      "HAZ, HAZ",
      "haz, HAZ",
      "Ház, HAZ",
      "house, HAZ",
      "üdülő, HAZ",
      "udulo, HAZ",
      "üdülőház, HAZ",
      "udulohaz, HAZ",
      "TELEK, TELEK",
      "telek, TELEK",
      "Telek, TELEK",
      "plot, TELEK",
      "land, TELEK",
      "GARAZS, GARAZS",
      "garazs, GARAZS",
      "Garázs, GARAZS",
      "garage, GARAZS",
      "IPARI_KERESKEDELMI, IPARI_KERESKEDELMI",
      "ipari-kereskedelmi, IPARI_KERESKEDELMI",
      "Ipari / Kereskedelmi, IPARI_KERESKEDELMI",
      "ipari, IPARI_KERESKEDELMI",
      "commercial, IPARI_KERESKEDELMI",
      "industrial, IPARI_KERESKEDELMI",
      "EGYEB_ISMERETLEN, EGYEB_ISMERETLEN",
      "egyeb-ismeretlen, EGYEB_ISMERETLEN",
      "Egyéb / Ismeretlen, EGYEB_ISMERETLEN"
  })
  @DisplayName("fromString correctly resolves canonical names, slugs, and synonyms")
  void testFromStringValid(String raw, PropertyType expected) {
    assertEquals(expected, PropertyType.fromString(raw));
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {"   ", "invalid_type", "unknown_category_xyz"})
  @DisplayName("fromString safely falls back to EGYEB_ISMERETLEN for null, blank, or unknown values")
  void testFromStringFallback(String raw) {
    assertEquals(PropertyType.EGYEB_ISMERETLEN, PropertyType.fromString(raw));
  }
}
