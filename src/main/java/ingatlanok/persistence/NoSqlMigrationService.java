package ingatlanok.persistence;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import ingatlanok.common.AppLogger;
import ingatlanok.model.IngatlanItem;
import ingatlanok.model.SearchCriteria;

import java.io.File;
import java.io.Reader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static org.dizitart.no2.filters.FluentFilter.where;

/**
 * Automatikus JSON / Legacy -> NoSQL Dokumentum adatbázis adatmigrációs szolgáltatás (Zero Data Loss elv).
 * Ha a NoSQL adatbázis még üres, a korábbi városszintű és konfigurációs JSON fájlokból
 * importálja az összes korábbi találatot és beállítást a Nitrite dokumentumtárba.
 */
public class NoSqlMigrationService {

    private static final AppLogger LOGGER = AppLogger.getLogger(NoSqlMigrationService.class);
    private final NitriteDatabaseManager dbManager;
    private final Gson gson;

    public NoSqlMigrationService(NitriteDatabaseManager dbManager) {
        this(dbManager, new Gson());
    }

    public NoSqlMigrationService(NitriteDatabaseManager dbManager, Gson gson) {
        this.dbManager = dbManager;
        this.gson = gson != null ? gson : new Gson();
    }

    /**
     * Automatikus migráció végrehajtása az alapértelmezett útvonalakról, amennyiben a NoSQL adatbázis üres.
     */
    public boolean migrateIfEmpty() {
        return migrateIfEmpty(
                Paths.get("data", "cities"),
                Paths.get("data", "ingatlanok_db.json"),
                Paths.get("data", "last_config.json")
        );
    }

    /**
     * Paraméterezhető migráció teszteléshez és egyedi könyvtárakhoz.
     */
    public boolean migrateIfEmpty(Path citiesDir, Path globalDbFile, Path configJsonFile) {
        NitriteListingStorage listingStorage = new NitriteListingStorage(dbManager);
        int totalInDb = listingStorage.getTotalCount();

        boolean migratedAny = false;

        // 1. Hirdetések migrációja, ha még üres a NoSQL dokumentumtár
        if (totalInDb == 0) {
            Map<String, IngatlanItem> collected = new LinkedHashMap<>();

            // Városszintű fájlok beolvasása
            if (citiesDir != null && Files.exists(citiesDir) && Files.isDirectory(citiesDir)) {
                File[] cityFiles = citiesDir.toFile().listFiles((dir, name) -> name.endsWith(".json"));
                if (cityFiles != null) {
                    Type listType = new TypeToken<List<IngatlanItem>>() {}.getType();
                    for (File cityFile : cityFiles) {
                        try (Reader reader = Files.newBufferedReader(cityFile.toPath(), StandardCharsets.UTF_8)) {
                            List<IngatlanItem> items = gson.fromJson(reader, listType);
                            if (items != null) {
                                for (IngatlanItem item : items) {
                                    if (item != null && item.getUrl() != null && !item.getUrl().isBlank()) {
                                        collected.putIfAbsent(item.getUrl(), item);
                                    }
                                }
                            }
                        } catch (Exception e) {
                            LOGGER.warn("Nem sikerült beolvasni a városfájlt migrációhoz [{}]: {}", cityFile.getName(), e.getMessage());
                        }
                    }
                }
            }

            // Globális JSON beolvasása fallbackként, ha még nem került be
            if (globalDbFile != null && Files.exists(globalDbFile)) {
                Type listType = new TypeToken<List<IngatlanItem>>() {}.getType();
                try (Reader reader = Files.newBufferedReader(globalDbFile, StandardCharsets.UTF_8)) {
                    List<IngatlanItem> items = gson.fromJson(reader, listType);
                    if (items != null) {
                        for (IngatlanItem item : items) {
                            if (item != null && item.getUrl() != null && !item.getUrl().isBlank()) {
                                collected.putIfAbsent(item.getUrl(), item);
                            }
                        }
                    }
                } catch (Exception e) {
                    LOGGER.warn("Nem sikerült beolvasni a globális adatbázisfájlt migrációhoz [{}]: {}", globalDbFile.getFileName(), e.getMessage());
                }
            }

            if (!collected.isEmpty()) {
                LOGGER.info("[NoSQL Migráció] {} db korábbi hirdetés betöltése folyamatban a NoSQL dokumentumtárba...", collected.size());
                listingStorage.saveAll(collected.values());
                LOGGER.info("[NoSQL Migráció] Sikeresen átmigrálva {} db hirdetés.", collected.size());
                migratedAny = true;
            }
        }

        // 2. Keresési konfiguráció migrációja, ha még nem létezik a NoSQL tárolóban
        boolean hasConfigInDb = dbManager.getConfigCollection().find(where("key").eq("last_criteria")).firstOrNull() != null;
        if (!hasConfigInDb && configJsonFile != null && Files.exists(configJsonFile)) {
            try (Reader reader = Files.newBufferedReader(configJsonFile, StandardCharsets.UTF_8)) {
                SearchCriteria migratedCriteria = gson.fromJson(reader, SearchCriteria.class);
                if (migratedCriteria != null) {
                    NitriteConfigStorage configStorage = new NitriteConfigStorage(dbManager);
                    configStorage.saveLastCriteria(migratedCriteria);
                    LOGGER.info("[NoSQL Migráció] Korábbi keresési konfiguráció sikeresen átmentve: {}", configJsonFile.getFileName());
                    migratedAny = true;
                }
            } catch (Exception e) {
                LOGGER.warn("Nem sikerült a korábbi konfiguráció migrációja [{}]: {}", configJsonFile.getFileName(), e.getMessage());
            }
        }

        return migratedAny;
    }
}
