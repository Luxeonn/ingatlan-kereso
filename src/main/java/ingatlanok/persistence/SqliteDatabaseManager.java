package ingatlanok.persistence;

import ingatlanok.common.AppLogger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Szálbiztos SQLite kapcsolat- és sémakezelő modul.
 * WAL (Write-Ahead Logging) mód, szigorú integritás, busy_timeout és automatikus táblalétrehozás.
 */
public class SqliteDatabaseManager {

    private static final AppLogger LOGGER = AppLogger.getLogger(SqliteDatabaseManager.class);
    private final Path dbPath;
    private final String jdbcUrl;

    public SqliteDatabaseManager() {
        this(Paths.get("data", "ingatlanok.db"));
    }

    public SqliteDatabaseManager(Path dbPath) {
        this.dbPath = dbPath != null ? dbPath.toAbsolutePath().normalize() : Paths.get("data", "ingatlanok.db").toAbsolutePath().normalize();
        this.jdbcUrl = "jdbc:sqlite:" + this.dbPath.toString();
        init();
    }

    public Path getDbPath() {
        return dbPath;
    }

    public String getJdbcUrl() {
        return jdbcUrl;
    }

    public Connection getConnection() throws SQLException {
        Connection conn = DriverManager.getConnection(jdbcUrl);
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA foreign_keys = ON;");
            stmt.execute("PRAGMA busy_timeout = 5000;");
        }
        return conn;
    }

    private void init() {
        try {
            Path parent = dbPath.getParent();
            if (parent != null && !Files.exists(parent)) {
                Files.createDirectories(parent);
            }

            try (Connection conn = DriverManager.getConnection(jdbcUrl);
                 Statement stmt = conn.createStatement()) {

                // SQLite optimalizációk és konkurens olvasás előkészítése
                stmt.execute("PRAGMA journal_mode = WAL;");
                stmt.execute("PRAGMA synchronous = NORMAL;");
                stmt.execute("PRAGMA foreign_keys = ON;");
                stmt.execute("PRAGMA busy_timeout = 5000;");

                // 1. Hirdetések főtáblája
                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS properties (
                        id VARCHAR(64) PRIMARY KEY,
                        portal VARCHAR(32) NOT NULL,
                        url VARCHAR(512) NOT NULL UNIQUE,
                        title VARCHAR(255) NOT NULL,
                        price_raw VARCHAR(64),
                        price_amount BIGINT,
                        price_millions REAL,
                        city VARCHAR(64) NOT NULL,
                        city_key VARCHAR(64) NOT NULL,
                        location VARCHAR(128),
                        area_sqm REAL,
                        rooms REAL,
                        price_per_sqm REAL,
                        details TEXT,
                        image_url TEXT,
                        discovered_at TEXT NOT NULL,
                        viewed INTEGER DEFAULT 0,
                        viewed_at TEXT,
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                    );
                """);

                // Indexek a villámgyors szűréshez és duplikátumszűréshez
                stmt.execute("CREATE INDEX IF NOT EXISTS idx_properties_url ON properties(url);");
                stmt.execute("CREATE INDEX IF NOT EXISTS idx_properties_city_key ON properties(city_key);");
                stmt.execute("CREATE INDEX IF NOT EXISTS idx_properties_viewed ON properties(viewed);");
                stmt.execute("CREATE INDEX IF NOT EXISTS idx_properties_discovered ON properties(discovered_at);");

                // 2. Keresési beállítások tábla
                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS search_config (
                        config_key VARCHAR(64) PRIMARY KEY,
                        config_value TEXT NOT NULL,
                        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                    );
                """);

                // 3. Keresési futási napló (audit és statisztika)
                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS search_runs (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        executed_at TEXT NOT NULL,
                        total_found INTEGER DEFAULT 0,
                        new_found INTEGER DEFAULT 0,
                        criteria_json TEXT,
                        portal_stats_json TEXT
                    );
                """);

                LOGGER.info("[SQLite] Adatbázis és séma sikeresen inicializálva: {}", dbPath);
            }
        } catch (IOException | SQLException e) {
            LOGGER.error("Hiba az SQLite adatbázis inicializálásakor [{}]: {}", dbPath, e.getMessage(), e);
            throw new RuntimeException("SQLite inicializálási hiba: " + e.getMessage(), e);
        }
    }
}
