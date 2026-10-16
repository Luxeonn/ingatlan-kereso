package ingatlanok.persistence;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import ingatlanok.common.AppLogger;
import ingatlanok.model.SearchCriteria;
import org.dizitart.no2.collection.Document;
import org.dizitart.no2.collection.NitriteCollection;
import org.dizitart.no2.collection.UpdateOptions;

import java.util.concurrent.locks.ReentrantReadWriteLock;

import static org.dizitart.no2.filters.FluentFilter.where;

/**
 * Beágyazott Nitrite NoSQL Dokumentum Adatbázisra épülő ConfigStorage implementáció.
 * A rendszer és keresési konfigurációkat sematikus és rugalmas NoSQL dokumentumként tárolja.
 */
public class NitriteConfigStorage implements ConfigStorage {

    private static final AppLogger LOGGER = AppLogger.getLogger(NitriteConfigStorage.class);
    private static final String CONFIG_KEY_LAST_CRITERIA = "last_criteria";

    private volatile NitriteDatabaseManager dbManager;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public NitriteConfigStorage() {
        this(null);
    }

    public NitriteConfigStorage(NitriteDatabaseManager dbManager) {
        this.dbManager = dbManager != null ? dbManager : NitriteDatabaseManager.getInstance(null);
    }

    public NitriteDatabaseManager getDbManager() {
        if (dbManager == null || dbManager.isClosed()) {
            dbManager = NitriteDatabaseManager.getInstance(dbManager != null ? dbManager.getDatabasePath() : null);
        }
        return dbManager;
    }

    @Override
    public SearchCriteria getLastCriteria() {
        NitriteDatabaseManager mgr = getDbManager();
        ReentrantReadWriteLock.ReadLock lock = mgr.getRwLock().readLock();
        lock.lock();
        try {
            NitriteCollection col = mgr.getConfigCollection();
            Document doc = col.find(where("key").eq(CONFIG_KEY_LAST_CRITERIA)).firstOrNull();
            if (doc != null) {
                String json = doc.get("value", String.class);
                if (json != null && !json.isBlank()) {
                    SearchCriteria criteria = gson.fromJson(json, SearchCriteria.class);
                    if (criteria != null) {
                        return criteria;
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error("Hiba a konfiguráció NoSQL dokumentumból való beolvasásakor: {}", e.getMessage(), e);
        } finally {
            lock.unlock();
        }
        return new SearchCriteria();
    }

    @Override
    public void saveLastCriteria(SearchCriteria criteria) {
        if (criteria == null) {
            return;
        }
        NitriteDatabaseManager mgr = getDbManager();
        ReentrantReadWriteLock.WriteLock lock = mgr.getRwLock().writeLock();
        lock.lock();
        try {
            NitriteCollection col = mgr.getConfigCollection();
            String json = gson.toJson(criteria);

            Document doc = Document.createDocument("key", CONFIG_KEY_LAST_CRITERIA)
                    .put("value", json)
                    .put("updatedAt", java.time.LocalDateTime.now().toString());

            UpdateOptions options = new UpdateOptions();
            options.setInsertIfAbsent(true);
            col.update(where("key").eq(CONFIG_KEY_LAST_CRITERIA), doc, options);
        } catch (Exception e) {
            LOGGER.error("Hiba a konfiguráció NoSQL dokumentumba mentésekor: {}", e.getMessage(), e);
        } finally {
            lock.unlock();
        }
    }
}
