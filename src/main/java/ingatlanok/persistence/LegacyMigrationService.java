package ingatlanok.persistence;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import ingatlanok.common.AppLogger;
import ingatlanok.common.CityUtils;
import ingatlanok.model.IngatlanItem;
import org.apache.commons.io.FileUtils;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Korábbi szöveges naplók és monolitikus adatbázisformátumok migrációjáért felelős szolgáltatás.
 */
public class LegacyMigrationService {

    private static final AppLogger LOGGER = AppLogger.getLogger(LegacyMigrationService.class);
    private final Gson gson;

    public LegacyMigrationService(Gson gson) {
        this.gson = gson != null ? gson : new Gson();
    }

    /**
     * Ha a városszintű könyvtárban még nincsenek fájlok, de létezik régi összesített JSON,
     * szétbontja azt városonkénti fájlokra.
     */
    public void migrateOldDatabaseToPerCityStructure(Path dbFilePath, Path citiesDirPath) {
        if (!Files.exists(dbFilePath)) {
            return;
        }
        File citiesDir = citiesDirPath.toFile();
        File[] existingCityFiles = citiesDir.listFiles((dir, name) -> name.endsWith(".json"));
        if (existingCityFiles != null && existingCityFiles.length > 0) {
            return;
        }

        LOGGER.info("[Adatbázis] Meglévő adatbázis migrálása városonkénti struktúrára...");
        try (Reader reader = Files.newBufferedReader(dbFilePath, StandardCharsets.UTF_8)) {
            Type listType = new TypeToken<List<IngatlanItem>>() {}.getType();
            List<IngatlanItem> items = gson.fromJson(reader, listType);
            if (items != null && !items.isEmpty()) {
                Map<String, List<IngatlanItem>> grouped = new HashMap<>();
                for (IngatlanItem item : items) {
                    String city = item.getCity();
                    if (city == null || city.isBlank()) {
                        city = item.getLocation();
                    }
                    if (city == null || city.isBlank()) {
                        city = "Debrecen";
                    }
                    String key = CityUtils.normalizeCityKey(city);
                    grouped.computeIfAbsent(key, k -> new ArrayList<>()).add(item);
                }

                if (!Files.exists(citiesDirPath)) {
                    Files.createDirectories(citiesDirPath);
                }

                for (Map.Entry<String, List<IngatlanItem>> entry : grouped.entrySet()) {
                    File cityFile = new File(citiesDir, entry.getKey() + ".json");
                    String json = gson.toJson(entry.getValue());
                    FileUtils.writeStringToFile(cityFile, json, StandardCharsets.UTF_8);
                }
                LOGGER.info("[Adatbázis] Sikeres migráció: {} város JSON létrehozva.", grouped.size());
            }
        } catch (Exception e) {
            LOGGER.error("Nem sikerült a régi adatbázis migrálása: {}", e.getMessage(), e);
        }
    }

    /**
     * Korábbi .txt formátumú előzménynaplók beolvasása és Debrecen memóriatárába illesztése.
     */
    public void importLegacyFile(Path legacyFilePath,
                                 Map<String, Map<String, IngatlanItem>> cityItemMaps,
                                 Map<String, Set<String>> cityKnownUrls,
                                 Map<String, IngatlanItem> itemMap,
                                 Set<String> knownUrls) {
        if (!Files.exists(legacyFilePath)) {
            return;
        }

        try (BufferedReader br = Files.newBufferedReader(legacyFilePath, StandardCharsets.UTF_8)) {
            String line;
            String timestamp = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(LocalDateTime.now());
            String debrecenKey = CityUtils.normalizeCityKey("Debrecen");
            Map<String, IngatlanItem> debMap = cityItemMaps.computeIfAbsent(debrecenKey, k -> new ConcurrentHashMap<>());
            Set<String> debUrls = cityKnownUrls.computeIfAbsent(debrecenKey, k -> ConcurrentHashMap.newKeySet());

            int importedCount = 0;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if ((line.startsWith("http://") || line.startsWith("https://")) && !knownUrls.contains(line)) {
                    String portal = detectPortal(line);
                    IngatlanItem item = new IngatlanItem(
                            UUID.randomUUID().toString().substring(0, 8),
                            portal,
                            line,
                            portal + " hirdetés (archív)",
                            "N/A",
                            "Debrecen",
                            "Debrecen",
                            "Archív előzmény",
                            timestamp,
                            false
                    );
                    debMap.put(line, item);
                    debUrls.add(line);
                    itemMap.put(line, item);
                    knownUrls.add(line);
                    importedCount++;
                }
            }
            if (importedCount > 0) {
                LOGGER.info("Legacy fájl import: {} új URL importálva [{}]", importedCount, legacyFilePath.getFileName());
            }
        } catch (IOException e) {
            LOGGER.error("Legacy fájl import hiba [{}]: {}", legacyFilePath.getFileName(), e.getMessage());
        }
    }

    public String detectPortal(String url) {
        if (url == null) return "Egyéb";
        if (url.contains("dh.hu")) return "Duna House";
        if (url.contains("oc.hu")) return "Otthon Centrum";
        if (url.contains("jofogas.hu")) return "Jófogás Ingatlan";
        if (url.contains("ingatlan.com")) return "Ingatlan.com";
        if (url.contains("ingatlanbazar.hu")) return "Ingatlanbazár";
        if (url.contains("koltozzbe.hu")) return "Költözzbe";
        return "Egyéb";
    }
}
