package ingatlanok;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import ingatlanok.common.SlugUtils;

class SlugUtilsTest {

  @Test
  void testStripAccents() {
    assertEquals("Hajduszoboszlo", SlugUtils.stripAccents("Hajdúszoboszló"));
    assertEquals("Debrecen", SlugUtils.stripAccents("Debrecen"));
    assertEquals("ar", SlugUtils.stripAccents("ár"));
    assertEquals("Szekesfehervar", SlugUtils.stripAccents("Székesfehérvár"));
    assertEquals("", SlugUtils.stripAccents(null));
    assertEquals("", SlugUtils.stripAccents(""));
  }

  @Test
  void testToSlug() {
    assertEquals("debrecen", SlugUtils.toSlug("Debrecen"));
    assertEquals("hajduszoboszlo", SlugUtils.toSlug("Hajdúszoboszló"));
    assertEquals("hajdu-bihar", SlugUtils.toSlug("Hajdú-Bihar"));
    assertEquals("debrecen-jozsa", SlugUtils.toSlug("Debrecen - Józsa"));
    assertEquals("szekesfehervar", SlugUtils.toSlug("  Székesfehérvár  "));
    assertEquals("ar-50-mft", SlugUtils.toSlug("Ár: 50 mFt!"));
    assertEquals("", SlugUtils.toSlug(null));
    assertEquals("", SlugUtils.toSlug("   "));
  }
}
