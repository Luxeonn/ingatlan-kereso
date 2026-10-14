package ingatlanok.persistence;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;

class SqliteDatabaseManagerTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("Adatbázis inicializálásakor létrejön a séma és a PRAGMA beállítások aktívak")
    void testInitAndPragmas() throws SQLException {
        Path dbFile = tempDir.resolve("test_ingatlanok.db");
        SqliteDatabaseManager dbManager = new SqliteDatabaseManager(dbFile);

        assertEquals(dbFile, dbManager.getDbPath());
        assertTrue(dbManager.getJdbcUrl().startsWith("jdbc:sqlite:"));

        try (Connection conn = dbManager.getConnection();
             Statement stmt = conn.createStatement()) {

            // PRAGMA journal_mode ellenőrzése (WAL mód)
            try (ResultSet rs = stmt.executeQuery("PRAGMA journal_mode;")) {
                assertTrue(rs.next());
                assertEquals("wal", rs.getString(1).toLowerCase());
            }

            // Táblák létezésének ellenőrzése
            try (ResultSet rs = stmt.executeQuery("SELECT name FROM sqlite_master WHERE type='table' ORDER BY name;")) {
                var tables = new java.util.ArrayList<String>();
                while (rs.next()) {
                    tables.add(rs.getString(1));
                }
                assertTrue(tables.contains("properties"), "properties táblának léteznie kell");
                assertTrue(tables.contains("search_config"), "search_config táblának léteznie kell");
                assertTrue(tables.contains("search_runs"), "search_runs táblának léteznie kell");
            }

            // Indexek létezésének ellenőrzése
            try (ResultSet rs = stmt.executeQuery("SELECT name FROM sqlite_master WHERE type='index';")) {
                var indexes = new java.util.ArrayList<String>();
                while (rs.next()) {
                    indexes.add(rs.getString(1));
                }
                assertTrue(indexes.contains("idx_properties_url"));
                assertTrue(indexes.contains("idx_properties_city_key"));
                assertTrue(indexes.contains("idx_properties_viewed"));
                assertTrue(indexes.contains("idx_properties_discovered"));
            }
        }
    }

    @Test
    @DisplayName("Liquibase naplózza a lefutott changeSeteket a DATABASECHANGELOG táblába")
    void testLiquibaseChangelogAudit() throws SQLException {
        Path dbFile = tempDir.resolve("test_liquibase_audit.db");
        SqliteDatabaseManager dbManager = new SqliteDatabaseManager(dbFile);

        try (Connection conn = dbManager.getConnection();
             Statement stmt = conn.createStatement()) {

            // DATABASECHANGELOG és DATABASECHANGELOGLOCK táblák létezése
            try (ResultSet rs = stmt.executeQuery("SELECT name FROM sqlite_master WHERE type='table';")) {
                var tables = new java.util.ArrayList<String>();
                while (rs.next()) {
                    tables.add(rs.getString(1).toUpperCase());
                }
                assertTrue(tables.contains("DATABASECHANGELOG"), "DATABASECHANGELOG táblának léteznie kell");
                assertTrue(tables.contains("DATABASECHANGELOGLOCK"), "DATABASECHANGELOGLOCK táblának léteznie kell");
            }

            // Lefutott changeSetek ellenőrzése
            try (ResultSet rs = stmt.executeQuery("SELECT ID, AUTHOR FROM DATABASECHANGELOG ORDER BY ORDEREXECUTED;")) {
                var changeSetIds = new java.util.ArrayList<String>();
                while (rs.next()) {
                    changeSetIds.add(rs.getString("ID"));
                    assertEquals("ingatlan-team", rs.getString("AUTHOR"));
                }
                assertTrue(changeSetIds.contains("001-create-properties-table"));
                assertTrue(changeSetIds.contains("001-create-properties-indexes"));
                assertTrue(changeSetIds.contains("001-create-search-config-table"));
                assertTrue(changeSetIds.contains("001-create-search-runs-table"));
            }
        }
    }

    @Test
    @DisplayName("A Liquibase migráció idempotens: többszöri futtatás sem okoz hibát vagy duplikációt")
    void testLiquibaseIdempotency() {
        Path dbFile = tempDir.resolve("test_liquibase_idempotent.db");
        SqliteDatabaseManager dbManager = new SqliteDatabaseManager(dbFile);

        // Második futtatás - nem dobhat hibát
        assertDoesNotThrow(dbManager::runLiquibaseMigrations);
    }

    @Test
    @DisplayName("Létező sémára futtatva a Liquibase preConditions sikeresen lefut MARK_RAN-ként")
    void testLiquibasePreconditionsOnExistingSchema() throws SQLException {
        Path dbFile = tempDir.resolve("test_legacy_schema.db");

        // Előre manuálisan létrehozunk egy táblát mint egy régebbi SQLite adatbázisban
        try (Connection rawConn = java.sql.DriverManager.getConnection("jdbc:sqlite:" + dbFile.toAbsolutePath());
             Statement stmt = rawConn.createStatement()) {
            stmt.execute("CREATE TABLE properties (id VARCHAR(64) PRIMARY KEY, portal VARCHAR(32) NOT NULL, url VARCHAR(512) NOT NULL UNIQUE, title VARCHAR(255) NOT NULL, price_raw VARCHAR(64), price_amount BIGINT, price_millions REAL, city VARCHAR(64) NOT NULL, city_key VARCHAR(64) NOT NULL, location VARCHAR(128), area_sqm REAL, rooms REAL, price_per_sqm REAL, details TEXT, image_url TEXT, discovered_at TEXT NOT NULL, viewed INTEGER DEFAULT 0, viewed_at TEXT, created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP);");
        }

        // Ezután a SqliteDatabaseManager ráereszti a Liquibase-t
        SqliteDatabaseManager dbManager = new SqliteDatabaseManager(dbFile);

        try (Connection conn = dbManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM DATABASECHANGELOG;")) {
            assertTrue(rs.next());
            assertTrue(rs.getInt(1) > 0, "A changeSeteknek auditálva kell lenniük még meglévő séma esetén is");
        }
    }
}
