package ingatlanok.persistence;

import ingatlanok.model.IngatlanItem;
import ingatlanok.model.SearchCriteria;

import java.util.*;

/**
 * Singleton Facade az Ingatlanhirdetés és Konfiguráció perzisztenciához.
 * Delegál a moduláris {@link ListingStorage} és {@link ConfigStorage} implementációkhoz.
 */
public class IngatlanRepository implements ListingStorage, ConfigStorage {

    private static final IngatlanRepository INSTANCE = new IngatlanRepository();

    private final ListingStorage listingStorage;
    private final ConfigStorage configStorage;

    private IngatlanRepository() {
        this(createDefaultListingStorage(), createDefaultConfigStorage());
    }

    public IngatlanRepository(ListingStorage listingStorage, ConfigStorage configStorage) {
        this.listingStorage = listingStorage != null ? listingStorage : createDefaultListingStorage();
        this.configStorage = configStorage != null ? configStorage : createDefaultConfigStorage();
    }

    private static boolean isJsonStoragePreferred() {
        String env = System.getenv("STORAGE_TYPE");
        if (env != null && env.equalsIgnoreCase("json")) {
            return true;
        }
        String prop = System.getProperty("storage.type");
        return prop != null && prop.equalsIgnoreCase("json");
    }

    private static ListingStorage createDefaultListingStorage() {
        if (isJsonStoragePreferred()) {
            return new JsonFileListingStorage();
        }
        SqliteDatabaseManager dbManager = new SqliteDatabaseManager();
        new SqliteMigrationService(dbManager).migrateIfEmpty();
        return new SqliteListingStorage(dbManager);
    }

    private static ConfigStorage createDefaultConfigStorage() {
        if (isJsonStoragePreferred()) {
            return new JsonConfigStorage();
        }
        SqliteDatabaseManager dbManager = new SqliteDatabaseManager();
        return new SqliteConfigStorage(dbManager);
    }

    public static IngatlanRepository getInstance() {
        return INSTANCE;
    }

    public ListingStorage getListingStorage() {
        return listingStorage;
    }

    public ConfigStorage getConfigStorage() {
        return configStorage;
    }

    @Override
    public boolean hasCityDatabase(String city) {
        return listingStorage.hasCityDatabase(city);
    }

    @Override
    public boolean isNewUrl(String city, String url) {
        return listingStorage.isNewUrl(city, url);
    }

    @Override
    public boolean containsUrl(String url) {
        return listingStorage.containsUrl(url);
    }

    @Override
    public void saveCityItems(String city, Collection<IngatlanItem> items) {
        listingStorage.saveCityItems(city, items);
    }

    @Override
    public void saveAll(Collection<IngatlanItem> items) {
        listingStorage.saveAll(items);
    }

    @Override
    public boolean markAsViewed(String url) {
        return listingStorage.markAsViewed(url);
    }

    @Override
    public boolean isViewed(String url) {
        return listingStorage.isViewed(url);
    }

    @Override
    public Set<String> getAllUrls() {
        return listingStorage.getAllUrls();
    }

    @Override
    public List<IngatlanItem> getAll() {
        return listingStorage.getAll();
    }

    @Override
    public List<IngatlanItem> getItemsByCity(String city) {
        return listingStorage.getItemsByCity(city);
    }

    @Override
    public List<Map<String, Object>> getTrackedCitiesInfo() {
        return listingStorage.getTrackedCitiesInfo();
    }

    @Override
    public void clearCityHistory(String city) {
        listingStorage.clearCityHistory(city);
    }

    @Override
    public void clearHistory() {
        listingStorage.clearHistory();
    }

    @Override
    public SearchCriteria getLastCriteria() {
        return configStorage.getLastCriteria();
    }

    @Override
    public void saveLastCriteria(SearchCriteria criteria) {
        configStorage.saveLastCriteria(criteria);
    }

    @Override
    public int getTotalCount() {
        return listingStorage.getTotalCount();
    }

    @Override
    public String getLastRunTime() {
        return listingStorage.getLastRunTime();
    }
}
