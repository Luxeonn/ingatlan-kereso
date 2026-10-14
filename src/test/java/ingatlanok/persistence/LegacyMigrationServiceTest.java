package ingatlanok.persistence;

import com.google.gson.Gson;
import ingatlanok.model.IngatlanItem;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class LegacyMigrationServiceTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("Monolitikus adatbázis sikeres migrálása városonkénti fájlokra")
    void testMigrateOldDatabaseToPerCityStructure() throws IOException {
        Path dbFile = tempDir.resolve("old_db.json");
        Path citiesDir = tempDir.resolve("cities");

        Gson gson = new Gson();
        IngatlanItem item1 = new IngatlanItem("1", "Duna House", "https://dh.hu/1", "Lakás", "50 M Ft", "Debrecen", "Debrecen", "50 m²", "2026-10-14 12:00:00", false);
        IngatlanItem item2 = new IngatlanItem("2", "Otthon Centrum", "https://oc.hu/2", "Ház", "70 M Ft", "Szeged", "Szeged", "80 m²", "2026-10-14 12:00:00", false);

        Files.writeString(dbFile, gson.toJson(List.of(item1, item2)));

        LegacyMigrationService service = new LegacyMigrationService(gson);
        service.migrateOldDatabaseToPerCityStructure(dbFile, citiesDir);

        assertTrue(Files.exists(citiesDir.resolve("debrecen.json")));
        assertTrue(Files.exists(citiesDir.resolve("szeged.json")));
    }

    @Test
    @DisplayName("Legacy txt importálás és portáldetektálás")
    void testImportLegacyFileAndDetectPortal() throws IOException {
        Path legacyTxt = tempDir.resolve("legacy.txt");
        List<String> lines = List.of(
                "https://dh.hu/elado-lakas/1",
                "https://oc.hu/ingatlan/2",
                "https://www.jofogas.hu/3",
                "https://ingatlan.com/4",
                "https://www.ingatlanbazar.hu/5",
                "https://koltozzbe.hu/6",
                "https://other.hu/7",
                "invalid line"
        );
        Files.write(legacyTxt, lines);

        Map<String, Map<String, IngatlanItem>> cityItemMaps = new HashMap<>();
        Map<String, Set<String>> cityKnownUrls = new HashMap<>();
        Map<String, IngatlanItem> itemMap = new HashMap<>();
        Set<String> knownUrls = new HashSet<>();

        LegacyMigrationService service = new LegacyMigrationService(new Gson());
        service.importLegacyFile(legacyTxt, cityItemMaps, cityKnownUrls, itemMap, knownUrls);

        assertEquals(7, knownUrls.size());
        assertEquals(7, itemMap.size());
        assertEquals("Duna House", service.detectPortal("https://dh.hu/1"));
        assertEquals("Otthon Centrum", service.detectPortal("https://oc.hu/1"));
        assertEquals("Jófogás Ingatlan", service.detectPortal("https://jofogas.hu/1"));
        assertEquals("Ingatlan.com", service.detectPortal("https://ingatlan.com/1"));
        assertEquals("Ingatlanbazár", service.detectPortal("https://ingatlanbazar.hu/1"));
        assertEquals("Költözzbe", service.detectPortal("https://koltozzbe.hu/1"));
        assertEquals("Egyéb", service.detectPortal("https://ismeretlen.hu/1"));
        assertEquals("Egyéb", service.detectPortal(null));
    }
}
