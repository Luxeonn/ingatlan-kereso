package ingatlanok;

import ingatlanok.common.PropertyParser;
import ingatlanok.model.PriceInfo;
import ingatlanok.model.PropertyDetails;
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
    void testParsePrice() {
        PriceInfo p1 = PropertyParser.parsePrice("Eladó lakás 54 M Ft");
        assertNotNull(p1);
        assertEquals(54.0, p1.getAmountMillion());
        assertEquals(54_000_000L, p1.getAmountHuf());

        PriceInfo p2 = PropertyParser.parsePrice("Ikerház 45,5 M Ft");
        assertNotNull(p2);
        assertEquals(45.5, p2.getAmountMillion());
        assertEquals(45_500_000L, p2.getAmountHuf());

        PriceInfo p3 = PropertyParser.parsePrice("Tégla lakás 35 000 000 Ft");
        assertNotNull(p3);
        assertEquals(35.0, p3.getAmountMillion());
        assertEquals(35_000_000L, p3.getAmountHuf());

        PriceInfo p4 = PropertyParser.parsePrice("Nincs ár");
        assertNotNull(p4);
        assertNull(p4.getAmountHuf());
        assertEquals("Ár a hirdetésben", p4.getRawText());
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
    void testParseDetails() {
        PriceInfo price = PropertyParser.parsePrice("50 M Ft");
        PropertyDetails d1 = PropertyParser.parseDetails("2 szoba • 50 m²", price);
        assertNotNull(d1);
        assertEquals(50.0, d1.getAreaSquareMeters());
        assertEquals(2.0, d1.getRoomCount());
        assertEquals(1_000_000L, d1.getPricePerSquareMeter(), "50M Ft / 50 m² = 1.000.000 Ft/m²");

        PropertyDetails d2 = PropertyParser.parseDetails("75 m² 3 szoba", price);
        assertNotNull(d2);
        assertEquals(75.0, d2.getAreaSquareMeters());
        assertEquals(3.0, d2.getRoomCount());
        assertEquals(666_667L, d2.getPricePerSquareMeter());
    }

    @Test
    void testExtractFirstGroup() {
        Pattern pattern = Pattern.compile("/ingatlan/([A-Za-z0-9]+)");
        assertEquals("abc123", PropertyParser.extractFirstGroup("https://dh.hu/ingatlan/abc123/reszletek", pattern, "fallback"));
        assertEquals("fallback", PropertyParser.extractFirstGroup("https://dh.hu/nincs-id", pattern, "fallback"));
        assertEquals("fallback", PropertyParser.extractFirstGroup(null, pattern, "fallback"));
    }
}
