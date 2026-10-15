package ingatlanok.persistence;

import ingatlanok.common.AppLogger;
import ingatlanok.common.CityUtils;
import ingatlanok.common.PropertyParser;
import ingatlanok.dto.ListingDto;
import ingatlanok.dto.ListingFilterDto;
import ingatlanok.model.IngatlanItem;
import ingatlanok.model.PriceHistoryEntry;
import ingatlanok.model.PriceInfo;
import ingatlanok.model.PropertyDetails;
import ingatlanok.model.StatusLogEntry;
import org.dizitart.no2.collection.Document;
import org.dizitart.no2.collection.DocumentCursor;
import org.dizitart.no2.collection.NitriteCollection;
import org.dizitart.no2.collection.UpdateOptions;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.dizitart.no2.filters.FluentFilter.where;

/**
 * Beágyazott Nitrite NoSQL Dokumentum Adatbázisra épülő ListingStorage implementáció.
 * Teljesen SQL-mentes, dokumentum-orientált adatszerkezettel, B-Tree indexelt
 * O(1) duplikátumkezeléssel és típusbiztos dinamikus szűréssel.
 */
public class NitriteListingStorage implements ListingStorage {

    private static final AppLogger LOGGER = AppLogger.getLogger(NitriteListingStorage.class);
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final NitriteDatabaseManager dbManager;
    private final AtomicInteger priceHistoryIdSequence = new AtomicInteger(1);
    private final AtomicInteger statusLogIdSequence = new AtomicInteger(1);

    public NitriteListingStorage() {
        this(new NitriteDatabaseManager());
    }

    public NitriteListingStorage(NitriteDatabaseManager dbManager) {
        this.dbManager = dbManager != null ? dbManager : new NitriteDatabaseManager();
        initIdSequences();
    }

    public NitriteDatabaseManager getDbManager() {
        return dbManager;
    }

    private void initIdSequences() {
        NitriteCollection phCol = dbManager.getPriceHistoryCollection();
        int maxPhId = 0;
        for (Document doc : phCol.find()) {
            Number id = doc.get("id", Number.class);
            if (id != null && id.intValue() > maxPhId) {
                maxPhId = id.intValue();
            }
        }
        priceHistoryIdSequence.set(maxPhId + 1);

        NitriteCollection slCol = dbManager.getStatusLogCollection();
        int maxSlId = 0;
        for (Document doc : slCol.find()) {
            Number id = doc.get("id", Number.class);
            if (id != null && id.intValue() > maxSlId) {
                maxSlId = id.intValue();
            }
        }
        statusLogIdSequence.set(maxSlId + 1);
    }

    @Override
    public void saveCityItems(String city, Collection<IngatlanItem> items) {
        if (items == null || items.isEmpty()) {
            return;
        }
        ReentrantReadWriteLock.WriteLock lock = dbManager.getRwLock().writeLock();
        lock.lock();
        try {
            saveItemsInternal(items);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void saveAll(Collection<IngatlanItem> items) {
        if (items == null || items.isEmpty()) {
            return;
        }
        ReentrantReadWriteLock.WriteLock lock = dbManager.getRwLock().writeLock();
        lock.lock();
        try {
            saveItemsInternal(items);
        } finally {
            lock.unlock();
        }
    }

    private void saveItemsInternal(Collection<IngatlanItem> items) {
        String now = LocalDateTime.now().format(DATE_TIME_FORMATTER);
        NitriteCollection propCol = dbManager.getPropertiesCollection();
        NitriteCollection phCol = dbManager.getPriceHistoryCollection();
        NitriteCollection slCol = dbManager.getStatusLogCollection();

        for (IngatlanItem item : items) {
            if (item == null) continue;
            String url = item.getUrl() != null ? item.getUrl().trim() : "";
            if (url.isBlank()) continue;

            String cityName = item.getCity() != null && !item.getCity().isBlank() ? item.getCity().trim() : "Ismeretlen";
            String cityKey = CityUtils.normalizeCityKey(cityName);
            String portal = item.getPortal() != null ? item.getPortal().trim() : "Egyéb";
            String title = item.getTitle() != null ? item.getTitle() : "";
            String discoveredAt = item.getDiscoveredAt() != null ? item.getDiscoveredAt() : now;

            PriceInfo priceInfo = item.getPriceInfo();
            if (priceInfo == null && item.getPrice() != null && !item.getPrice().isBlank()) {
                priceInfo = PropertyParser.parsePrice(item.getPrice());
                item.setPriceInfo(priceInfo);
            }
            String priceRaw = (priceInfo != null && priceInfo.getRawText() != null) ? priceInfo.getRawText() : (item.getPrice() != null ? item.getPrice() : "");
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

            Document existing = propCol.find(where("url").eq(url)).firstOrNull();
            boolean isNew = (existing == null);

            String propertyId;
            Long prevAmount = null;
            String prevStatus = null;
            boolean previouslyViewed = false;
            String previouslyViewedAt = null;

            if (!isNew) {
                propertyId = existing.get("id", String.class);
                Number existingPrice = existing.get("priceAmount", Number.class);
                prevAmount = existingPrice != null ? existingPrice.longValue() : null;
                prevStatus = existing.get("status", String.class);
                Boolean v = existing.get("viewed", Boolean.class);
                previouslyViewed = (v != null && v);
                previouslyViewedAt = existing.get("viewedAt", String.class);
            } else {
                String rawId = item.getId() != null ? item.getId().trim() : "";
                propertyId = (!rawId.isBlank()) ? portal + "_" + rawId : portal + "_" + Integer.toUnsignedString(url.hashCode());
            }

            boolean finalViewed = previouslyViewed || item.isViewed();
            String finalViewedAt = previouslyViewed ? previouslyViewedAt : (item.isViewed() ? (item.getViewedAt() != null ? item.getViewedAt() : now) : null);
            String targetStatus = (item.getStatus() != null && !item.getStatus().isBlank()) ? item.getStatus() : "ACTIVE";

            Long priceDelta = (existing != null) ? existing.get("priceDelta", Long.class) : null;
            Double priceChangePercent = (existing != null) ? existing.get("priceChangePercent", Double.class) : null;
            String lastPriceChangeAt = (existing != null) ? existing.get("lastPriceChangeAt", String.class) : null;

            // Árváltozás és történet kezelése
            if (priceAmount != null && priceAmount > 0) {
                if (isNew) {
                    Document phDoc = Document.createDocument("id", priceHistoryIdSequence.getAndIncrement())
                            .put("propertyId", propertyId)
                            .put("url", url)
                            .put("recordedAt", discoveredAt)
                            .put("priceRaw", priceRaw)
                            .put("priceAmount", priceAmount)
                            .put("priceMillions", priceMillions)
                            .put("pricePerSqm", pricePerSqm)
                            .put("previousPriceAmount", null)
                            .put("priceDelta", 0L)
                            .put("changePercent", 0.0);
                    phCol.insert(phDoc);
                } else if (prevAmount != null && prevAmount > 0 && !prevAmount.equals(priceAmount)) {
                    long delta = priceAmount - prevAmount;
                    double changePct = Math.round(((double) delta / prevAmount * 100.0) * 100.0) / 100.0;
                    priceDelta = delta;
                    priceChangePercent = changePct;
                    lastPriceChangeAt = now;

                    Document phDoc = Document.createDocument("id", priceHistoryIdSequence.getAndIncrement())
                            .put("propertyId", propertyId)
                            .put("url", url)
                            .put("recordedAt", now)
                            .put("priceRaw", priceRaw)
                            .put("priceAmount", priceAmount)
                            .put("priceMillions", priceMillions)
                            .put("pricePerSqm", pricePerSqm)
                            .put("previousPriceAmount", prevAmount)
                            .put("priceDelta", delta)
                            .put("changePercent", changePct);
                    phCol.insert(phDoc);

                    if (delta < 0) {
                        targetStatus = "PRICE_DROP";
                    } else if (delta > 0) {
                        targetStatus = "PRICE_INCREASE";
                    }
                    LOGGER.info("[Árváltozás] {} - Régi: {} Ft -> Új: {} Ft (Delta: {} Ft, {}%)",
                            title, prevAmount, priceAmount, delta, changePct);
                }
            }

            // Státusz napló kezelése
            if (isNew) {
                Document slDoc = Document.createDocument("id", statusLogIdSequence.getAndIncrement())
                        .put("propertyId", propertyId)
                        .put("url", url)
                        .put("status", targetStatus)
                        .put("changedAt", discoveredAt)
                        .put("note", "Kezdeti állapot felfedezve");
                slCol.insert(slDoc);
            } else if (prevStatus != null && !prevStatus.equalsIgnoreCase(targetStatus)) {
                Document slDoc = Document.createDocument("id", statusLogIdSequence.getAndIncrement())
                        .put("propertyId", propertyId)
                        .put("url", url)
                        .put("status", targetStatus)
                        .put("changedAt", now)
                        .put("note", "Státusz változás: " + prevStatus + " -> " + targetStatus);
                slCol.insert(slDoc);
            }

            Document doc = Document.createDocument("url", url)
                    .put("id", propertyId)
                    .put("portal", portal)
                    .put("city", cityName)
                    .put("normalizedCity", cityKey)
                    .put("location", cityName)
                    .put("title", title)
                    .put("price", priceRaw)
                    .put("priceAmount", priceAmount)
                    .put("priceMillions", priceMillions)
                    .put("pricePerSqm", pricePerSqm)
                    .put("details", details)
                    .put("areaSqm", areaSqm)
                    .put("rooms", rooms)
                    .put("imageUrl", item.getImageUrl())
                    .put("discoveredAt", discoveredAt)
                    .put("isNew", item.isNew())
                    .put("viewed", finalViewed)
                    .put("viewedAt", finalViewedAt)
                    .put("status", targetStatus)
                    .put("priceDelta", priceDelta)
                    .put("priceChangePercent", priceChangePercent)
                    .put("lastPriceChangeAt", lastPriceChangeAt)
                    .put("updatedAt", now);

            UpdateOptions updateOptions = new UpdateOptions();
            updateOptions.setInsertIfAbsent(true);
            propCol.update(where("url").eq(url), doc, updateOptions);
        }
    }

    @Override
    public List<IngatlanItem> getItemsByCity(String city) {
        if (city == null || city.isBlank()) {
            return Collections.emptyList();
        }
        String cityKey = CityUtils.normalizeCityKey(city);
        ReentrantReadWriteLock.ReadLock lock = dbManager.getRwLock().readLock();
        lock.lock();
        try {
            NitriteCollection propCol = dbManager.getPropertiesCollection();
            DocumentCursor cursor = propCol.find(where("normalizedCity").eq(cityKey));
            List<IngatlanItem> items = new ArrayList<>();
            for (Document doc : cursor) {
                items.add(documentToItem(doc));
            }
            items.sort(Comparator.comparing(IngatlanItem::getDiscoveredAt, Comparator.nullsLast(Comparator.reverseOrder())));
            return items;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public List<IngatlanItem> getAll() {
        ReentrantReadWriteLock.ReadLock lock = dbManager.getRwLock().readLock();
        lock.lock();
        try {
            NitriteCollection propCol = dbManager.getPropertiesCollection();
            List<IngatlanItem> items = new ArrayList<>();
            for (Document doc : propCol.find()) {
                items.add(documentToItem(doc));
            }
            items.sort(Comparator.comparing(IngatlanItem::getDiscoveredAt, Comparator.nullsLast(Comparator.reverseOrder())));
            return items;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean hasCityDatabase(String city) {
        if (city == null || city.isBlank()) return false;
        String cityKey = CityUtils.normalizeCityKey(city);
        ReentrantReadWriteLock.ReadLock lock = dbManager.getRwLock().readLock();
        lock.lock();
        try {
            NitriteCollection propCol = dbManager.getPropertiesCollection();
            return propCol.find(where("normalizedCity").eq(cityKey)).firstOrNull() != null;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean isNewUrl(String city, String url) {
        if (url == null || url.isBlank()) return false;
        String trimmedUrl = url.trim();
        ReentrantReadWriteLock.ReadLock lock = dbManager.getRwLock().readLock();
        lock.lock();
        try {
            NitriteCollection propCol = dbManager.getPropertiesCollection();
            if (city != null && !city.isBlank()) {
                if (!hasCityDatabase(city)) {
                    return false;
                }
                String cityKey = CityUtils.normalizeCityKey(city);
                return propCol.find(where("url").eq(trimmedUrl).and(where("normalizedCity").eq(cityKey))).firstOrNull() == null;
            }
            return propCol.find(where("url").eq(trimmedUrl)).firstOrNull() == null;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean containsUrl(String url) {
        if (url == null || url.isBlank()) return false;
        String trimmedUrl = url.trim();
        ReentrantReadWriteLock.ReadLock lock = dbManager.getRwLock().readLock();
        lock.lock();
        try {
            return dbManager.getPropertiesCollection().find(where("url").eq(trimmedUrl)).firstOrNull() != null;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean markAsViewed(String url) {
        if (url == null || url.isBlank()) return false;
        String trimmedUrl = url.trim();
        ReentrantReadWriteLock.WriteLock lock = dbManager.getRwLock().writeLock();
        lock.lock();
        try {
            NitriteCollection propCol = dbManager.getPropertiesCollection();
            Document doc = propCol.find(where("url").eq(trimmedUrl)).firstOrNull();
            if (doc != null) {
                doc.put("viewed", true);
                doc.put("viewedAt", LocalDateTime.now().format(DATE_TIME_FORMATTER));
                propCol.update(where("url").eq(trimmedUrl), doc);
                return true;
            }
            return false;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean isViewed(String url) {
        if (url == null || url.isBlank()) return false;
        String trimmedUrl = url.trim();
        ReentrantReadWriteLock.ReadLock lock = dbManager.getRwLock().readLock();
        lock.lock();
        try {
            Document doc = dbManager.getPropertiesCollection().find(where("url").eq(trimmedUrl)).firstOrNull();
            if (doc != null) {
                Boolean v = doc.get("viewed", Boolean.class);
                return v != null && v;
            }
            return false;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public Set<String> getAllUrls() {
        ReentrantReadWriteLock.ReadLock lock = dbManager.getRwLock().readLock();
        lock.lock();
        try {
            Set<String> urls = new HashSet<>();
            for (Document doc : dbManager.getPropertiesCollection().find()) {
                String u = doc.get("url", String.class);
                if (u != null && !u.isBlank()) {
                    urls.add(u);
                }
            }
            return urls;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public List<Map<String, Object>> getTrackedCitiesInfo() {
        ReentrantReadWriteLock.ReadLock lock = dbManager.getRwLock().readLock();
        lock.lock();
        try {
            Map<String, TrackedCityAccumulator> map = new HashMap<>();
            for (Document doc : dbManager.getPropertiesCollection().find()) {
                String city = doc.get("city", String.class);
                String normalized = doc.get("normalizedCity", String.class);
                if (city != null && !city.isBlank()) {
                    String key = (normalized != null && !normalized.isBlank()) ? normalized : CityUtils.normalizeCityKey(city);
                    map.computeIfAbsent(key, k -> new TrackedCityAccumulator(city, key)).increment();
                }
            }
            List<Map<String, Object>> result = new ArrayList<>();
            for (TrackedCityAccumulator acc : map.values()) {
                result.add(Map.of(
                        "name", acc.cityName,
                        "key", acc.cityKey,
                        "count", acc.count
                ));
            }
            return result;
        } finally {
            lock.unlock();
        }
    }

    private static class TrackedCityAccumulator {
        final String cityName;
        final String cityKey;
        int count = 0;

        TrackedCityAccumulator(String cityName, String cityKey) {
            this.cityName = cityName;
            this.cityKey = cityKey;
        }

        void increment() {
            count++;
        }
    }

    @Override
    public void clearCityHistory(String city) {
        if (city == null || city.isBlank()) return;
        String cityKey = CityUtils.normalizeCityKey(city);
        ReentrantReadWriteLock.WriteLock lock = dbManager.getRwLock().writeLock();
        lock.lock();
        try {
            NitriteCollection propCol = dbManager.getPropertiesCollection();
            DocumentCursor cursor = propCol.find(where("normalizedCity").eq(cityKey));
            List<String> urlsToRemove = new ArrayList<>();
            for (Document doc : cursor) {
                String u = doc.get("url", String.class);
                if (u != null) urlsToRemove.add(u);
            }
            propCol.remove(where("normalizedCity").eq(cityKey));

            NitriteCollection phCol = dbManager.getPriceHistoryCollection();
            NitriteCollection slCol = dbManager.getStatusLogCollection();
            for (String u : urlsToRemove) {
                phCol.remove(where("url").eq(u));
                slCol.remove(where("url").eq(u));
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void clearHistory() {
        ReentrantReadWriteLock.WriteLock lock = dbManager.getRwLock().writeLock();
        lock.lock();
        try {
            dbManager.getPropertiesCollection().clear();
            dbManager.getPriceHistoryCollection().clear();
            dbManager.getStatusLogCollection().clear();
            dbManager.getSearchRunsCollection().clear();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public int getTotalCount() {
        ReentrantReadWriteLock.ReadLock lock = dbManager.getRwLock().readLock();
        lock.lock();
        try {
            return (int) dbManager.getPropertiesCollection().size();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public Map<String, Long> countByPortal() {
        ReentrantReadWriteLock.ReadLock lock = dbManager.getRwLock().readLock();
        lock.lock();
        try {
            Map<String, Long> counts = new HashMap<>();
            for (Document doc : dbManager.getPropertiesCollection().find()) {
                String portal = doc.get("portal", String.class);
                if (portal != null && !portal.isBlank()) {
                    counts.put(portal, counts.getOrDefault(portal, 0L) + 1L);
                }
            }
            return counts;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public String getLastRunTime() {
        ReentrantReadWriteLock.ReadLock lock = dbManager.getRwLock().readLock();
        lock.lock();
        try {
            String latest = null;
            for (Document doc : dbManager.getPropertiesCollection().find()) {
                String discovered = doc.get("discoveredAt", String.class);
                if (discovered != null) {
                    if (latest == null || discovered.compareTo(latest) > 0) {
                        latest = discovered;
                    }
                }
            }
            return latest;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public List<PriceHistoryEntry> getPriceHistory(String idOrUrl) {
        if (idOrUrl == null || idOrUrl.isBlank()) return Collections.emptyList();
        String key = idOrUrl.trim();
        ReentrantReadWriteLock.ReadLock lock = dbManager.getRwLock().readLock();
        lock.lock();
        try {
            NitriteCollection phCol = dbManager.getPriceHistoryCollection();
            DocumentCursor cursor = phCol.find(where("url").eq(key).or(where("propertyId").eq(key)));
            List<PriceHistoryEntry> entries = new ArrayList<>();
            for (Document doc : cursor) {
                Number id = doc.get("id", Number.class);
                String propertyId = doc.get("propertyId", String.class);
                String recordedAt = doc.get("recordedAt", String.class);
                String priceRaw = doc.get("priceRaw", String.class);
                Number priceAmount = doc.get("priceAmount", Number.class);
                Number priceMillions = doc.get("priceMillions", Number.class);
                Number pricePerSqm = doc.get("pricePerSqm", Number.class);
                Number prevAmount = doc.get("previousPriceAmount", Number.class);
                Number delta = doc.get("priceDelta", Number.class);
                Number changePct = doc.get("changePercent", Number.class);

                entries.add(new PriceHistoryEntry(
                        id != null ? id.intValue() : 0,
                        propertyId != null ? propertyId : key,
                        recordedAt,
                        priceRaw,
                        priceAmount != null ? priceAmount.longValue() : 0L,
                        priceMillions != null ? priceMillions.doubleValue() : 0.0,
                        pricePerSqm != null ? pricePerSqm.longValue() : null,
                        prevAmount != null ? prevAmount.longValue() : null,
                        delta != null ? delta.longValue() : null,
                        changePct != null ? changePct.doubleValue() : null
                ));
            }
            entries.sort(Comparator.comparingInt(PriceHistoryEntry::id));
            return entries;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public List<StatusLogEntry> getStatusLog(String idOrUrl) {
        if (idOrUrl == null || idOrUrl.isBlank()) return Collections.emptyList();
        String key = idOrUrl.trim();
        ReentrantReadWriteLock.ReadLock lock = dbManager.getRwLock().readLock();
        lock.lock();
        try {
            NitriteCollection slCol = dbManager.getStatusLogCollection();
            DocumentCursor cursor = slCol.find(where("url").eq(key).or(where("propertyId").eq(key)));
            List<StatusLogEntry> entries = new ArrayList<>();
            for (Document doc : cursor) {
                Number id = doc.get("id", Number.class);
                String propertyId = doc.get("propertyId", String.class);
                String status = doc.get("status", String.class);
                String changedAt = doc.get("changedAt", String.class);
                String note = doc.get("note", String.class);

                entries.add(new StatusLogEntry(
                        id != null ? id.intValue() : 0,
                        propertyId != null ? propertyId : key,
                        status,
                        changedAt,
                        note
                ));
            }
            entries.sort(Comparator.comparingInt(StatusLogEntry::id));
            return entries;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public List<IngatlanItem> getDiscountedProperties(String city) {
        ListingFilterDto filter = ListingFilterDto.discounted(city);
        return findByCriteria(filter).stream()
                .map(ListingDto::toItem)
                .collect(Collectors.toList());
    }

    @Override
    public List<ListingDto> findByCriteria(ListingFilterDto filter) {
        ReentrantReadWriteLock.ReadLock lock = dbManager.getRwLock().readLock();
        lock.lock();
        try {
            List<IngatlanItem> all = getAll();
            Stream<IngatlanItem> stream = all.stream();

            if (filter != null) {
                if (filter.cities() != null && !filter.cities().isEmpty()) {
                    Set<String> normalizedFilterCities = filter.cities().stream()
                            .map(CityUtils::normalizeCityKey)
                            .collect(Collectors.toSet());
                    stream = stream.filter(item -> {
                        String k = CityUtils.normalizeCityKey(item.getCity());
                        return normalizedFilterCities.contains(k);
                    });
                }

                if (filter.portals() != null && !filter.portals().isEmpty()) {
                    Set<String> normalizedFilterPortals = filter.portals().stream()
                            .map(String::toLowerCase)
                            .collect(Collectors.toSet());
                    stream = stream.filter(item -> {
                        String p = item.getPortal() != null ? item.getPortal().toLowerCase() : "";
                        return normalizedFilterPortals.contains(p);
                    });
                }

                if (filter.minPriceMillion() != null) {
                    stream = stream.filter(item -> item.getPriceInfo() != null && item.getPriceInfo().getAmountMillion() >= filter.minPriceMillion());
                }

                if (filter.maxPriceMillion() != null) {
                    stream = stream.filter(item -> item.getPriceInfo() != null && item.getPriceInfo().getAmountMillion() <= filter.maxPriceMillion());
                }

                if (filter.minArea() != null) {
                    stream = stream.filter(item -> item.getDetailsInfo() != null && item.getDetailsInfo().getAreaSquareMeters() >= filter.minArea());
                }

                if (filter.maxArea() != null) {
                    stream = stream.filter(item -> item.getDetailsInfo() != null && item.getDetailsInfo().getAreaSquareMeters() <= filter.maxArea());
                }

                if (filter.minRooms() != null) {
                    stream = stream.filter(item -> item.getDetailsInfo() != null && item.getDetailsInfo().getRoomCount() >= filter.minRooms());
                }

                if (filter.maxRooms() != null) {
                    stream = stream.filter(item -> item.getDetailsInfo() != null && item.getDetailsInfo().getRoomCount() <= filter.maxRooms());
                }

                if (filter.excludeViewed()) {
                    stream = stream.filter(item -> !item.isViewed());
                }

                if (filter.onlyDiscounted()) {
                    stream = stream.filter(item -> item.getPriceDelta() != null && item.getPriceDelta() < 0);
                }

                // Rendezés
                String sortBy = filter.sortBy() != null ? filter.sortBy() : "DISCOVERED_DESC";
                Comparator<IngatlanItem> comparator = switch (sortBy.toUpperCase()) {
                    case "PRICE_ASC" -> Comparator.comparingDouble(i -> (i.getPriceInfo() != null ? i.getPriceInfo().getAmountMillion() : Double.MAX_VALUE));
                    case "PRICE_DESC" -> Comparator.comparingDouble((IngatlanItem i) -> (i.getPriceInfo() != null ? i.getPriceInfo().getAmountMillion() : 0.0)).reversed();
                    case "AREA_ASC" -> Comparator.comparingDouble(i -> (i.getDetailsInfo() != null ? i.getDetailsInfo().getAreaSquareMeters() : Double.MAX_VALUE));
                    case "AREA_DESC" -> Comparator.comparingDouble((IngatlanItem i) -> (i.getDetailsInfo() != null ? i.getDetailsInfo().getAreaSquareMeters() : 0.0)).reversed();
                    case "DISCOUNT_DESC" -> Comparator.comparingLong((IngatlanItem i) -> (i.getPriceDelta() != null ? i.getPriceDelta() : 0L));
                    default -> Comparator.comparing(IngatlanItem::getDiscoveredAt, Comparator.nullsLast(Comparator.reverseOrder()));
                };
                stream = stream.sorted(comparator);

                if (filter.offset() != null && filter.offset() > 0) {
                    stream = stream.skip(filter.offset());
                }
                if (filter.limit() != null && filter.limit() > 0) {
                    stream = stream.limit(filter.limit());
                }
            }

            return stream.map(ListingDto::fromItem).collect(Collectors.toList());
        } finally {
            lock.unlock();
        }
    }

    private IngatlanItem documentToItem(Document doc) {
        IngatlanItem item = new IngatlanItem();
        item.setId(doc.get("id", String.class));
        item.setUrl(doc.get("url", String.class));
        item.setPortal(doc.get("portal", String.class));
        item.setTitle(doc.get("title", String.class));
        item.setPrice(doc.get("price", String.class));
        item.setLocation(doc.get("location", String.class));
        item.setCity(doc.get("city", String.class));
        item.setDetails(doc.get("details", String.class));
        item.setDiscoveredAt(doc.get("discoveredAt", String.class));
        Boolean isNew = doc.get("isNew", Boolean.class);
        item.setNew(isNew != null && isNew);
        Boolean viewed = doc.get("viewed", Boolean.class);
        item.setViewed(viewed != null && viewed);
        item.setViewedAt(doc.get("viewedAt", String.class));
        item.setImageUrl(doc.get("imageUrl", String.class));
        item.setStatus(doc.get("status", String.class));

        Number delta = doc.get("priceDelta", Number.class);
        if (delta != null) {
            item.setPriceDelta(delta.longValue());
        }
        Number changePct = doc.get("priceChangePercent", Number.class);
        if (changePct != null) {
            item.setPriceChangePercent(changePct.doubleValue());
        }
        item.setLastPriceChangeAt(doc.get("lastPriceChangeAt", String.class));

        Number priceAmount = doc.get("priceAmount", Number.class);
        Number priceMillions = doc.get("priceMillions", Number.class);
        if (priceAmount != null || priceMillions != null) {
            item.setPriceInfo(new PriceInfo(
                    item.getPrice(),
                    priceAmount != null ? priceAmount.longValue() : 0L,
                    priceMillions != null ? priceMillions.doubleValue() : 0.0
            ));
        }

        Number areaSqm = doc.get("areaSqm", Number.class);
        Number rooms = doc.get("rooms", Number.class);
        Number pricePerSqm = doc.get("pricePerSqm", Number.class);
        if (areaSqm != null || rooms != null || pricePerSqm != null) {
            item.setDetailsInfo(new PropertyDetails(
                    item.getDetails(),
                    areaSqm != null ? areaSqm.doubleValue() : 0.0,
                    rooms != null ? rooms.doubleValue() : 0.0,
                    pricePerSqm != null ? pricePerSqm.longValue() : 0L
            ));
        }

        return item;
    }
}
