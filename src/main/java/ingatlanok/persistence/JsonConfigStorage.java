package ingatlanok.persistence;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import ingatlanok.common.AppLogger;
import ingatlanok.model.SearchCriteria;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Keresési konfiguráció mentéséért és betöltéséért felelős atomi, szálbiztos JSON tároló.
 */
public class JsonConfigStorage implements ConfigStorage {

    private static final AppLogger LOGGER = AppLogger.getLogger(JsonConfigStorage.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path DEFAULT_CONFIG_PATH = Paths.get("data", "last_config.json");

    private final Path configPath;
    private final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();

    public JsonConfigStorage() {
        this(DEFAULT_CONFIG_PATH);
    }

    public JsonConfigStorage(Path configPath) {
        this.configPath = configPath != null ? configPath.toAbsolutePath().normalize() : DEFAULT_CONFIG_PATH.toAbsolutePath().normalize();
    }

    @Override
    public SearchCriteria getLastCriteria() {
        rwLock.readLock().lock();
        try {
            if (Files.exists(configPath)) {
                try (Reader reader = Files.newBufferedReader(configPath, StandardCharsets.UTF_8)) {
                    SearchCriteria sc = GSON.fromJson(reader, SearchCriteria.class);
                    if (sc != null) {
                        return sc;
                    }
                } catch (Exception e) {
                    LOGGER.warn("Nem sikerült beolvasni a konfigurációt [{}]: {}", configPath, e.getMessage());
                }
            }
        } finally {
            rwLock.readLock().unlock();
        }
        return new SearchCriteria();
    }

    @Override
    public void saveLastCriteria(SearchCriteria criteria) {
        if (criteria == null) {
            return;
        }

        rwLock.writeLock().lock();
        try {
            Path parent = configPath.getParent();
            if (parent != null && !Files.exists(parent)) {
                Files.createDirectories(parent);
            }

            String json = GSON.toJson(criteria);
            Path tempFile = configPath.resolveSibling(configPath.getFileName() + ".tmp");
            Files.writeString(tempFile, json, StandardCharsets.UTF_8);

            try {
                Files.move(tempFile, configPath, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(tempFile, configPath, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            LOGGER.error("Hiba a konfiguráció mentésekor [{}]: {}", configPath, e.getMessage(), e);
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    public Path getConfigPath() {
        return configPath;
    }
}
