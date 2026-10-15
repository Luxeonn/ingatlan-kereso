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

    private static ListingStorage createDefaultListingStorage() {
        NitriteDatabaseManager dbManager = NitriteDatabaseManager.getInstance(null);
        new NoSqlMigrationService(dbManager).migrateIfEmpty();
        return new NitriteListingStorage(dbManager);
    }

    private static ConfigStorage createDefaultConfigStorage() {
        NitriteDatabaseManager dbManager = NitriteDatabaseManager.getInstance(null);
        return new NitriteConfigStorage(dbManager);
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
    public Map<String, Long> countByPortal() {
        return listingStorage.countByPortal();
    }

    @Override
    public String getLastRunTime() {
        return listingStorage.getLastRunTime();
    }

    @Override
    public List<ingatlanok.model.PriceHistoryEntry> getPriceHistory(String idOrUrl) {
        return listingStorage.getPriceHistory(idOrUrl);
    }

    @Override
    public List<ingatlanok.model.StatusLogEntry> getStatusLog(String idOrUrl) {
        return listingStorage.getStatusLog(idOrUrl);
    }

    @Override
    public List<IngatlanItem> getDiscountedProperties(String city) {
        return listingStorage.getDiscountedProperties(city);
    }

    @Override
    public List<ingatlanok.dto.ListingDto> findByCriteria(ingatlanok.dto.ListingFilterDto filter) {
        return listingStorage.findByCriteria(filter);
    }
}
