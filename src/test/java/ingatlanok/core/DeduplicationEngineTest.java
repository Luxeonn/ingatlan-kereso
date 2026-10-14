package ingatlanok.core;

import ingatlanok.model.IngatlanItem;
import ingatlanok.persistence.ListingStorage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class DeduplicationEngineTest {

    @Test
    @DisplayName("Deduplikáció URL alapján a sorrend megőrzésével")
    void testDeduplicateByUrl() {
        DeduplicationEngine engine = new DeduplicationEngine(null);

        IngatlanItem item1 = new IngatlanItem("1", "dh", "http://1", "Első", "50 M Ft", "Debrecen", "", "2026-10-14", false);
        IngatlanItem item2 = new IngatlanItem("2", "dh", "http://2", "Második", "60 M Ft", "Debrecen", "", "2026-10-14", false);
        IngatlanItem item3 = new IngatlanItem("3", "dh", "http://1", "Első duplikátum", "50 M Ft", "Debrecen", "", "2026-10-14", false);

        List<IngatlanItem> deduped = engine.deduplicateByUrl(List.of(item1, item2, item3));
        assertEquals(2, deduped.size());
        assertEquals("http://1", deduped.get(0).getUrl());
        assertEquals("Első", deduped.get(0).getTitle());
        assertEquals("http://2", deduped.get(1).getUrl());
    }

    @Test
    @DisplayName("Baseline snapshot esetén minden találat isNew = false értéket kap")
    void testApplyNewStatusBaseline() {
        DeduplicationEngine engine = new DeduplicationEngine(null);

        IngatlanItem item = new IngatlanItem("1", "dh", "http://1", "Új városi", "50 M Ft", "Eger", "", "2026-10-14", true);
        engine.applyNewStatus("Eger", List.of(item), true);

        assertFalse(item.isNew());
    }

    @Test
    @DisplayName("Ismert város esetén a tároló alapján kap új státuszt")
    void testApplyNewStatusKnownCity() {
        ListingStorage mockStorage = new ListingStorage() {
            @Override public void saveCityItems(String city, Collection<IngatlanItem> items) {}
            @Override public void saveAll(Collection<IngatlanItem> items) {}
            @Override public List<IngatlanItem> getItemsByCity(String city) { return List.of(); }
            @Override public List<IngatlanItem> getAll() { return List.of(); }
            @Override public boolean hasCityDatabase(String city) { return true; }
            @Override public boolean isNewUrl(String city, String url) { return "http://new".equals(url); }
            @Override public boolean containsUrl(String url) { return false; }
            @Override public boolean markAsViewed(String url) { return false; }
            @Override public boolean isViewed(String url) { return false; }
            @Override public Set<String> getAllUrls() { return Set.of(); }
            @Override public List<Map<String, Object>> getTrackedCitiesInfo() { return List.of(); }
            @Override public void clearCityHistory(String city) {}
            @Override public void clearHistory() {}
            @Override public int getTotalCount() { return 0; }
            @Override public String getLastRunTime() { return null; }
        };

        DeduplicationEngine engine = new DeduplicationEngine(mockStorage);
        IngatlanItem item1 = new IngatlanItem("1", "dh", "http://new", "Új tétel", "50 M Ft", "Debrecen", "", "2026-10-14", false);
        IngatlanItem item2 = new IngatlanItem("2", "dh", "http://old", "Régi tétel", "50 M Ft", "Debrecen", "", "2026-10-14", false);

        engine.applyNewStatus("Debrecen", List.of(item1, item2), false);

        assertTrue(item1.isNew());
        assertFalse(item2.isNew());
    }
}
