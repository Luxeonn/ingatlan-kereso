package ingatlanok.persistence.mapper;

import ingatlanok.model.*;
import org.dizitart.no2.collection.Document;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("NitriteDocumentMapper Unit Tests")
class NitriteDocumentMapperTest {

    @Test
    @DisplayName("toItem converts Nitrite Document to IngatlanItem correctly")
    void testToItem() {
        assertNull(NitriteDocumentMapper.toItem(null));

        Document doc = Document.createDocument("url", "https://dh.hu/10")
                .put("id", "dh_10")
                .put("portal", "Duna House")
                .put("title", "Kecskeméti ház")
                .put("price", "60 M Ft")
                .put("priceAmount", 60_000_000L)
                .put("priceMillions", 60.0)
                .put("pricePerSqm", 600_000L)
                .put("city", "Kecskemét")
                .put("location", "Kecskemét")
                .put("details", "100 m2, 3 szoba")
                .put("areaSqm", 100.0)
                .put("rooms", 3.0)
                .put("discoveredAt", "2026-03-01 10:00:00")
                .put("isNew", true)
                .put("viewed", true)
                .put("viewedAt", "2026-03-01 11:00:00")
                .put("imageUrl", "https://dh.hu/img.png")
                .put("status", "ACTIVE")
                .put("priceDelta", 0L)
                .put("priceChangePercent", 0.0);

        IngatlanItem item = NitriteDocumentMapper.toItem(doc);
        assertNotNull(item);
        assertEquals("dh_10", item.getId());
        assertEquals("https://dh.hu/10", item.getUrl());
        assertEquals("Kecskemét", item.getCity());
        assertTrue(item.isNew());
        assertTrue(item.isViewed());
        assertEquals("2026-03-01 11:00:00", item.getViewedAt());
        assertNotNull(item.getPriceInfo());
        assertEquals(60_000_000L, item.getPriceInfo().getAmountHuf());
        assertNotNull(item.getDetailsInfo());
        assertEquals(100.0, item.getDetailsInfo().getAreaSquareMeters());
    }

    @Test
    @DisplayName("toPropertyDocument creates Document with all attributes")
    void testToPropertyDocument() {
        IngatlanItem item = new IngatlanItem();
        item.setUrl("https://oc.hu/20");
        item.setImageUrl("https://oc.hu/pic.jpg");
        item.setNew(true);

        Document doc = NitriteDocumentMapper.toPropertyDocument(
                item, "oc_20", "Otthon Centrum", "Pécs", "pecs", "Pécsi lakás",
                "35 M Ft", 35_000_000L, 35.0, 700_000L, "50 m2, 2 szoba", 50.0, 2.0,
                "2026-03-01 08:00:00", false, null, "ACTIVE", 0L, 0.0, null, "2026-03-01 12:00:00"
        );

        assertNotNull(doc);
        assertEquals("oc_20", doc.get("id"));
        assertEquals("https://oc.hu/20", doc.get("url"));
        assertEquals("Pécs", doc.get("city"));
        assertEquals("pecs", doc.get("normalizedCity"));
        assertEquals(35_000_000L, doc.get("priceAmount"));
        assertEquals(50.0, doc.get("areaSqm"));
        assertFalse(doc.get("viewed", Boolean.class));
    }

    @Test
    @DisplayName("PriceHistory Document roundtrip")
    void testPriceHistoryDocument() {
        Document doc = NitriteDocumentMapper.toPriceHistoryDocument(
                5, "prop_1", "https://site.hu/1", "2026-03-01 10:00:00", "40 M Ft",
                40_000_000L, 40.0, 800_000L, 42_000_000L, -2_000_000L, -4.76
        );

        PriceHistoryEntry entry = NitriteDocumentMapper.toPriceHistoryEntry(doc, "fallback");
        assertEquals(5, entry.id());
        assertEquals("prop_1", entry.propertyId());
        assertEquals(-2_000_000L, entry.priceDelta());
        assertEquals(-4.76, entry.changePercent());
        assertEquals(42_000_000L, entry.previousPriceAmount());
    }

    @Test
    @DisplayName("StatusLog Document roundtrip")
    void testStatusLogDocument() {
        Document doc = NitriteDocumentMapper.toStatusLogDocument(
                8, "prop_1", "https://site.hu/1", "PRICE_DROP", "2026-03-01 12:00:00", "Árcsökkenés"
        );

        StatusLogEntry entry = NitriteDocumentMapper.toStatusLogEntry(doc, "fallback");
        assertEquals(8, entry.id());
        assertEquals("prop_1", entry.propertyId());
        assertEquals("PRICE_DROP", entry.status());
        assertEquals("Árcsökkenés", entry.note());
    }
}
