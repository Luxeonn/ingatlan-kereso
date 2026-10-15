package ingatlanok.persistence;

import ingatlanok.common.AppLogger;
import ingatlanok.common.CityUtils;
import ingatlanok.common.PropertyParser;
import ingatlanok.common.SlugUtils;
import ingatlanok.dto.ListingDto;
import ingatlanok.dto.ListingFilterDto;
import ingatlanok.dto.PriceHistoryDto;
import ingatlanok.dto.StatusLogDto;
import ingatlanok.model.IngatlanItem;
import ingatlanok.model.PriceHistoryEntry;
import ingatlanok.model.PriceInfo;
import ingatlanok.model.PropertyDetails;
import ingatlanok.model.StatusLogEntry;
import ingatlanok.persistence.builder.ListingCriteriaQueryBuilder;
import ingatlanok.persistence.dao.ListingDao;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 3. Normálformás (3NF) relációs SQLite adatbázisra épülő ListingStorage implementáció.
 * Típusbiztos Jdbi v3 SqlObject DAO és ListingCriteriaQueryBuilder segítségével működik,
 * garantálva a Clean Architecture-t és elkerülve a nyers SQL stringeket.
 */
public class SqliteListingStorage implements ListingStorage {

    private static final AppLogger LOGGER = AppLogger.getLogger(SqliteListingStorage.class);
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final SqliteDatabaseManager dbManager;
    private final ListingCriteriaQueryBuilder queryBuilder = new ListingCriteriaQueryBuilder();
    private final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();

    public SqliteListingStorage() {
        this(new SqliteDatabaseManager());
    }

    public SqliteListingStorage(SqliteDatabaseManager dbManager) {
        this.dbManager = dbManager != null ? dbManager : new SqliteDatabaseManager();
    }

    public SqliteDatabaseManager getDbManager() {
        return dbManager;
    }

    @Override
    public List<ListingDto> findByCriteria(ListingFilterDto filter) {
        rwLock.readLock().lock();
        try {
            return dbManager.getJdbi().withHandle(handle -> queryBuilder.execute(handle, filter));
        } catch (Exception e) {
            LOGGER.error("Hiba a Criteria Builder lekérdezés futtatásakor: {}", e.getMessage(), e);
            return Collections.emptyList();
        } finally {
            rwLock.readLock().unlock();
        }
    }

    @Override
    public void saveCityItems(String city, Collection<IngatlanItem> items) {
        if (items == null || items.isEmpty()) {
            return;
        }
        rwLock.writeLock().lock();
        try {
            saveItemsInternal(items);
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    @Override
    public void saveAll(Collection<IngatlanItem> items) {
        if (items == null || items.isEmpty()) {
            return;
        }
        rwLock.writeLock().lock();
        try {
            saveItemsInternal(items);
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    private void saveItemsInternal(Collection<IngatlanItem> items) {
        String now = LocalDateTime.now().format(DATE_TIME_FORMATTER);

        try {
            dbManager.getJdbi().useTransaction(handle -> {
                ListingDao dao = handle.attach(ListingDao.class);

                for (IngatlanItem item : items) {
                    if (item == null) continue;
                    String url = item.getUrl() != null ? item.getUrl().trim() : "";
                    if (url.isBlank()) continue;

                    String portalId = resolvePortalId(item.getPortal());
                    String portalName = resolvePortalName(portalId, item.getPortal());
                    dao.upsertPortal(portalId, portalName);

                    String cityName = item.getCity() != null && !item.getCity().isBlank() ? item.getCity().trim() : "Ismeretlen";
                    String cityKey = CityUtils.normalizeCityKey(cityName);
                    dao.upsertLocation(cityKey, cityName, cityKey);

                    String rawId = item.getId() != null ? item.getId().trim() : "";
                    String propertyId = (!rawId.isBlank()) ? portalId + "_" + rawId : portalId + "_" + Math.abs(url.hashCode());
                    String title = item.getTitle() != null ? item.getTitle() : "";
                    String discoveredAt = item.getDiscoveredAt() != null ? item.getDiscoveredAt() : now;
                    int viewed = item.isViewed() ? 1 : 0;
                    String viewedAt = item.getViewedAt();

                    PriceInfo priceInfo = item.getPriceInfo();
                    if (priceInfo == null && item.getPrice() != null && !item.getPrice().isBlank()) {
                        priceInfo = PropertyParser.parsePrice(item.getPrice());
                        item.setPriceInfo(priceInfo);
                    }
                    String priceRaw = (priceInfo != null && priceInfo.getRawText() != null) ? priceInfo.getRawText() : item.getPrice();
                    Long priceAmount = (priceInfo != null) ? priceInfo.getAmountHuf() : null;
                    Double priceMillions = (priceInfo != null) ? priceInfo.getAmountMillion() : null;

                    PropertyDetails detailsInfo = item.getDetailsInfo();
                    if (detailsInfo == null && item.getDetails() != null && !item.getDetails().isBlank()) {
                        detailsInfo = PropertyParser.parseDetails(item.getDetails(), priceInfo);
                        item.setDetailsInfo(detailsInfo);
                    }
                    Double areaSqm = (detailsInfo != null) ? detailsInfo.getAreaSquareMeters() : null;
                    Double rooms = (detailsInfo != null) ? detailsInfo.getRoomCount() : null;
                    Long pricePerSqm = (detailsInfo != null) ? detailsInfo.getPricePerSquareMeter() : null;
                    String details = item.getDetails() != null ? item.getDetails() : "";

                    // Meglévő ingatlan vizsgálata
                    Optional<ListingDao.ExistingPropertyInfo> existingOpt = dao.findExistingPropertyInfo(url);
                    boolean isNew = existingOpt.isEmpty();
                    Long prevAmount = null;
                    String prevStatus = null;
                    int historyCount = 0;

                    if (!isNew) {
                        ListingDao.ExistingPropertyInfo existing = existingOpt.get();
                        propertyId = existing.id();
                        prevAmount = existing.priceAmount();
                        prevStatus = existing.status();
                        historyCount = existing.historyCount();
                    }

                    String targetStatus = (item.getStatus() != null && !item.getStatus().isBlank()) ? item.getStatus() : "ACTIVE";
                    if (!isNew && priceAmount != null && prevAmount != null && !priceAmount.equals(prevAmount)) {
                        long delta = priceAmount - prevAmount;
                        double changePct = Math.round(((double) delta / prevAmount) * 10000.0) / 100.0;
                        targetStatus = (delta < 0) ? "PRICE_DROP" : "PRICE_INCREASE";
                        item.setPriceDelta(delta);
                        item.setPriceChangePercent(changePct);
                        item.setLastPriceChangeAt(now);
                    }

                    // 1. Property és attribútumok mentése (FK integritás miatt ezt kötelező először menteni!)
                    dao.upsertProperty(propertyId, portalId, cityKey, rawId, url, title, targetStatus, discoveredAt, viewed, viewedAt);
                    dao.upsertAttributes(url, priceRaw, priceAmount, priceMillions, pricePerSqm, areaSqm, rooms, details);

                    String imageUrl = item.getImageUrl();
                    if (imageUrl != null && !imageUrl.isBlank()) {
                        dao.insertPrimaryImage(url, imageUrl);
                    }

                    // 2. Ártörténet és státusznapló mentése
                    if (isNew) {
                        if (priceAmount != null) {
                            dao.insertPriceHistory(propertyId, discoveredAt, priceRaw, priceAmount, priceMillions != null ? priceMillions : 0.0, pricePerSqm, null, 0L, 0.0);
                        }
                        dao.insertStatusLog(propertyId, targetStatus, discoveredAt, "Kezdeti állapot rögzítése");
                    } else {
                        if (priceAmount != null && prevAmount != null && !priceAmount.equals(prevAmount)) {
                            long delta = priceAmount - prevAmount;
                            double changePct = Math.round(((double) delta / prevAmount) * 10000.0) / 100.0;
                            dao.insertPriceHistory(propertyId, now, priceRaw, priceAmount, priceMillions != null ? priceMillions : 0.0, pricePerSqm, prevAmount, delta, changePct);
                            dao.insertStatusLog(propertyId, targetStatus, now, "Árváltozás: " + prevAmount + " Ft -> " + priceAmount + " Ft (" + (delta > 0 ? "+" : "") + delta + " Ft, " + changePct + "%)");
                        } else if (historyCount == 0 && priceAmount != null) {
                            dao.insertPriceHistory(propertyId, discoveredAt, priceRaw, priceAmount, priceMillions != null ? priceMillions : 0.0, pricePerSqm, null, 0L, 0.0);
                        }

                        if (targetStatus != null && prevStatus != null && !targetStatus.equalsIgnoreCase(prevStatus) && (priceAmount == null || priceAmount.equals(prevAmount))) {
                            dao.insertStatusLog(propertyId, targetStatus, now, "Státusz változás: " + prevStatus + " -> " + targetStatus);
                        }
                    }
                }
            });
        } catch (Exception e) {
            LOGGER.error("Hiba a hirdetések Jdbi SQLite mentésekor: {}", e.getMessage(), e);
            throw new RuntimeException("Adatbázis mentési hiba: " + e.getMessage(), e);
        }
    }

    private static String resolvePortalId(String rawPortal) {
        if (rawPortal == null || rawPortal.isBlank()) {
            return "egyeb";
        }
        String p = rawPortal.trim().toLowerCase(Locale.ROOT);
        if (p.contains("duna") || p.equals("dh")) return "dh";
        if (p.contains("otthon") || p.equals("oc")) return "oc";
        if (p.contains("jofogas") || p.contains("jófogás")) return "jofogas";
        if (p.contains("bazar") || p.contains("bazár")) return "ingatlanbazar";
        if (p.contains("koltozz") || p.contains("költözz")) return "koltozzbe";
        if (p.contains("ingatlan.com") || p.contains("ingatlancom")) return "ingatlancom";
        return SlugUtils.toSlug(p);
    }

    private static String resolvePortalName(String portalId, String rawPortal) {
        return switch (portalId) {
            case "dh" -> "Duna House";
            case "oc" -> "Otthon Centrum";
            case "jofogas" -> "Jófogás";
            case "ingatlanbazar" -> "Ingatlanbazár";
            case "koltozzbe" -> "Költözzbe";
            case "ingatlancom" -> "Ingatlan.com";
            default -> (rawPortal != null && !rawPortal.isBlank()) ? rawPortal : "Egyéb";
        };
    }

    @Override
    public List<IngatlanItem> getItemsByCity(String city) {
        if (city == null || city.isBlank()) {
            return Collections.emptyList();
        }
        List<ListingDto> dtos = findByCriteria(ListingFilterDto.byCity(city));
        return dtos.stream().map(ListingDto::toItem).toList();
    }

    @Override
    public List<IngatlanItem> getAll() {
        List<ListingDto> dtos = findByCriteria(ListingFilterDto.all());
        return dtos.stream().map(ListingDto::toItem).toList();
    }

    @Override
    public boolean hasCityDatabase(String city) {
        if (city == null || city.isBlank()) {
            return false;
        }
        rwLock.readLock().lock();
        try {
            String cityKey = CityUtils.normalizeCityKey(city);
            return dbManager.getJdbi().withExtension(ListingDao.class, dao -> dao.hasCityDatabase(cityKey)).isPresent();
        } catch (Exception e) {
            LOGGER.error("Hiba a város adatbázis létezésének ellenőrzésekor [{}]: {}", city, e.getMessage(), e);
            return false;
        } finally {
            rwLock.readLock().unlock();
        }
    }

    @Override
    public boolean isNewUrl(String city, String url) {
        if (url == null || url.isBlank()) {
            return false;
        }
        rwLock.readLock().lock();
        try {
            if (city != null && !city.isBlank()) {
                if (!hasCityDatabase(city)) {
                    return false;
                }
                String cityKey = CityUtils.normalizeCityKey(city);
                return dbManager.getJdbi().withExtension(ListingDao.class, dao -> dao.containsCityUrl(cityKey, url.trim())).isEmpty();
            } else {
                return !containsUrl(url);
            }
        } catch (Exception e) {
            LOGGER.error("Hiba az új URL ellenőrzésekor: {}", e.getMessage(), e);
            return true;
        } finally {
            rwLock.readLock().unlock();
        }
    }

    @Override
    public boolean containsUrl(String url) {
        if (url == null || url.isBlank()) {
            return false;
        }
        rwLock.readLock().lock();
        try {
            return dbManager.getJdbi().withExtension(ListingDao.class, dao -> dao.containsUrl(url.trim())).isPresent();
        } catch (Exception e) {
            LOGGER.error("Hiba az URL létezésének ellenőrzésekor [{}]: {}", url, e.getMessage(), e);
            return false;
        } finally {
            rwLock.readLock().unlock();
        }
    }

    @Override
    public boolean isViewed(String url) {
        if (url == null || url.isBlank()) {
            return false;
        }
        rwLock.readLock().lock();
        try {
            return dbManager.getJdbi().withExtension(ListingDao.class, dao -> dao.isViewed(url.trim()))
                    .map(v -> v == 1)
                    .orElse(false);
        } catch (Exception e) {
            LOGGER.error("Hiba a hirdetés megtekintettségének lekérdezésekor [{}]: {}", url, e.getMessage(), e);
            return false;
        } finally {
            rwLock.readLock().unlock();
        }
    }

    @Override
    public boolean markAsViewed(String url) {
        if (url == null || url.isBlank()) {
            return false;
        }
        rwLock.writeLock().lock();
        try {
            String now = LocalDateTime.now().format(DATE_TIME_FORMATTER);
            int updated = dbManager.getJdbi().withExtension(ListingDao.class, dao -> dao.markAsViewed(url.trim(), now));
            return updated > 0;
        } catch (Exception e) {
            LOGGER.error("Hiba a hirdetés megtekintettként jelölésekor [{}]: {}", url, e.getMessage(), e);
            return false;
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    @Override
    public Set<String> getAllUrls() {
        rwLock.readLock().lock();
        try {
            List<String> urls = dbManager.getJdbi().withExtension(ListingDao.class, ListingDao::getAllUrls);
            return new HashSet<>(urls);
        } catch (Exception e) {
            LOGGER.error("Hiba az összes URL lekérdezésekor: {}", e.getMessage(), e);
            return Collections.emptySet();
        } finally {
            rwLock.readLock().unlock();
        }
    }

    public List<String> getUrlsByCity(String city) {
        if (city == null || city.isBlank()) {
            return Collections.emptyList();
        }
        rwLock.readLock().lock();
        try {
            String cityKey = CityUtils.normalizeCityKey(city);
            return dbManager.getJdbi().withExtension(ListingDao.class, dao -> dao.getUrlsByCity(cityKey));
        } catch (Exception e) {
            LOGGER.error("Hiba a városi URL-ek lekérdezésekor [{}]: {}", city, e.getMessage(), e);
            return Collections.emptyList();
        } finally {
            rwLock.readLock().unlock();
        }
    }

    @Override
    public int getTotalCount() {
        rwLock.readLock().lock();
        try {
            return dbManager.getJdbi().withExtension(ListingDao.class, ListingDao::getTotalCount);
        } catch (Exception e) {
            LOGGER.error("Hiba az összes hirdetésszám lekérésekor: {}", e.getMessage(), e);
            return 0;
        } finally {
            rwLock.readLock().unlock();
        }
    }

    @Override
    public Map<String, Long> countByPortal() {
        rwLock.readLock().lock();
        try {
            List<ListingDao.PortalCountRecord> records = dbManager.getJdbi().withExtension(ListingDao.class, ListingDao::countByPortal);
            Map<String, Long> result = new LinkedHashMap<>();
            for (ListingDao.PortalCountRecord r : records) {
                if (r.portal() != null && !r.portal().isBlank()) {
                    result.put(r.portal().trim().toLowerCase(Locale.ROOT), (long) r.count());
                }
            }
            return result;
        } catch (Exception e) {
            LOGGER.error("Hiba a portálonkénti számláláskor: {}", e.getMessage(), e);
            return Collections.emptyMap();
        } finally {
            rwLock.readLock().unlock();
        }
    }

    @Override
    public String getLastRunTime() {
        rwLock.readLock().lock();
        try {
            return dbManager.getJdbi().withExtension(ListingDao.class, ListingDao::getLastRunTime).orElse(null);
        } catch (Exception e) {
            LOGGER.error("Hiba az utolsó futási idő lekérésekor: {}", e.getMessage(), e);
            return null;
        } finally {
            rwLock.readLock().unlock();
        }
    }

    @Override
    public List<Map<String, Object>> getTrackedCitiesInfo() {
        rwLock.readLock().lock();
        try {
            List<ListingDao.TrackedCityRecord> records = dbManager.getJdbi().withExtension(ListingDao.class, ListingDao::getTrackedCities);
            List<Map<String, Object>> list = new ArrayList<>();
            for (ListingDao.TrackedCityRecord r : records) {
                Map<String, Object> map = new LinkedHashMap<>();
                map.put("key", r.cityKey());
                map.put("cityKey", r.cityKey());
                map.put("name", r.cityName());
                map.put("city", r.cityName());
                map.put("cityName", r.cityName());
                map.put("count", r.count());
                map.put("file", "sqlite:properties");
                list.add(map);
            }
            return list;
        } catch (Exception e) {
            LOGGER.error("Hiba a nyilvántartott városok lekérésekor: {}", e.getMessage(), e);
            return Collections.emptyList();
        } finally {
            rwLock.readLock().unlock();
        }
    }

    @Override
    public void clearHistory() {
        rwLock.writeLock().lock();
        try {
            dbManager.getJdbi().useTransaction(handle -> {
                ListingDao dao = handle.attach(ListingDao.class);
                dao.clearPriceHistory();
                dao.clearStatusLog();
                dao.clearPropertyImages();
                dao.clearPropertyAttributes();
                dao.clearProperties();
                dao.clearSearchRuns();
            });
        } catch (Exception e) {
            LOGGER.error("Hiba a relációs adatbázis törlésekor: {}", e.getMessage(), e);
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    @Override
    public void clearCityHistory(String city) {
        if (city == null || city.isBlank()) {
            return;
        }
        rwLock.writeLock().lock();
        try {
            String cityKey = CityUtils.normalizeCityKey(city);
            dbManager.getJdbi().useHandle(handle -> {
                handle.execute("DELETE FROM property_price_history WHERE property_id IN (SELECT id FROM properties WHERE location_id = ?);", cityKey);
                handle.execute("DELETE FROM property_status_log WHERE property_id IN (SELECT id FROM properties WHERE location_id = ?);", cityKey);
                handle.execute("DELETE FROM property_images WHERE property_id IN (SELECT id FROM properties WHERE location_id = ?);", cityKey);
                handle.execute("DELETE FROM property_attributes WHERE property_id IN (SELECT id FROM properties WHERE location_id = ?);", cityKey);
                handle.execute("DELETE FROM properties WHERE location_id = ?;", cityKey);
            });
        } catch (Exception e) {
            LOGGER.error("Hiba a városi előzmények törlésekor [{}]: {}", city, e.getMessage(), e);
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    public void recordSearchRun(String executedAt, int totalFound, int newFound, String criteriaJson, String portalStatsJson) {
        try {
            dbManager.getJdbi().useExtension(ListingDao.class, dao -> dao.recordSearchRun(executedAt, totalFound, newFound, criteriaJson, portalStatsJson));
        } catch (Exception e) {
            LOGGER.error("Hiba a keresési futás naplózásakor az SQLite-ba: {}", e.getMessage());
        }
    }

    @Override
    public List<PriceHistoryEntry> getPriceHistory(String idOrUrl) {
        if (idOrUrl == null || idOrUrl.isBlank()) {
            return Collections.emptyList();
        }
        rwLock.readLock().lock();
        try {
            List<PriceHistoryDto> list = dbManager.getJdbi().withExtension(ListingDao.class, dao -> dao.getPriceHistory(idOrUrl.trim()));
            return list.stream().map(PriceHistoryDto::toModel).toList();
        } catch (Exception e) {
            LOGGER.error("Hiba az ártörténet lekérésekor [{}]: {}", idOrUrl, e.getMessage(), e);
            return Collections.emptyList();
        } finally {
            rwLock.readLock().unlock();
        }
    }

    @Override
    public List<StatusLogEntry> getStatusLog(String idOrUrl) {
        if (idOrUrl == null || idOrUrl.isBlank()) {
            return Collections.emptyList();
        }
        rwLock.readLock().lock();
        try {
            List<StatusLogDto> list = dbManager.getJdbi().withExtension(ListingDao.class, dao -> dao.getStatusLog(idOrUrl.trim()));
            return list.stream().map(StatusLogDto::toModel).toList();
        } catch (Exception e) {
            LOGGER.error("Hiba a státusznapló lekérésekor [{}]: {}", idOrUrl, e.getMessage(), e);
            return Collections.emptyList();
        } finally {
            rwLock.readLock().unlock();
        }
    }

    @Override
    public List<IngatlanItem> getDiscountedProperties(String city) {
        List<ListingDto> dtos = findByCriteria(ListingFilterDto.discounted(city));
        return dtos.stream().map(ListingDto::toItem).toList();
    }
}
