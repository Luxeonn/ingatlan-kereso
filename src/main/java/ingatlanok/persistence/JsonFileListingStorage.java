package ingatlanok.persistence;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import ingatlanok.common.AppLogger;
import ingatlanok.common.CityUtils;
import ingatlanok.model.IngatlanItem;
import org.apache.commons.io.FileUtils;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Városonként szegmentált, kétszintű atomi JSON hirdetésterjesztő és adatbázis implementáció.
 * Zero Data Loss elv, ReentrantReadWriteLock zárolás és szigorú Path Traversal védelem.
 */
public class JsonFileListingStorage implements ListingStorage {

    private static final AppLogger LOGGER = AppLogger.getLogger(JsonFileListingStorage.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final Path citiesDir;
    private final Path globalDbFile;
    private final Path legacyHistoryFile;
    private final Path oldHistoryFile;

    private final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();

    private final Map<String, IngatlanItem> itemMap = new ConcurrentHashMap<>();
    private final Set<String> knownUrls = ConcurrentHashMap.newKeySet();

    private final Map<String, Map<String, IngatlanItem>> cityItemMaps = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> cityKnownUrls = new ConcurrentHashMap<>();

    private final LegacyMigrationService migrationService;
    private String lastRunTime = null;

    public JsonFileListingStorage() {
        this(Paths.get("data", "cities"),
             Paths.get("data", "ingatlanok_db.json"),
             Paths.get("ingatlanok_history.txt"),
             Paths.get("2017_06_06.txt"));
    }

    public JsonFileListingStorage(Path citiesDir, Path globalDbFile, Path legacyHistoryFile, Path oldHistoryFile) {
        this.citiesDir = citiesDir.toAbsolutePath().normalize();
        this.globalDbFile = globalDbFile.toAbsolutePath().normalize();
        this.legacyHistoryFile = legacyHistoryFile != null ? legacyHistoryFile.toAbsolutePath().normalize() : null;
        this.oldHistoryFile = oldHistoryFile != null ? oldHistoryFile.toAbsolutePath().normalize() : null;
        this.migrationService = new LegacyMigrationService(GSON);

        init();
    }

    private void init() {
        rwLock.writeLock().lock();
        try {
            if (!Files.exists(citiesDir)) {
                Files.createDirectories(citiesDir);
            }
            Path dataDir = globalDbFile.getParent();
            if (dataDir != null && !Files.exists(dataDir)) {
                Files.createDirectories(dataDir);
            }

            migrationService.migrateOldDatabaseToPerCityStructure(globalDbFile, citiesDir);
            loadFromCitiesDirectory();

            if (itemMap.isEmpty() && Files.exists(globalDbFile)) {
                loadFromAggregateJson();
            }

            if (oldHistoryFile != null) {
                migrationService.importLegacyFile(oldHistoryFile, cityItemMaps, cityKnownUrls, itemMap, knownUrls);
            }
            if (legacyHistoryFile != null) {
                migrationService.importLegacyFile(legacyHistoryFile, cityItemMaps, cityKnownUrls, itemMap, knownUrls);
            }

            LOGGER.info("[Adatbázis] Betöltve: {} korábbi hirdetés {} városból.", itemMap.size(), cityItemMaps.size());
        } catch (IOException e) {
            LOGGER.error("Hiba a JsonFileListingStorage inicializálásakor: {}", e.getMessage(), e);
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    /**
     * Szigorú Path Traversal ellenőrzés a településkulcsra.
     */
    public Path resolveAndValidateCityFile(String cityKey) {
        if (cityKey == null || cityKey.isBlank() || !cityKey.matches("^[a-z0-9_-]+$")) {
            throw new IllegalArgumentException("Érvénytelen település azonosító (Path traversal védelem): " + cityKey);
        }
        Path targetPath = citiesDir.resolve(cityKey + ".json").normalize();
        if (!targetPath.startsWith(citiesDir)) {
            throw new SecurityException("Érvénytelen elérési út kísérlet (Path traversal védelem): " + cityKey);
        }
        return targetPath;
    }

    private void loadFromCitiesDirectory() {
        File[] files = citiesDir.toFile().listFiles((dir, name) -> name.endsWith(".json"));
        if (files == null) return;

        Type listType = new TypeToken<List<IngatlanItem>>() {}.getType();
        for (File cityFile : files) {
            String cityKey = cityFile.getName().replace(".json", "");
            try (Reader reader = Files.newBufferedReader(cityFile.toPath(), StandardCharsets.UTF_8)) {
                List<IngatlanItem> items = GSON.fromJson(reader, listType);
                if (items != null) {
                    Map<String, IngatlanItem> map = cityItemMaps.computeIfAbsent(cityKey, k -> new ConcurrentHashMap<>());
                    Set<String> urls = cityKnownUrls.computeIfAbsent(cityKey, k -> ConcurrentHashMap.newKeySet());

                    for (IngatlanItem item : items) {
                        if (item.getUrl() != null) {
                            map.put(item.getUrl(), item);
                            urls.add(item.getUrl());

                            itemMap.put(item.getUrl(), item);
                            knownUrls.add(item.getUrl());
                        }
                    }
                }
            } catch (Exception e) {
                LOGGER.error("Hiba a város fájl betöltésekor [{}]: {}", cityFile.getName(), e.getMessage());
            }
        }
    }

    private void loadFromAggregateJson() {
        try (Reader reader = Files.newBufferedReader(globalDbFile, StandardCharsets.UTF_8)) {
            Type listType = new TypeToken<List<IngatlanItem>>() {}.getType();
            List<IngatlanItem> items = GSON.fromJson(reader, listType);
            if (items != null) {
                for (IngatlanItem item : items) {
                    if (item.getUrl() != null) {
                        String key = CityUtils.normalizeCityKey(item.getCity());
                        cityItemMaps.computeIfAbsent(key, k -> new ConcurrentHashMap<>()).put(item.getUrl(), item);
                        cityKnownUrls.computeIfAbsent(key, k -> ConcurrentHashMap.newKeySet()).add(item.getUrl());

                        itemMap.put(item.getUrl(), item);
                        knownUrls.add(item.getUrl());
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error("Nem sikerült beolvasni a globális adatbázis fájlt [{}]: {}", globalDbFile.getFileName(), e.getMessage());
        }
    }

    private void writeJsonAtomically(Path targetPath, String json) throws IOException {
        Path parent = targetPath.getParent();
        if (parent != null && !Files.exists(parent)) {
            Files.createDirectories(parent);
        }
        Path tempFile = targetPath.resolveSibling(targetPath.getFileName() + ".tmp");
        Files.writeString(tempFile, json, StandardCharsets.UTF_8);
        try {
            Files.move(tempFile, targetPath, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(tempFile, targetPath, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    @Override
    public boolean hasCityDatabase(String city) {
        if (city == null || city.isBlank()) return false;
        String cityKey = CityUtils.normalizeCityKey(city);
        rwLock.readLock().lock();
        try {
            Path cityFile = resolveAndValidateCityFile(cityKey);
            if (Files.exists(cityFile) && Files.size(cityFile) > 5) {
                return true;
            }
            Set<String> urls = cityKnownUrls.get(cityKey);
            return urls != null && !urls.isEmpty();
        } catch (Exception ignored) {
            return false;
        } finally {
            rwLock.readLock().unlock();
        }
    }

    @Override
    public boolean isNewUrl(String city, String url) {
        if (url == null || url.isBlank()) return false;
        String cityKey = CityUtils.normalizeCityKey(city);
        rwLock.readLock().lock();
        try {
            if (!hasCityDatabase(city)) {
                return false;
            }
            Set<String> urls = cityKnownUrls.get(cityKey);
            if (urls == null) {
                return false;
            }
            return !urls.contains(url);
        } finally {
            rwLock.readLock().unlock();
        }
    }

    @Override
    public boolean containsUrl(String url) {
        return knownUrls.contains(url);
    }

    @Override
    public void saveCityItems(String city, Collection<IngatlanItem> items) {
        if (items == null || items.isEmpty()) return;
        String cityKey = CityUtils.normalizeCityKey(city);

        rwLock.writeLock().lock();
        try {
            Path cityFile = resolveAndValidateCityFile(cityKey);
            Map<String, IngatlanItem> map = cityItemMaps.computeIfAbsent(cityKey, k -> new ConcurrentHashMap<>());
            Set<String> urls = cityKnownUrls.computeIfAbsent(cityKey, k -> ConcurrentHashMap.newKeySet());
            List<String> newUrlsForLegacy = new ArrayList<>();

            for (IngatlanItem item : items) {
                if (item.getUrl() != null) {
                    IngatlanItem existing = map.get(item.getUrl());
                    if (existing == null) {
                        existing = itemMap.get(item.getUrl());
                    }
                    if (existing != null && existing.isViewed() && !item.isViewed()) {
                        item.setViewed(true);
                        item.setViewedAt(existing.getViewedAt());
                    }

                    map.put(item.getUrl(), item);
                    urls.add(item.getUrl());

                    itemMap.put(item.getUrl(), item);
                    knownUrls.add(item.getUrl());
                    newUrlsForLegacy.add(item.getUrl());
                }
            }

            this.lastRunTime = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(LocalDateTime.now());

            String cityJson = GSON.toJson(new ArrayList<>(map.values()));
            writeJsonAtomically(cityFile, cityJson);

            String dbJson = GSON.toJson(new ArrayList<>(itemMap.values()));
            writeJsonAtomically(globalDbFile, dbJson);

            if (legacyHistoryFile != null && !newUrlsForLegacy.isEmpty()) {
                try {
                    FileUtils.writeLines(legacyHistoryFile.toFile(), StandardCharsets.UTF_8.name(), newUrlsForLegacy, true);
                } catch (IOException e) {
                    LOGGER.warn("Hiba a legacy naplófájl mentésekor: {}", e.getMessage());
                }
            }
        } catch (IOException e) {
            LOGGER.error("Hiba a város JSON mentésekor [{}]: {}", cityKey, e.getMessage(), e);
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    @Override
    public void saveAll(Collection<IngatlanItem> items) {
        if (items == null || items.isEmpty()) return;

        Map<String, List<IngatlanItem>> byCity = new HashMap<>();
        for (IngatlanItem item : items) {
            String city = item.getCity();
            if (city == null || city.isBlank()) {
                city = item.getLocation();
            }
            if (city == null || city.isBlank()) {
                city = "Debrecen";
            }
            byCity.computeIfAbsent(city, k -> new ArrayList<>()).add(item);
        }

        for (Map.Entry<String, List<IngatlanItem>> entry : byCity.entrySet()) {
            saveCityItems(entry.getKey(), entry.getValue());
        }
    }

    @Override
    public boolean markAsViewed(String url) {
        if (url == null || url.isBlank()) {
            return false;
        }

        rwLock.writeLock().lock();
        try {
            String timestamp = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(LocalDateTime.now());
            boolean found = false;

            IngatlanItem globalItem = itemMap.get(url);
            if (globalItem != null) {
                globalItem.setViewed(true);
                globalItem.setViewedAt(timestamp);
                found = true;
            }

            String affectedCityKey = null;
            for (Map.Entry<String, Map<String, IngatlanItem>> entry : cityItemMaps.entrySet()) {
                IngatlanItem cityItem = entry.getValue().get(url);
                if (cityItem != null) {
                    cityItem.setViewed(true);
                    cityItem.setViewedAt(timestamp);
                    affectedCityKey = entry.getKey();
                    found = true;
                    break;
                }
            }

            if (affectedCityKey != null) {
                try {
                    Path cityFile = resolveAndValidateCityFile(affectedCityKey);
                    Map<String, IngatlanItem> map = cityItemMaps.get(affectedCityKey);
                    if (map != null) {
                        writeJsonAtomically(cityFile, GSON.toJson(new ArrayList<>(map.values())));
                    }
                } catch (IOException e) {
                    LOGGER.error("Hiba a megtekintett állapot mentésekor [{}]: {}", affectedCityKey, e.getMessage());
                }
            }

            try {
                writeJsonAtomically(globalDbFile, GSON.toJson(new ArrayList<>(itemMap.values())));
            } catch (IOException e) {
                LOGGER.error("Hiba a teljes adatbázis megtekintési állapot mentésekor: {}", e.getMessage());
            }

            return found;
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    @Override
    public boolean isViewed(String url) {
        if (url == null || url.isBlank()) {
            return false;
        }
        IngatlanItem item = itemMap.get(url);
        if (item != null) {
            return item.isViewed();
        }
        for (Map<String, IngatlanItem> map : cityItemMaps.values()) {
            IngatlanItem cityItem = map.get(url);
            if (cityItem != null) {
                return cityItem.isViewed();
            }
        }
        return false;
    }

    @Override
    public Set<String> getAllUrls() {
        return Collections.unmodifiableSet(knownUrls);
    }

    @Override
    public List<IngatlanItem> getAll() {
        rwLock.readLock().lock();
        try {
            List<IngatlanItem> list = new ArrayList<>(itemMap.values());
            list.sort((a, b) -> {
                if (a.getDiscoveredAt() == null) return 1;
                if (b.getDiscoveredAt() == null) return -1;
                return b.getDiscoveredAt().compareTo(a.getDiscoveredAt());
            });
            return list;
        } finally {
            rwLock.readLock().unlock();
        }
    }

    @Override
    public List<IngatlanItem> getItemsByCity(String city) {
        if (city == null || city.isBlank()) {
            return getAll();
        }
        String key = CityUtils.normalizeCityKey(city);
        rwLock.readLock().lock();
        try {
            Map<String, IngatlanItem> map = cityItemMaps.get(key);
            if (map == null) {
                return Collections.emptyList();
            }
            List<IngatlanItem> list = new ArrayList<>(map.values());
            list.sort((a, b) -> {
                if (a.getDiscoveredAt() == null) return 1;
                if (b.getDiscoveredAt() == null) return -1;
                return b.getDiscoveredAt().compareTo(a.getDiscoveredAt());
            });
            return list;
        } finally {
            rwLock.readLock().unlock();
        }
    }

    @Override
    public List<Map<String, Object>> getTrackedCitiesInfo() {
        rwLock.readLock().lock();
        try {
            List<Map<String, Object>> result = new ArrayList<>();
            File[] files = citiesDir.toFile().listFiles((dir, name) -> name.endsWith(".json"));

            if (files != null) {
                for (File file : files) {
                    String key = file.getName().replace(".json", "");
                    Map<String, IngatlanItem> map = cityItemMaps.get(key);
                    int count = map != null ? map.size() : 0;
                    String displayName = key.substring(0, 1).toUpperCase(Locale.ROOT) + key.substring(1);

                    if (map != null && !map.isEmpty()) {
                        for (IngatlanItem item : map.values()) {
                            if (item.getCity() != null && !item.getCity().isBlank()) {
                                displayName = item.getCity();
                                break;
                            }
                        }
                    }

                    Map<String, Object> info = new LinkedHashMap<>();
                    info.put("key", key);
                    info.put("name", displayName);
                    info.put("itemCount", count);
                    info.put("lastModified", DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                            .withZone(ZoneId.systemDefault())
                            .format(Instant.ofEpochMilli(file.lastModified())));
                    result.add(info);
                }
            }
            result.sort((a, b) -> ((String) a.get("name")).compareToIgnoreCase((String) b.get("name")));
            return result;
        } finally {
            rwLock.readLock().unlock();
        }
    }

    @Override
    public void clearCityHistory(String city) {
        if (city == null || city.isBlank()) return;
        String key = CityUtils.normalizeCityKey(city);

        rwLock.writeLock().lock();
        try {
            Map<String, IngatlanItem> removedItems = cityItemMaps.remove(key);
            Set<String> removedUrls = cityKnownUrls.remove(key);

            if (removedUrls != null) {
                for (String url : removedUrls) {
                    knownUrls.remove(url);
                    itemMap.remove(url);
                }
            }

            try {
                Path cityFile = resolveAndValidateCityFile(key);
                Files.deleteIfExists(cityFile);

                String json = GSON.toJson(new ArrayList<>(itemMap.values()));
                writeJsonAtomically(globalDbFile, json);
            } catch (Exception e) {
                LOGGER.error("Hiba a város adatbázis törlésekor [{}]: {}", key, e.getMessage());
            }
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    @Override
    public void clearHistory() {
        rwLock.writeLock().lock();
        try {
            itemMap.clear();
            knownUrls.clear();
            cityItemMaps.clear();
            cityKnownUrls.clear();

            if (Files.exists(citiesDir)) {
                FileUtils.cleanDirectory(citiesDir.toFile());
            }
            Files.deleteIfExists(globalDbFile);
            if (legacyHistoryFile != null) {
                Files.deleteIfExists(legacyHistoryFile);
            }
        } catch (Exception e) {
            LOGGER.error("Hiba az előzmények törlésekor: {}", e.getMessage(), e);
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    @Override
    public int getTotalCount() {
        return itemMap.size();
    }

    @Override
    public String getLastRunTime() {
        return lastRunTime;
    }

    public Path getCitiesDir() {
        return citiesDir;
    }

    public Path getGlobalDbFile() {
        return globalDbFile;
    }
}
