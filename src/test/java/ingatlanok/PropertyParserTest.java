package ingatlanok;

import ingatlanok.common.PropertyParser;
import org.junit.jupiter.api.Test;

import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

class PropertyParserTest {

    @Test
    void testExtractPrice() {
        assertEquals("54 M Ft", PropertyParser.extractPrice("Eladó lakás Debrecen belváros 54 M Ft sürgősen"));
        assertEquals("45,5 M Ft", PropertyParser.extractPrice("Ikerház 45,5 M Ft jó állapotú"));
        assertEquals("45.5 M Ft", PropertyParser.extractPrice("Családi ház 45.5 M Ft"));
        assertEquals("35 000 000 Ft", PropertyParser.extractPrice("Tégla lakás 35 000 000 Ft irányár"));
        assertEquals("Ár a hirdetésben", PropertyParser.extractPrice(null));
        assertEquals("Ár a hirdetésben", PropertyParser.extractPrice("Nincs ár megadva"));
    }

    @Test
    void testExtractDetails() {
        assertEquals("2 szoba • 48 m²", PropertyParser.extractDetails("Debrecen 2 szoba • 48 m² tégla"));
        assertEquals("55 m²", PropertyParser.extractDetails("Kiadó lakás 55 m² erkélyes"));
        assertEquals("65 m² 3 szoba", PropertyParser.extractDetails("Felújított ház 65 m² 3 szoba"));
        assertEquals("", PropertyParser.extractDetails(null));
        assertEquals("", PropertyParser.extractDetails("Semmi méretadat"));
    }

    @Test
    void testExtractFirstGroup() {
        Pattern pattern = Pattern.compile("/ingatlan/([A-Za-z0-9]+)");
        assertEquals("abc123", PropertyParser.extractFirstGroup("https://dh.hu/ingatlan/abc123/reszletek", pattern, "fallback"));
        assertEquals("fallback", PropertyParser.extractFirstGroup("https://dh.hu/nincs-id", pattern, "fallback"));
        assertEquals("fallback", PropertyParser.extractFirstGroup(null, pattern, "fallback"));
    }
}
