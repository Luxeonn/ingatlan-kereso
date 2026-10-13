package ingatlanok;

import ingatlanok.model.IngatlanItem;
import ingatlanok.model.SearchCriteria;
import ingatlanok.persistence.IngatlanRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MultiCityRepositoryTest {

    private final String TEST_CITY = "TesztVaros";
    private final IngatlanRepository repo = IngatlanRepository.getInstance();

    @BeforeEach
    @AfterEach
    void cleanUp() {
        repo.clearCityHistory(TEST_CITY);
    }

    @Test
    void testPerCityDatabaseBaselineSnapshotAndDifference() {

        assertFalse(repo.hasCityDatabase(TEST_CITY), "Még nem szabad léteznie a teszt város adatbázisának");

        String url1 = "https://teszt.hu/ingatlan/1";
        String url2 = "https://teszt.hu/ingatlan/2";
        assertFalse(repo.isNewUrl(TEST_CITY, url1), "Első kereséskor az elemek baseline snapshotként kerülnek mentésre, nem újak");

        IngatlanItem item1 = new IngatlanItem("1", "Duna House", url1, "Teszt 1", "50 M Ft", TEST_CITY, "", LocalDateTime.now().toString(), false);
        IngatlanItem item2 = new IngatlanItem("2", "Duna House", url2, "Teszt 2", "60 M Ft", TEST_CITY, "", LocalDateTime.now().toString(), false);
        repo.saveCityItems(TEST_CITY, List.of(item1, item2));

        assertTrue(repo.hasCityDatabase(TEST_CITY), "A mentés után a város adatbázisának léteznie kell");

        assertFalse(repo.isNewUrl(TEST_CITY, url1), "A már elmentett elem nem új");
        assertFalse(repo.isNewUrl(TEST_CITY, url2), "A már elmentett elem nem új");

        String url3 = "https://teszt.hu/ingatlan/3";
        assertTrue(repo.isNewUrl(TEST_CITY, url3), "A még nem mentett elem újnak kell számítson");

        List<Map<String, Object>> tracked = repo.getTrackedCitiesInfo();
        boolean foundTracked = tracked.stream().anyMatch(t -> TEST_CITY.equalsIgnoreCase((String) t.get("name")) || "tesztvaros".equals(t.get("key")));
        assertTrue(foundTracked, "A tracked városok között szerepelnie kell");

        repo.clearCityHistory(TEST_CITY);
        assertFalse(repo.hasCityDatabase(TEST_CITY), "Törlés után a város adatbázisának törlődnie kell");
    }

    @Test
    void testSearchCriteriaCitiesAndClone() {
        SearchCriteria criteria = new SearchCriteria();
        assertEquals(1, criteria.getCities().size());
        assertEquals("Debrecen", criteria.getCities().get(0));

        criteria.setCities(List.of("Debrecen", "Hajdúszoboszló"));
        assertEquals(2, criteria.getCities().size());
        assertEquals("Debrecen", criteria.getCity());

        SearchCriteria cloned = criteria.cloneForCity("Hajdúszoboszló");
        assertEquals("Hajdúszoboszló", cloned.getCity());
        assertEquals(List.of("Hajdúszoboszló"), cloned.getCities());
        assertEquals(criteria.getPropertyType(), cloned.getPropertyType());
    }
}
