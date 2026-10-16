package ingatlanok.persistence;

import ingatlanok.common.CityUtils;
import ingatlanok.dto.ListingDto;
import ingatlanok.dto.ListingFilterDto;
import ingatlanok.model.IngatlanItem;
import ingatlanok.model.PriceHistoryEntry;
import ingatlanok.model.PriceInfo;
import ingatlanok.model.StatusLogEntry;
import ingatlanok.persistence.mapper.NitriteDocumentMapper;
import ingatlanok.persistence.service.ListingFilterService;
import ingatlanok.persistence.service.PriceHistoryTracker;
import ingatlanok.persistence.service.PriceHistoryTracker.PriceDeltaCalculation;
import ingatlanok.persistence.service.StatusLogTracker;
import ingatlanok.persistence.service.StatusLogTracker.StatusDecision;
import org.dizitart.no2.collection.Document;
import org.dizitart.no2.collection.NitriteCollection;
import org.dizitart.no2.collection.UpdateOptions;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.dizitart.no2.filters.FluentFilter.where;

/**
 * NoSQL Nitrite DB alapú ListingStorage implementáció.
 */
public class NitriteListingStorage implements ListingStorage {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private volatile NitriteDatabaseManager dbManager;
    private final PriceHistoryTracker priceHistoryTracker;
    private final StatusLogTracker statusLogTracker;
    private final ListingFilterService listingFilterService;
    private final AtomicInteger priceHistoryIdSequence = new AtomicInteger(1);
    private final AtomicInteger statusLogIdSequence = new AtomicInteger(1);

    public NitriteListingStorage() {
        this(null);
    }

    public NitriteListingStorage(NitriteDatabaseManager dbManager) {
        this(dbManager, null, null, null);
    }

    public NitriteListingStorage(NitriteDatabaseManager db, PriceHistoryTracker pht, StatusLogTracker slt, ListingFilterService lfs) {
        this.dbManager = db != null ? db : NitriteDatabaseManager.getInstance(null);
        this.priceHistoryTracker = pht != null ? pht : new PriceHistoryTracker();
        this.statusLogTracker = slt != null ? slt : new StatusLogTracker();
        this.listingFilterService = lfs != null ? lfs : new ListingFilterService();
        initIdSequences();
    }

    public NitriteDatabaseManager getDbManager() {
        if (dbManager == null || dbManager.isClosed()) {
            dbManager = NitriteDatabaseManager.getInstance(dbManager != null ? dbManager.getDatabasePath() : null);
        }
        return dbManager;
    }

    private <T> T withReadLock(Supplier<T> action) {
        var lock = getDbManager().getRwLock().readLock();
        lock.lock();
        try {
            return action.get();
        } finally {
            lock.unlock();
        }
    }

    private <T> T withWriteLock(Supplier<T> action) {
        var lock = getDbManager().getRwLock().writeLock();
        lock.lock();
        try {
            return action.get();
        } finally {
            lock.unlock();
        }
    }

    private void initIdSequences() {
        priceHistoryIdSequence.set(getMaxId(getDbManager().getPriceHistoryCollection()) + 1);
        statusLogIdSequence.set(getMaxId(getDbManager().getStatusLogCollection()) + 1);
    }

    private int getMaxId(NitriteCollection col) {
        int max = 0;
        for (Document d : col.find()) {
            Number id = d.get("id", Number.class);
            if (id != null && id.intValue() > max) {
                max = id.intValue();
            }
        }
        return max;
    }

    @Override
    public void saveCityItems(String city, Collection<IngatlanItem> items) {
        saveAll(items);
    }

    @Override
    public void saveAll(Collection<IngatlanItem> items) {
        if (items == null || items.isEmpty()) {
            return;
        }
        withWriteLock(() -> {
            String now = LocalDateTime.now().format(DATE_TIME_FORMATTER);
            NitriteCollection propCol = getDbManager().getPropertiesCollection();
            NitriteCollection phCol = getDbManager().getPriceHistoryCollection();
            NitriteCollection slCol = getDbManager().getStatusLogCollection();
            for (IngatlanItem item : items) {
                if (item != null && item.getUrl() != null && !item.getUrl().trim().isBlank()) {
                    saveItem(item, now, propCol, phCol, slCol);
                }
            }
            return null;
        });
    }

    private void saveItem(IngatlanItem item, String now, NitriteCollection propCol, NitriteCollection phCol, NitriteCollection slCol) {
        NitriteDocumentMapper.normalizeItem(item);
        String url = item.getUrl().trim();
        Document existing = propCol.find(where("url").eq(url)).firstOrNull();
        boolean isNew = (existing == null);
        String portal = item.getPortal() != null ? item.getPortal().trim() : "Egyéb";
        String propId = isNew ? ((item.getId() != null && !item.getId().isBlank()) ? portal + "_" + item.getId().trim() : portal + "_" + Integer.toUnsignedString(url.hashCode())) : existing.get("id", String.class);
        Long prevAmount = !isNew && existing.get("priceAmount", Number.class) != null ? existing.get("priceAmount", Number.class).longValue() : null;

        PriceInfo pi = item.getPriceInfo();
        Long priceAmount = pi != null ? pi.getAmountHuf() : null;
        PriceDeltaCalculation pc = priceHistoryTracker.calculate(isNew, prevAmount, priceAmount,
                existing != null ? existing.get("priceDelta", Long.class) : null, existing != null ? existing.get("priceChangePercent", Double.class) : null,
                existing != null ? existing.get("lastPriceChangeAt", String.class) : null, item.getStatus(), item.getTitle() != null ? item.getTitle() : "", now);

        if (priceAmount != null && priceAmount > 0) {
            String raw = pi != null && pi.getRawText() != null ? pi.getRawText() : (item.getPrice() != null ? item.getPrice() : "");
            Long perSqm = item.getDetailsInfo() != null ? item.getDetailsInfo().getPricePerSquareMeter() : null;
            if (pc.isNewPriceRecord()) {
                phCol.insert(NitriteDocumentMapper.toPriceHistoryDocument(priceHistoryIdSequence.getAndIncrement(), propId, url, item.getDiscoveredAt() != null ? item.getDiscoveredAt() : now, raw, priceAmount, pi.getAmountMillion(), perSqm, null, 0L, 0.0));
            } else if (pc.priceChanged()) {
                phCol.insert(NitriteDocumentMapper.toPriceHistoryDocument(priceHistoryIdSequence.getAndIncrement(), propId, url, now, raw, priceAmount, pi.getAmountMillion(), perSqm, pc.previousPriceAmount(), pc.delta(), pc.changePercent()));
            }
        }
        StatusDecision sd = statusLogTracker.evaluate(isNew, !isNew ? existing.get("status", String.class) : null, pc.resolvedStatus(), isNew && item.getDiscoveredAt() != null ? item.getDiscoveredAt() : now);
        if (sd.shouldRecord()) {
            slCol.insert(NitriteDocumentMapper.toStatusLogDocument(statusLogIdSequence.getAndIncrement(), propId, url, sd.status(), sd.changedAt(), sd.note()));
        }

        UpdateOptions uo = new UpdateOptions();
        uo.setInsertIfAbsent(true);
        propCol.update(where("url").eq(url), NitriteDocumentMapper.toPropertyDocument(item, propId, existing, pc, now), uo);
    }

    private List<IngatlanItem> findAndSort(org.dizitart.no2.filters.Filter filter) {
        return withReadLock(() -> {
            List<IngatlanItem> items = new ArrayList<>();
            for (Document doc : (filter != null ? getDbManager().getPropertiesCollection().find(filter) : getDbManager().getPropertiesCollection().find())) {
                items.add(NitriteDocumentMapper.toItem(doc));
            }
            items.sort(Comparator.comparing(IngatlanItem::getDiscoveredAt, Comparator.nullsLast(Comparator.reverseOrder())));
            return items;
        });
    }

    @Override
    public List<IngatlanItem> getItemsByCity(String city) {
        return (city == null || city.isBlank()) ? Collections.emptyList() : findAndSort(where("normalizedCity").eq(CityUtils.normalizeCityKey(city)));
    }

    @Override
    public List<IngatlanItem> getAll() {
        return findAndSort(null);
    }

    @Override
    public boolean hasCityDatabase(String city) {
        return city != null && !city.isBlank() && withReadLock(() -> getDbManager().getPropertiesCollection().find(where("normalizedCity").eq(CityUtils.normalizeCityKey(city))).firstOrNull() != null);
    }

    @Override
    public boolean isNewUrl(String city, String url) {
        if (url == null || url.isBlank()) {
            return false;
        }
        String trimmed = url.trim();
        return withReadLock(() -> (city != null && !city.isBlank()) ? (hasCityDatabase(city) && getDbManager().getPropertiesCollection().find(where("url").eq(trimmed).and(where("normalizedCity").eq(CityUtils.normalizeCityKey(city)))).firstOrNull() == null) : getDbManager().getPropertiesCollection().find(where("url").eq(trimmed)).firstOrNull() == null);
    }

    @Override
    public boolean containsUrl(String url) {
        return url != null && !url.isBlank() && withReadLock(() -> getDbManager().getPropertiesCollection().find(where("url").eq(url.trim())).firstOrNull() != null);
    }

    @Override
    public boolean markAsViewed(String url) {
        if (url == null || url.isBlank()) {
            return false;
        }
        String t = url.trim();
        return withWriteLock(() -> {
            Document doc = getDbManager().getPropertiesCollection().find(where("url").eq(t)).firstOrNull();
            if (doc == null) {
                return false;
            }
            doc.put("viewed", true).put("viewedAt", LocalDateTime.now().format(DATE_TIME_FORMATTER));
            getDbManager().getPropertiesCollection().update(where("url").eq(t), doc);
            return true;
        });
    }

    @Override
    public boolean isViewed(String url) {
        return url != null && !url.isBlank() && withReadLock(() -> Optional.ofNullable(getDbManager().getPropertiesCollection().find(where("url").eq(url.trim())).firstOrNull()).map(d -> Boolean.TRUE.equals(d.get("viewed", Boolean.class))).orElse(false));
    }

    @Override
    public Set<String> getAllUrls() {
        return withReadLock(() -> {
            Set<String> urls = new HashSet<>();
            for (Document doc : getDbManager().getPropertiesCollection().find()) {
                String u = doc.get("url", String.class);
                if (u != null && !u.isBlank()) {
                    urls.add(u);
                }
            }
            return urls;
        });
    }

    @Override
    public List<Map<String, Object>> getTrackedCitiesInfo() {
        return withReadLock(() -> {
            Map<String, String> names = new HashMap<>();
            Map<String, Long> counts = new HashMap<>();
            for (Document doc : getDbManager().getPropertiesCollection().find()) {
                String city = doc.get("city", String.class);
                if (city != null && !city.isBlank()) {
                    String norm = doc.get("normalizedCity", String.class);
                    String k = norm != null && !norm.isBlank() ? norm : CityUtils.normalizeCityKey(city);
                    names.putIfAbsent(k, city);
                    counts.put(k, counts.getOrDefault(k, 0L) + 1L);
                }
            }
            return counts.entrySet().stream().map(e -> Map.<String, Object>of("name", names.get(e.getKey()), "key", e.getKey(), "count", e.getValue())).toList();
        });
    }

    @Override
    public void clearCityHistory(String city) {
        if (city == null || city.isBlank()) {
            return;
        }
        String cityKey = CityUtils.normalizeCityKey(city);
        withWriteLock(() -> {
            for (Document doc : getDbManager().getPropertiesCollection().find(where("normalizedCity").eq(cityKey))) {
                String u = doc.get("url", String.class);
                if (u != null) {
                    getDbManager().getPriceHistoryCollection().remove(where("url").eq(u));
                    getDbManager().getStatusLogCollection().remove(where("url").eq(u));
                }
            }
            getDbManager().getPropertiesCollection().remove(where("normalizedCity").eq(cityKey));
            return null;
        });
    }

    @Override
    public void clearHistory() {
        withWriteLock(() -> {
            getDbManager().getPropertiesCollection().clear();
            getDbManager().getPriceHistoryCollection().clear();
            getDbManager().getStatusLogCollection().clear();
            getDbManager().getSearchRunsCollection().clear();
            return null;
        });
    }

    @Override
    public int getTotalCount() {
        return withReadLock(() -> (int) getDbManager().getPropertiesCollection().size());
    }

    @Override
    public Map<String, Long> countByPortal() {
        return withReadLock(() -> {
            Map<String, Long> map = new HashMap<>();
            for (Document doc : getDbManager().getPropertiesCollection().find()) {
                String p = doc.get("portal", String.class);
                if (p != null && !p.isBlank()) {
                    map.put(p, map.getOrDefault(p, 0L) + 1L);
                }
            }
            return map;
        });
    }

    @Override
    public String getLastRunTime() {
        return withReadLock(() -> {
            String latest = null;
            for (Document doc : getDbManager().getPropertiesCollection().find()) {
                String disc = doc.get("discoveredAt", String.class);
                if (disc != null && (latest == null || disc.compareTo(latest) > 0)) {
                    latest = disc;
                }
            }
            return latest;
        });
    }

    @Override
    public List<PriceHistoryEntry> getPriceHistory(String idOrUrl) {
        if (idOrUrl == null || idOrUrl.isBlank()) {
            return Collections.emptyList();
        }
        String k = idOrUrl.trim();
        return withReadLock(() -> {
            List<PriceHistoryEntry> list = new ArrayList<>();
            for (Document d : getDbManager().getPriceHistoryCollection().find(where("url").eq(k).or(where("propertyId").eq(k)))) {
                list.add(NitriteDocumentMapper.toPriceHistoryEntry(d, k));
            }
            list.sort(Comparator.comparingInt(PriceHistoryEntry::id));
            return list;
        });
    }

    @Override
    public List<StatusLogEntry> getStatusLog(String idOrUrl) {
        if (idOrUrl == null || idOrUrl.isBlank()) {
            return Collections.emptyList();
        }
        String k = idOrUrl.trim();
        return withReadLock(() -> {
            List<StatusLogEntry> list = new ArrayList<>();
            for (Document d : getDbManager().getStatusLogCollection().find(where("url").eq(k).or(where("propertyId").eq(k)))) {
                list.add(NitriteDocumentMapper.toStatusLogEntry(d, k));
            }
            list.sort(Comparator.comparingInt(StatusLogEntry::id));
            return list;
        });
    }

    @Override
    public List<IngatlanItem> getDiscountedProperties(String city) {
        return listingFilterService.filterItems(getAll(), ListingFilterDto.discounted(city));
    }

    @Override
    public List<ListingDto> findByCriteria(ListingFilterDto filter) {
        return withReadLock(() -> listingFilterService.filter(getAll(), filter));
    }
}
