package ingatlanok.web;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("removal")
class WebServerHostConfigTest {

    @Test
    @DisplayName("WebServer alapértelmezett host és port feloldása")
    void testDefaultHostAndPortResolution() {
        assertEquals("127.0.0.1", WebServer.resolveDefaultHost());
        assertEquals(8080, WebServer.resolveDefaultPort());
    }

    @Test
    @DisplayName("WebServer egyedi host és port beállítása")
    void testCustomHostAndPort() {
        WebServer server = new WebServer("0.0.0.0", 8995);
        assertEquals("0.0.0.0", server.getHost());
        assertEquals(8995, server.getPort());
    }

    @Test
    @DisplayName("WebServer indulása és leállása egyedi porton")
    void testStartAndStop() throws IOException {
        WebServer server = new WebServer("127.0.0.1", 8996);
        assertDoesNotThrow(server::start);
        assertTrue(server.getPort() >= 8996);
        assertDoesNotThrow(server::stop);
    }
}
