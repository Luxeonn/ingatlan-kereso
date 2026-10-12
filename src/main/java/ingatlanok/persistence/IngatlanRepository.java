package ingatlanok.persistence;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import ingatlanok.model.IngatlanItem;
import ingatlanok.model.SearchCriteria;
import org.apache.commons.io.FileUtils;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Fájlalapú JSON/szöveges "adatbázis" kezelő a hirdetésekhez és beállításokhoz.
 */
public class IngatlanRepository {

    private static final String DATA_DIR = "data";
    private static final String DB_FILE = "data/ingatlanok_db.json";
    private static final String CONFIG_FILE = "data/last_config.json";
    private static final String LEGACY_HISTORY_FILE = "ingatlanok_history.txt";
    private static final String OLD_HISTORY_FILE = "2017_06_06.txt";

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final IngatlanRepository INSTANCE = new IngatlanRepository();

    private final Map<String, IngatlanItem> itemMap = new ConcurrentHashMap<>();
    private final Set<String> knownUrls = ConcurrentHashMap.newKeySet();
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

        // 1. JSON adatbázis betöltése ha létezik
        File dbFile = new File(DB_FILE);
        if (dbFile.exists()) {
            loadFromJson(dbFile);
        }

        // 2. Legacy fájlok importálása (ha vannak korábbi txt fájlok és még nincsenek a DB-ben)
        importLegacyFile(new File(OLD_HISTORY_FILE));
        importLegacyFile(new File(LEGACY_HISTORY_FILE));

        System.out.println("[Adatbázis] Betöltve: " + itemMap.size() + " korábbi hirdetés.");
    }

    private void loadFromJson(File file) {
        try (Reader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
            Type listType = new TypeToken<List<IngatlanItem>>() {}.getType();
            List<IngatlanItem> items = GSON.fromJson(reader, listType);
            if (items != null) {
                for (IngatlanItem item : items) {
                    if (item.getUrl() != null) {
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
                            "Ismeretlen",
                            "Archív előzmény",
                            timestamp,
                            false
                    );
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

    public synchronized void saveAll(Collection<IngatlanItem> items) {
        if (items == null || items.isEmpty()) return;

        List<String> newUrlsForLegacy = new ArrayList<>();
        for (IngatlanItem item : items) {
            if (item.getUrl() != null) {
                itemMap.put(item.getUrl(), item);
                knownUrls.add(item.getUrl());
                newUrlsForLegacy.add(item.getUrl());
            }
        }
        this.lastRunTime = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(LocalDateTime.now());

        // Mentés JSON fájlba
        try {
            File dbFile = new File(DB_FILE);
            String json = GSON.toJson(new ArrayList<>(itemMap.values()));
            FileUtils.writeStringToFile(dbFile, json, StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.err.println("Hiba a JSON mentésekor: " + e.getMessage());
        }

        // Szinkronizálás a hagyományos ingatlanok_history.txt fájlba is a kompatibilitásért
        try {
            File legacyFile = new File(LEGACY_HISTORY_FILE);
            FileUtils.writeLines(legacyFile, StandardCharsets.UTF_8.name(), newUrlsForLegacy, true);
        } catch (IOException e) {
            System.err.println("Hiba a legacy fájl mentésekor: " + e.getMessage());
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
        // Csökkenő sorrend dátum alapján (legfrissebb elöl)
        list.sort((a, b) -> {
            if (a.getDiscoveredAt() == null) return 1;
            if (b.getDiscoveredAt() == null) return -1;
            return b.getDiscoveredAt().compareTo(a.getDiscoveredAt());
        });
        return list;
    }

    public synchronized void clearHistory() {
        itemMap.clear();
        knownUrls.clear();
        try {
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
                // Ignore, return default
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
