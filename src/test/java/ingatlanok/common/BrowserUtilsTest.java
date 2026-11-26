package ingatlanok.common;

import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class BrowserUtilsTest {

  @Test
  @DisplayName("openUrl returns false for null and blank URLs")
  void testOpenUrlValidation() {
    assertFalse(BrowserUtils.openUrl(null));
    assertFalse(BrowserUtils.openUrl(""));
    assertFalse(BrowserUtils.openUrl("   "));
  }

  @Test
  @DisplayName("openUrl rejects unapproved schemes and untrusted domains")
  void testOpenUrlDomainAndSchemeValidation() {
    assertFalse(BrowserUtils.openUrl("ftp://dh.hu/ingatlan/123"));
    assertFalse(BrowserUtils.openUrl("file:///etc/passwd"));
    assertFalse(BrowserUtils.openUrl("https://evil-attacker.com/exploit"));
    assertFalse(BrowserUtils.openUrl("javascript:alert(1)"));
    assertFalse(BrowserUtils.openUrl("http://"));
  }
}
