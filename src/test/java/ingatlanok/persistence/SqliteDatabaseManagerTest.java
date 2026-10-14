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
}
