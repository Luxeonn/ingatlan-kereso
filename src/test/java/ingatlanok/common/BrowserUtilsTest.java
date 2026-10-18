package ingatlanok.common;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BrowserUtilsTest {

    @Test
    @DisplayName("openUrl returns false for null and blank URLs")
    void testOpenUrlValidation() {
        assertFalse(BrowserUtils.openUrl(null));
        assertFalse(BrowserUtils.openUrl(""));
        assertFalse(BrowserUtils.openUrl("   "));
    }

    @Test
    @DisplayName("openUrls returns empty list for null or empty collections")
    void testOpenUrlsEmptyCollection() {
        List<String> resultNull = BrowserUtils.openUrls(null, 5);
        assertNotNull(resultNull);
        assertTrue(resultNull.isEmpty());

        List<String> resultEmpty = BrowserUtils.openUrls(Collections.emptyList(), 5);
        assertNotNull(resultEmpty);
        assertTrue(resultEmpty.isEmpty());
    }

    @Test
    @DisplayName("openUrls respects maximum open limit")
    void testOpenUrlsMaxLimit() {
        List<String> invalidUrls = List.of("   ", "", "invalid");
        List<String> openedZero = BrowserUtils.openUrls(invalidUrls, 0);
        assertNotNull(openedZero);
        assertTrue(openedZero.isEmpty());

        List<String> opened = BrowserUtils.openUrls(List.of("   ", "   "), 2);
        assertTrue(opened.isEmpty());
    }
}
