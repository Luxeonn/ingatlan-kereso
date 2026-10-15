package ingatlanok.persistence;

import ingatlanok.common.AppLogger;
import org.dizitart.no2.Nitrite;
import org.dizitart.no2.collection.NitriteCollection;
import org.dizitart.no2.index.IndexOptions;
import org.dizitart.no2.index.IndexType;
import org.dizitart.no2.mvstore.MVStoreModule;

import java.io.Closeable;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Beágyazott NoSQL Dokumentum Adatbázis menedzser (Nitrite DB + MVStore).
 * Teljesen kiváltja a relációs SQL táblákat, automatikus B-Tree indexeléssel
 * és típusbiztos NoSQL dokumentum kollekciókkal.
 */
public class NitriteDatabaseManager implements Closeable {

    private static final AppLogger LOGGER = AppLogger.getLogger(NitriteDatabaseManager.class);
    private static final String DEFAULT_DB_PATH = "data/ingatlanok_nosql.db";

    public static final String COLLECTION_PROPERTIES = "properties";
    public static final String COLLECTION_PRICE_HISTORY = "price_history";
    public static final String COLLECTION_STATUS_LOG = "status_log";
    public static final String COLLECTION_CONFIG = "config";
    public static final String COLLECTION_SEARCH_RUNS = "search_runs";

    private static final Map<String, NitriteDatabaseManager> INSTANCE_CACHE = new ConcurrentHashMap<>();

    public static synchronized NitriteDatabaseManager getInstance(Path databasePath) {
        Path targetPath = databasePath != null ? databasePath : Paths.get(DEFAULT_DB_PATH);
        String key = targetPath.toAbsolutePath().normalize().toString();
        NitriteDatabaseManager existing = INSTANCE_CACHE.get(key);
        if (existing != null && !existing.isClosed()) {
            return existing;
        }
        NitriteDatabaseManager created = new NitriteDatabaseManager(targetPath);
        INSTANCE_CACHE.put(key, created);
        return created;
    }

    private final Path databasePath;
    private final Nitrite nitrite;
    private final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();

    public NitriteDatabaseManager() {
        this(Paths.get(DEFAULT_DB_PATH));
    }

    public NitriteDatabaseManager(Path databasePath) {
        this.databasePath = databasePath != null ? databasePath : Paths.get(DEFAULT_DB_PATH);
        String key = this.databasePath.toAbsolutePath().normalize().toString();
        INSTANCE_CACHE.put(key, this);

        File parentDir = this.databasePath.toFile().getParentFile();
        if (parentDir != null && !parentDir.exists() && !parentDir.mkdirs()) {
            LOGGER.warn("Nem sikerült létrehozni a könyvtárat: {}", parentDir.getAbsolutePath());
        }

        MVStoreModule storeModule = MVStoreModule.withConfig()
                .filePath(this.databasePath.toAbsolutePath().toString())
                .compress(true)
                .build();

        this.nitrite = Nitrite.builder()
                .loadModule(storeModule)
                .openOrCreate();

        initCollectionsAndIndexes();
        LOGGER.info("[NoSQL] Nitrite dokumentum adatbázis és MVStore inicializálva: {}", this.databasePath);
    }

    private void initCollectionsAndIndexes() {
        rwLock.writeLock().lock();
        try {
            NitriteCollection propCol = nitrite.getCollection(COLLECTION_PROPERTIES);
            createIndexIfNotExists(propCol, IndexOptions.indexOptions(IndexType.UNIQUE), "url");
            createIndexIfNotExists(propCol, IndexOptions.indexOptions(IndexType.NON_UNIQUE), "city");
            createIndexIfNotExists(propCol, IndexOptions.indexOptions(IndexType.NON_UNIQUE), "normalizedCity");
            createIndexIfNotExists(propCol, IndexOptions.indexOptions(IndexType.NON_UNIQUE), "portal");
            createIndexIfNotExists(propCol, IndexOptions.indexOptions(IndexType.NON_UNIQUE), "discoveredAt");

            NitriteCollection phCol = nitrite.getCollection(COLLECTION_PRICE_HISTORY);
            createIndexIfNotExists(phCol, IndexOptions.indexOptions(IndexType.NON_UNIQUE), "url");
            createIndexIfNotExists(phCol, IndexOptions.indexOptions(IndexType.NON_UNIQUE), "propertyId");

            NitriteCollection slCol = nitrite.getCollection(COLLECTION_STATUS_LOG);
            createIndexIfNotExists(slCol, IndexOptions.indexOptions(IndexType.NON_UNIQUE), "url");
            createIndexIfNotExists(slCol, IndexOptions.indexOptions(IndexType.NON_UNIQUE), "propertyId");

            NitriteCollection cfgCol = nitrite.getCollection(COLLECTION_CONFIG);
            createIndexIfNotExists(cfgCol, IndexOptions.indexOptions(IndexType.UNIQUE), "key");
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    private void createIndexIfNotExists(NitriteCollection col, IndexOptions options, String... fields) {
        try {
            col.createIndex(options, fields);
        } catch (Exception ignored) {
            // Már létező index vagy inicializálási figyelmeztetés
        }
    }

    public Nitrite getNitrite() {
        return nitrite;
    }

    public NitriteCollection getPropertiesCollection() {
        return nitrite.getCollection(COLLECTION_PROPERTIES);
    }

    public NitriteCollection getPriceHistoryCollection() {
        return nitrite.getCollection(COLLECTION_PRICE_HISTORY);
    }

    public NitriteCollection getStatusLogCollection() {
        return nitrite.getCollection(COLLECTION_STATUS_LOG);
    }

    public NitriteCollection getConfigCollection() {
        return nitrite.getCollection(COLLECTION_CONFIG);
    }

    public NitriteCollection getSearchRunsCollection() {
        return nitrite.getCollection(COLLECTION_SEARCH_RUNS);
    }

    public Path getDatabasePath() {
        return databasePath;
    }

    public ReentrantReadWriteLock getRwLock() {
        return rwLock;
    }

    public boolean isClosed() {
        return nitrite == null || nitrite.isClosed();
    }

    @Override
    public void close() {
        rwLock.writeLock().lock();
        try {
            INSTANCE_CACHE.remove(databasePath.toAbsolutePath().normalize().toString());
            if (nitrite != null && !nitrite.isClosed()) {
                nitrite.close();
                LOGGER.info("[NoSQL] Nitrite dokumentum adatbázis lezárva: {}", databasePath);
            }
        } finally {
            rwLock.writeLock().unlock();
        }
    }
}
