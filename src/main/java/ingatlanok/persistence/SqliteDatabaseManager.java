package ingatlanok.persistence;

import ingatlanok.common.AppLogger;

import liquibase.Contexts;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;

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
 * WAL (Write-Ahead Logging) mód, szigorú integritás, busy_timeout és Liquibase XML migráció.
 */
public class SqliteDatabaseManager {

    private static final AppLogger LOGGER = AppLogger.getLogger(SqliteDatabaseManager.class);
    private static final String CHANGELOG_PATH = "db/changelog/db.changelog-master.xml";
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
            }

            // Liquibase deklaratív XML sémamigráció
            runLiquibaseMigrations();

            LOGGER.info("[SQLite] Adatbázis és séma sikeresen inicializálva: {}", dbPath);
        } catch (Exception e) {
            LOGGER.error("Hiba az SQLite adatbázis inicializálásakor [{}]: {}", dbPath, e.getMessage(), e);
            throw new RuntimeException("SQLite inicializálási hiba: " + e.getMessage(), e);
        }
    }

    public void runLiquibaseMigrations() {
        try (Connection conn = getConnection()) {
            Database database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(conn));
            try (Liquibase liquibase = new Liquibase(CHANGELOG_PATH, new ClassLoaderResourceAccessor(), database)) {
                liquibase.update(new Contexts());
            }
        } catch (Exception e) {
            LOGGER.error("Hiba a Liquibase migráció futtatásakor [{}]: {}", dbPath, e.getMessage(), e);
            throw new RuntimeException("Liquibase migrációs hiba: " + e.getMessage(), e);
        }
    }
}
