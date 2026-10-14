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

/**
 * Automatikus JSON -> SQLite adatmigrációs szolgáltatás (Zero Data Loss elv).
 * Ha a relációs adatbázis még üres, a korábbi városszintű és konfigurációs JSON fájlokból
 * tranzakcióban importálja az összes korábbi találatot és beállítást.
 */
public class SqliteMigrationService {

    private static final AppLogger LOGGER = AppLogger.getLogger(SqliteMigrationService.class);
    private final SqliteDatabaseManager dbManager;
    private final Gson gson;

    public SqliteMigrationService(SqliteDatabaseManager dbManager) {
        this(dbManager, new Gson());
    }

    public SqliteMigrationService(SqliteDatabaseManager dbManager, Gson gson) {
        this.dbManager = dbManager;
        this.gson = gson != null ? gson : new Gson();
    }

    /**
     * Automatikus migráció végrehajtása az alapértelmezett útvonalakról, amennyiben az SQLite tábla üres.
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
        SqliteListingStorage listingStorage = new SqliteListingStorage(dbManager);
        int totalInDb = listingStorage.getTotalCount();

        boolean migratedAny = false;

        // 1. Hirdetések migrációja, ha még üres az SQLite
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
                LOGGER.info("[Migráció] {} db korábbi hirdetés importálása SQLite adatbázisba...", collected.size());
                listingStorage.saveAll(collected.values());
                LOGGER.info("[Migráció] Sikeres hirdetés-migráció: {} db rekord rögzítve az SQLite-ban.", collected.size());
                migratedAny = true;
            }
        }

        // 2. Keresési konfiguráció migrációja, ha még nincs mentve az SQLite-ban
        SqliteConfigStorage configStorage = new SqliteConfigStorage(dbManager);
        if (configJsonFile != null && Files.exists(configJsonFile)) {
            boolean hasConfig = false;
            try (java.sql.Connection conn = dbManager.getConnection();
                 java.sql.Statement stmt = conn.createStatement();
                 java.sql.ResultSet rs = stmt.executeQuery("SELECT 1 FROM search_config WHERE config_key = 'last_criteria' LIMIT 1;")) {
                hasConfig = rs.next();
            } catch (Exception ignored) {
            }

            if (!hasConfig) {
                try (Reader reader = Files.newBufferedReader(configJsonFile, StandardCharsets.UTF_8)) {
                    SearchCriteria fileCriteria = gson.fromJson(reader, SearchCriteria.class);
                    if (fileCriteria != null) {
                        configStorage.saveLastCriteria(fileCriteria);
                        LOGGER.info("[Migráció] Keresési konfiguráció sikeresen átmásolva SQLite-ba: {}", configJsonFile.getFileName());
                        migratedAny = true;
                    }
                } catch (Exception e) {
                    LOGGER.warn("Nem sikerült átmásolni a keresési konfigurációt [{}]: {}", configJsonFile.getFileName(), e.getMessage());
                }
            }
        }

        return migratedAny;
    }
}
