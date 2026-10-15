package ingatlanok.persistence.dao;

import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import java.util.Optional;

/**
 * Jdbi SqlObject DAO a keresési konfiguráció tárolásához.
 */
public interface ConfigDao {

    @SqlQuery("SELECT config_value FROM search_config WHERE config_key = :key LIMIT 1;")
    Optional<String> loadConfigValue(@Bind("key") String key);

    @SqlUpdate("INSERT INTO search_config (config_key, config_value, updated_at) VALUES (:key, :value, CURRENT_TIMESTAMP) ON CONFLICT(config_key) DO UPDATE SET config_value = excluded.config_value, updated_at = CURRENT_TIMESTAMP;")
    void saveConfigValue(@Bind("key") String key, @Bind("value") String value);
}
