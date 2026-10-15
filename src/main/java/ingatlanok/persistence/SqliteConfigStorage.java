package ingatlanok.persistence;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import ingatlanok.common.AppLogger;
import ingatlanok.model.SearchCriteria;
import ingatlanok.persistence.dao.ConfigDao;

import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * SQLite alapú keresési konfiguráció-tároló implementáció Jdbi v3 DAO-val.
 */
public class SqliteConfigStorage implements ConfigStorage {

    private static final AppLogger LOGGER = AppLogger.getLogger(SqliteConfigStorage.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String CONFIG_KEY_LAST = "last_criteria";

    private final SqliteDatabaseManager dbManager;
    private final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();

    public SqliteConfigStorage() {
        this(new SqliteDatabaseManager());
    }

    public SqliteConfigStorage(SqliteDatabaseManager dbManager) {
        this.dbManager = dbManager != null ? dbManager : new SqliteDatabaseManager();
    }

    public SqliteDatabaseManager getDbManager() {
        return dbManager;
    }

    @Override
    public SearchCriteria getLastCriteria() {
        rwLock.readLock().lock();
        try {
            return dbManager.getJdbi().withExtension(ConfigDao.class, dao -> dao.loadConfigValue(CONFIG_KEY_LAST))
                    .map(json -> GSON.fromJson(json, SearchCriteria.class))
                    .orElseGet(SearchCriteria::new);
        } catch (Exception e) {
            LOGGER.error("Hiba a konfiguráció Jdbi SQLite-ból történő olvasásakor: {}", e.getMessage(), e);
            return new SearchCriteria();
        } finally {
            rwLock.readLock().unlock();
        }
    }

    @Override
    public void saveLastCriteria(SearchCriteria criteria) {
        if (criteria == null) {
            return;
        }
        rwLock.writeLock().lock();
        try {
            String json = GSON.toJson(criteria);
            dbManager.getJdbi().useExtension(ConfigDao.class, dao -> dao.saveConfigValue(CONFIG_KEY_LAST, json));
        } catch (Exception e) {
            LOGGER.error("Hiba a konfiguráció Jdbi SQLite-ba mentésekor: {}", e.getMessage(), e);
        } finally {
            rwLock.writeLock().unlock();
        }
    }
}
