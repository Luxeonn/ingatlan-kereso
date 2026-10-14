package ingatlanok.persistence;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import ingatlanok.common.AppLogger;
import ingatlanok.model.SearchCriteria;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * SQLite alapú keresési konfiguráció-tároló implementáció.
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
            String sql = "SELECT config_value FROM search_config WHERE config_key = ? LIMIT 1;";
            try (Connection conn = dbManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, CONFIG_KEY_LAST);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        String json = rs.getString("config_value");
                        if (json != null && !json.isBlank()) {
                            SearchCriteria sc = GSON.fromJson(json, SearchCriteria.class);
                            if (sc != null) {
                                return sc;
                            }
                        }
                    }
                }
            } catch (Exception e) {
                LOGGER.error("Hiba a konfiguráció SQLite-ból történő olvasásakor: {}", e.getMessage());
            }
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
            String sql = """
                INSERT INTO search_config (config_key, config_value, updated_at)
                VALUES (?, ?, CURRENT_TIMESTAMP)
                ON CONFLICT(config_key) DO UPDATE SET
                    config_value = excluded.config_value,
                    updated_at = CURRENT_TIMESTAMP;
            """;
            try (Connection conn = dbManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, CONFIG_KEY_LAST);
                ps.setString(2, json);
                ps.executeUpdate();
            } catch (SQLException e) {
                LOGGER.error("Hiba a konfiguráció SQLite mentésekor: {}", e.getMessage(), e);
            }
        } finally {
            rwLock.writeLock().unlock();
        }
    }
}
