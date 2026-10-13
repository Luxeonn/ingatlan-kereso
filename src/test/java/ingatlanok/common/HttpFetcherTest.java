package ingatlanok.common;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HttpFetcherTest {

    @Test
    @DisplayName("Constants and headers validation")
    void testConstants() {
        assertNotNull(HttpFetcher.DEFAULT_USER_AGENT);
        assertTrue(HttpFetcher.DEFAULT_USER_AGENT.contains("Chrome"));
        assertTrue(HttpFetcher.DEFAULT_TIMEOUT_MS > 0);
    }

    @Test
    @DisplayName("fetchDocument returns null on invalid or blank URLs")
    void testFetchDocumentGracefulHandling() {
        assertNull(HttpFetcher.fetchDocument(null));
        assertNull(HttpFetcher.fetchDocument(""));
        assertNull(HttpFetcher.fetchDocument("   "));
        assertNull(HttpFetcher.fetchDocument("https://invalid-non-existing-host-987654321.test", 1000));
    }

    @Test
    @DisplayName("fetchJson returns null on invalid or blank URLs")
    void testFetchJsonGracefulHandling() {
        assertNull(HttpFetcher.fetchJson(null));
        assertNull(HttpFetcher.fetchJson(""));
        assertNull(HttpFetcher.fetchJson("   "));
        assertNull(HttpFetcher.fetchJson("https://invalid-non-existing-host-987654321.test", 1000));
    }
}
