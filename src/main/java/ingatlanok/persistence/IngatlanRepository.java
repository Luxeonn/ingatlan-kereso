package ingatlanok.persistence;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import ingatlanok.common.CityUtils;
import ingatlanok.model.IngatlanItem;
import ingatlanok.model.SearchCriteria;
import org.apache.commons.io.FileUtils;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class IngatlanRepository {

    private static final String DATA_DIR = "data";
    private static final String CITIES_DIR = "data/cities";
    private static final String DB_FILE = "data/ingatlanok_db.json";
    private static final String CONFIG_FILE = "data/last_config.json";
    private static final String LEGACY_HISTORY_FILE = "ingatlanok_history.txt";
    private static final String OLD_HISTORY_FILE = "2017_06_06.txt";

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final IngatlanRepository INSTANCE = new IngatlanRepository();

    private final Map<String, IngatlanItem> itemMap = new ConcurrentHashMap<>();
    private final Set<String> knownUrls = ConcurrentHashMap.newKeySet();

    private final Map<String, Map<String, IngatlanItem>> cityItemMaps = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> cityKnownUrls = new ConcurrentHashMap<>();

    private String lastRunTime = null;

    private IngatlanRepository() {
        init();
    }

    public static IngatlanRepository getInstance() {
        return INSTANCE;
    }

    private synchronized void init() {
        File dataDir = new File(DATA_DIR);
        if (!dataDir.exists()) {
            dataDir.mkdirs();
        }

        File citiesDir = new File(CITIES_DIR);
        if (!citiesDir.exists()) {
            citiesDir.mkdirs();
        }

        File[] existingCityFiles = citiesDir.listFiles((dir, name) -> name.endsWith(".json"));
        boolean hasCityFiles = existingCityFiles != null && existingCityFiles.length > 0;

        File dbFile = new File(DB_FILE);
        if (!hasCityFiles && dbFile.exists()) {
            migrateOldDatabaseToPerCityStructure(dbFile);
        }

        loadFromCitiesDirectory(citiesDir);

        if (itemMap.isEmpty() && dbFile.exists()) {
            loadFromAggregateJson(dbFile);
        }

        importLegacyFile(new File(OLD_HISTORY_FILE));
        importLegacyFile(new File(LEGACY_HISTORY_FILE));

        System.out.println("[Adatbázis] Betöltve: " + itemMap.size() + " korábbi hirdetés " + cityItemMaps.size() + " városból.");
    }

    private void migrateOldDatabaseToPerCityStructure(File file) {
        System.out.println("[Adatbázis] Meglévő adatbázis migrálása városonkénti struktúrára...");
        try (Reader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
            Type listType = new TypeToken<List<IngatlanItem>>() {}.getType();
            List<IngatlanItem> items = GSON.fromJson(reader, listType);
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

                File citiesDir = new File(CITIES_DIR);
                for (Map.Entry<String, List<IngatlanItem>> entry : grouped.entrySet()) {
                    File cityFile = new File(citiesDir, entry.getKey() + ".json");
                    String json = GSON.toJson(entry.getValue());
                    FileUtils.writeStringToFile(cityFile, json, StandardCharsets.UTF_8);
                }
                System.out.println("[Adatbázis] Sikeres migráció: " + grouped.size() + " város JSON létrehozva.");
            }
        } catch (Exception e) {
            System.err.println("Nem sikerült a régi adatbázis migrálása: " + e.getMessage());
        }
    }

    private void loadFromCitiesDirectory(File citiesDir) {
        File[] files = citiesDir.listFiles((dir, name) -> name.endsWith(".json"));
        if (files == null) return;

        Type listType = new TypeToken<List<IngatlanItem>>() {}.getType();
        for (File cityFile : files) {
            String cityKey = cityFile.getName().replace(".json", "");
            try (Reader reader = new InputStreamReader(new FileInputStream(cityFile), StandardCharsets.UTF_8)) {
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
                System.err.println("Hiba a város fájl betöltésekor [" + cityFile.getName() + "]: " + e.getMessage());
            }
        }
    }

    private void loadFromAggregateJson(File file) {
        try (Reader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
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
            System.err.println("Nem sikerült beolvasni az adatbázis fájlt [" + file.getName() + "]: " + e.getMessage());
        }
    }

    private void importLegacyFile(File file) {
        if (!file.exists()) return;
        try (BufferedReader br = new BufferedReader(new FileReader(file, StandardCharsets.UTF_8))) {
            String line;
            String timestamp = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(LocalDateTime.now());
            String debrecenKey = CityUtils.normalizeCityKey("Debrecen");
            Map<String, IngatlanItem> debMap = cityItemMaps.computeIfAbsent(debrecenKey, k -> new ConcurrentHashMap<>());
            Set<String> debUrls = cityKnownUrls.computeIfAbsent(debrecenKey, k -> ConcurrentHashMap.newKeySet());

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
                }
            }
        } catch (IOException e) {
            System.err.println("Legacy fájl import hiba [" + file.getName() + "]: " + e.getMessage());
        }
    }

    private String detectPortal(String url) {
        if (url.contains("dh.hu")) return "Duna House";
        if (url.contains("oc.hu")) return "Otthon Centrum";
        if (url.contains("jofogas.hu")) return "Jófogás Ingatlan";
        if (url.contains("ingatlan.com")) return "Ingatlan.com";
        if (url.contains("ingatlanbazar.hu")) return "Ingatlanbazár";
        return "Egyéb";
    }

    public boolean hasCityDatabase(String city) {
        if (city == null || city.isBlank()) return false;
        String cityKey = CityUtils.normalizeCityKey(city);
        File cityFile = new File(CITIES_DIR, cityKey + ".json");
        if (cityFile.exists() && cityFile.length() > 5) {
            return true;
        }
        Set<String> urls = cityKnownUrls.get(cityKey);
        return urls != null && !urls.isEmpty();
    }

    public boolean isNewUrl(String city, String url) {
        if (url == null || url.isBlank()) return false;
        String cityKey = CityUtils.normalizeCityKey(city);
        if (!hasCityDatabase(city)) {

            return false;
        }
        Set<String> urls = cityKnownUrls.get(cityKey);
        if (urls == null) {
            return false;
        }
        return !urls.contains(url);
    }

    public synchronized void saveCityItems(String city, Collection<IngatlanItem> items) {
        if (items == null || items.isEmpty()) return;
        String cityKey = CityUtils.normalizeCityKey(city);

        Map<String, IngatlanItem> map = cityItemMaps.computeIfAbsent(cityKey, k -> new ConcurrentHashMap<>());
        Set<String> urls = cityKnownUrls.computeIfAbsent(cityKey, k -> ConcurrentHashMap.newKeySet());
        List<String> newUrlsForLegacy = new ArrayList<>();

        for (IngatlanItem item : items) {
            if (item.getUrl() != null) {
                map.put(item.getUrl(), item);
                urls.add(item.getUrl());

                itemMap.put(item.getUrl(), item);
                knownUrls.add(item.getUrl());
                newUrlsForLegacy.add(item.getUrl());
            }
        }
        this.lastRunTime = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(LocalDateTime.now());

        try {
            File cityFile = new File(CITIES_DIR, cityKey + ".json");
            String json = GSON.toJson(new ArrayList<>(map.values()));
            FileUtils.writeStringToFile(cityFile, json, StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.err.println("Hiba a város JSON mentésekor [" + cityKey + "]: " + e.getMessage());
        }

        try {
            File dbFile = new File(DB_FILE);
            String json = GSON.toJson(new ArrayList<>(itemMap.values()));
            FileUtils.writeStringToFile(dbFile, json, StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.err.println("Hiba a teljes JSON adatbázis mentésekor: " + e.getMessage());
        }

        try {
            File legacyFile = new File(LEGACY_HISTORY_FILE);
            FileUtils.writeLines(legacyFile, StandardCharsets.UTF_8.name(), newUrlsForLegacy, true);
        } catch (IOException e) {
            System.err.println("Hiba a legacy fájl mentésekor: " + e.getMessage());
        }
    }

    public synchronized void saveAll(Collection<IngatlanItem> items) {
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

    public boolean containsUrl(String url) {
        return knownUrls.contains(url);
    }

    public Set<String> getAllUrls() {
        return Collections.unmodifiableSet(knownUrls);
    }

    public List<IngatlanItem> getAll() {
        List<IngatlanItem> list = new ArrayList<>(itemMap.values());
        list.sort((a, b) -> {
            if (a.getDiscoveredAt() == null) return 1;
            if (b.getDiscoveredAt() == null) return -1;
            return b.getDiscoveredAt().compareTo(a.getDiscoveredAt());
        });
        return list;
    }

    public List<IngatlanItem> getItemsByCity(String city) {
        if (city == null || city.isBlank()) {
            return getAll();
        }
        String key = CityUtils.normalizeCityKey(city);
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
    }

    public List<Map<String, Object>> getTrackedCitiesInfo() {
        List<Map<String, Object>> result = new ArrayList<>();
        File citiesDir = new File(CITIES_DIR);
        File[] files = citiesDir.listFiles((dir, name) -> name.endsWith(".json"));

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
    }

    public synchronized void clearCityHistory(String city) {
        if (city == null || city.isBlank()) return;
        String key = CityUtils.normalizeCityKey(city);

        Map<String, IngatlanItem> removedItems = cityItemMaps.remove(key);
        Set<String> removedUrls = cityKnownUrls.remove(key);

        if (removedUrls != null) {
            for (String url : removedUrls) {
                knownUrls.remove(url);
                itemMap.remove(url);
            }
        }

        try {
            File cityFile = new File(CITIES_DIR, key + ".json");
            if (cityFile.exists()) {
                cityFile.delete();
            }

            File dbFile = new File(DB_FILE);
            String json = GSON.toJson(new ArrayList<>(itemMap.values()));
            FileUtils.writeStringToFile(dbFile, json, StandardCharsets.UTF_8);
        } catch (Exception e) {
            System.err.println("Hiba a város adatbázis törlésekor [" + key + "]: " + e.getMessage());
        }
    }

    public synchronized void clearHistory() {
        itemMap.clear();
        knownUrls.clear();
        cityItemMaps.clear();
        cityKnownUrls.clear();
        try {
            File citiesDir = new File(CITIES_DIR);
            if (citiesDir.exists()) {
                FileUtils.cleanDirectory(citiesDir);
            }
            File dbFile = new File(DB_FILE);
            if (dbFile.exists()) {
                dbFile.delete();
            }
            File legacyFile = new File(LEGACY_HISTORY_FILE);
            if (legacyFile.exists()) {
                legacyFile.delete();
            }
        } catch (Exception e) {
            System.err.println("Hiba az előzmények törlésekor: " + e.getMessage());
        }
    }

    public SearchCriteria getLastCriteria() {
        File conf = new File(CONFIG_FILE);
        if (conf.exists()) {
            try (Reader reader = new InputStreamReader(new FileInputStream(conf), StandardCharsets.UTF_8)) {
                SearchCriteria sc = GSON.fromJson(reader, SearchCriteria.class);
                if (sc != null) return sc;
            } catch (Exception e) {

            }
        }
        return new SearchCriteria();
    }

    public void saveLastCriteria(SearchCriteria criteria) {
        if (criteria == null) return;
        try {
            File conf = new File(CONFIG_FILE);
            String json = GSON.toJson(criteria);
            FileUtils.writeStringToFile(conf, json, StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.err.println("Hiba a konfiguráció mentésekor: " + e.getMessage());
        }
    }

    public int getTotalCount() {
        return itemMap.size();
    }

    public String getLastRunTime() {
        return lastRunTime;
    }
}
