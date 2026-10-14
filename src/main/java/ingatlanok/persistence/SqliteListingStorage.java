package ingatlanok.persistence;

import ingatlanok.common.AppLogger;
import ingatlanok.common.CityUtils;
import ingatlanok.common.PropertyParser;
import ingatlanok.model.IngatlanItem;
import ingatlanok.model.PriceInfo;
import ingatlanok.model.PropertyDetails;

import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Relációs SQLite adatbázisra épülő ListingStorage implementáció.
 * Normál táblaszerkezet, tranzakciós batch mentés, indexelt O(1) duplikátumszűrés és WAL mód.
 */
public class SqliteListingStorage implements ListingStorage {

    private static final AppLogger LOGGER = AppLogger.getLogger(SqliteListingStorage.class);
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final SqliteDatabaseManager dbManager;
    private final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();
    private String lastRunTime = null;

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
    public void saveCityItems(String city, Collection<IngatlanItem> items) {
        if (items == null || items.isEmpty()) {
            return;
        }
        rwLock.writeLock().lock();
        try {
            saveItemsInternal(items);
            this.lastRunTime = LocalDateTime.now().format(DATE_TIME_FORMATTER);
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
            this.lastRunTime = LocalDateTime.now().format(DATE_TIME_FORMATTER);
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    private void saveItemsInternal(Collection<IngatlanItem> items) {
        String sql = """
            INSERT INTO properties (
                id, portal, url, title, price_raw, price_amount, price_millions,
                city, city_key, location, area_sqm, rooms, price_per_sqm,
                details, image_url, discovered_at, viewed, viewed_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT(url) DO UPDATE SET
                title = excluded.title,
                price_raw = excluded.price_raw,
                price_amount = excluded.price_amount,
                price_millions = excluded.price_millions,
                city = excluded.city,
                city_key = excluded.city_key,
                location = excluded.location,
                area_sqm = excluded.area_sqm,
                rooms = excluded.rooms,
                price_per_sqm = excluded.price_per_sqm,
                details = excluded.details,
                image_url = CASE WHEN excluded.image_url IS NOT NULL AND excluded.image_url != '' THEN excluded.image_url ELSE properties.image_url END,
                viewed = CASE WHEN properties.viewed = 1 THEN 1 ELSE excluded.viewed END,
                viewed_at = CASE WHEN properties.viewed = 1 THEN properties.viewed_at ELSE excluded.viewed_at END;
        """;

        try (Connection conn = dbManager.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                for (IngatlanItem item : items) {
                    if (item == null || item.getUrl() == null || item.getUrl().isBlank()) {
                        continue;
                    }

                    String url = item.getUrl().trim();
                    String rawId = item.getId();
                    String portal = item.getPortal() != null ? item.getPortal() : "Egyéb";
                    String id;
                    if (rawId != null && !rawId.isBlank() && !"N/A".equalsIgnoreCase(rawId)) {
                        id = portal + "_" + rawId + "_" + Integer.toHexString(url.hashCode());
                    } else {
                        id = UUID.nameUUIDFromBytes(url.getBytes(StandardCharsets.UTF_8)).toString();
                    }
                    String title = item.getTitle() != null ? item.getTitle() : portal + " hirdetés";

                    // Ár kinyerése és strukturálása
                    String priceRaw = item.getPrice();
                    Long priceAmount = null;
                    Double priceMillions = null;
                    PriceInfo pi = item.getPriceInfo();
                    if (pi == null && priceRaw != null) {
                        pi = PropertyParser.parsePrice(priceRaw);
                    }
                    if (pi != null) {
                        priceAmount = pi.getAmountHuf();
                        priceMillions = pi.getAmountMillion();
                    }

                    String city = item.getCity() != null && !item.getCity().isBlank() ? item.getCity() : "Debrecen";
                    String cityKey = CityUtils.normalizeCityKey(city);
                    String location = item.getLocation() != null ? item.getLocation() : city;

                    // Terület és szobaszám
                    Double areaSqm = null;
                    Double rooms = null;
                    Long pricePerSqm = null;
                    PropertyDetails pd = item.getDetailsInfo();
                    if (pd == null && item.getDetails() != null) {
                        pd = PropertyParser.parseDetails(item.getDetails(), pi);
                    }
                    if (pd != null) {
                        areaSqm = pd.getAreaSquareMeters();
                        rooms = pd.getRoomCount();
                        pricePerSqm = pd.getPricePerSquareMeter();
                    }

                    String details = item.getDetails() != null ? item.getDetails() : "";
                    String imageUrl = item.getImageUrl() != null ? item.getImageUrl() : "";
                    String discoveredAt = item.getDiscoveredAt() != null && !item.getDiscoveredAt().isBlank()
                            ? item.getDiscoveredAt()
                            : LocalDateTime.now().format(DATE_TIME_FORMATTER);
                    int viewed = item.isViewed() ? 1 : 0;
                    String viewedAt = item.getViewedAt();

                    ps.setString(1, id);
                    ps.setString(2, portal);
                    ps.setString(3, item.getUrl().trim());
                    ps.setString(4, title);
                    ps.setString(5, priceRaw);
                    if (priceAmount != null) ps.setLong(6, priceAmount); else ps.setNull(6, Types.BIGINT);
                    if (priceMillions != null) ps.setDouble(7, priceMillions); else ps.setNull(7, Types.REAL);
                    ps.setString(8, city);
                    ps.setString(9, cityKey);
                    ps.setString(10, location);
                    if (areaSqm != null) ps.setDouble(11, areaSqm); else ps.setNull(11, Types.REAL);
                    if (rooms != null) ps.setDouble(12, rooms); else ps.setNull(12, Types.REAL);
                    if (pricePerSqm != null) ps.setLong(13, pricePerSqm); else ps.setNull(13, Types.BIGINT);
                    ps.setString(14, details);
                    ps.setString(15, imageUrl);
                    ps.setString(16, discoveredAt);
                    ps.setInt(17, viewed);
                    ps.setString(18, viewedAt);

                    ps.addBatch();
                }
                ps.executeBatch();
            }
            conn.commit();
        } catch (SQLException e) {
            LOGGER.error("Hiba a hirdetések SQLite mentésekor: {}", e.getMessage(), e);
            throw new RuntimeException("Adatbázis mentési hiba: " + e.getMessage(), e);
        }
    }

    @Override
    public List<IngatlanItem> getItemsByCity(String city) {
        if (city == null || city.isBlank()) {
            return Collections.emptyList();
        }
        rwLock.readLock().lock();
        try {
            String cityKey = CityUtils.normalizeCityKey(city);
            String sql = "SELECT * FROM properties WHERE city_key = ? ORDER BY discovered_at DESC;";
            try (Connection conn = dbManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, cityKey);
                try (ResultSet rs = ps.executeQuery()) {
                    List<IngatlanItem> list = new ArrayList<>();
                    while (rs.next()) {
                        list.add(mapResultSetToItem(rs));
                    }
                    return list;
                }
            } catch (SQLException e) {
                LOGGER.error("Hiba a hirdetések lekérésekor város szerint [{}]: {}", city, e.getMessage(), e);
                return Collections.emptyList();
            }
        } finally {
            rwLock.readLock().unlock();
        }
    }

    @Override
    public List<IngatlanItem> getAll() {
        rwLock.readLock().lock();
        try {
            String sql = "SELECT * FROM properties ORDER BY discovered_at DESC;";
            try (Connection conn = dbManager.getConnection();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                List<IngatlanItem> list = new ArrayList<>();
                while (rs.next()) {
                    list.add(mapResultSetToItem(rs));
                }
                return list;
            } catch (SQLException e) {
                LOGGER.error("Hiba az összes hirdetés lekérésekor az SQLite-ból: {}", e.getMessage(), e);
                return Collections.emptyList();
            }
        } finally {
            rwLock.readLock().unlock();
        }
    }

    @Override
    public boolean hasCityDatabase(String city) {
        if (city == null || city.isBlank()) {
            return false;
        }
        rwLock.readLock().lock();
        try {
            String cityKey = CityUtils.normalizeCityKey(city);
            String sql = "SELECT 1 FROM properties WHERE city_key = ? LIMIT 1;";
            try (Connection conn = dbManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, cityKey);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next();
                }
            } catch (SQLException e) {
                LOGGER.error("Hiba a város adatbázis létezésének ellenőrzésekor [{}]: {}", city, e.getMessage(), e);
                return false;
            }
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
                String sql = "SELECT 1 FROM properties WHERE url = ? AND city_key = ? LIMIT 1;";
                try (Connection conn = dbManager.getConnection();
                     PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, url.trim());
                    ps.setString(2, cityKey);
                    try (ResultSet rs = ps.executeQuery()) {
                        return !rs.next();
                    }
                }
            } else {
                return !containsUrl(url);
            }
        } catch (SQLException e) {
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
            String sql = "SELECT 1 FROM properties WHERE url = ? LIMIT 1;";
            try (Connection conn = dbManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, url.trim());
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next();
                }
            } catch (SQLException e) {
                LOGGER.error("Hiba az URL létezésének ellenőrzésekor [{}]: {}", url, e.getMessage(), e);
                return false;
            }
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
            String sql = "UPDATE properties SET viewed = 1, viewed_at = ? WHERE url = ?;";
            try (Connection conn = dbManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, now);
                ps.setString(2, url.trim());
                int rows = ps.executeUpdate();
                return rows > 0;
            } catch (SQLException e) {
                LOGGER.error("Hiba a hirdetés megtekintettre állításakor [{}]: {}", url, e.getMessage(), e);
                return false;
            }
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    @Override
    public boolean isViewed(String url) {
        if (url == null || url.isBlank()) {
            return false;
        }
        rwLock.readLock().lock();
        try {
            String sql = "SELECT viewed FROM properties WHERE url = ? LIMIT 1;";
            try (Connection conn = dbManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, url.trim());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return rs.getInt("viewed") == 1;
                    }
                    return false;
                }
            } catch (SQLException e) {
                LOGGER.error("Hiba a megtekintettség lekérdezésekor [{}]: {}", url, e.getMessage(), e);
                return false;
            }
        } finally {
            rwLock.readLock().unlock();
        }
    }

    @Override
    public Set<String> getAllUrls() {
        rwLock.readLock().lock();
        try {
            String sql = "SELECT url FROM properties;";
            try (Connection conn = dbManager.getConnection();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                Set<String> set = new LinkedHashSet<>();
                while (rs.next()) {
                    set.add(rs.getString("url"));
                }
                return set;
            } catch (SQLException e) {
                LOGGER.error("Hiba az ismert URL-ek lekérésekor: {}", e.getMessage(), e);
                return Collections.emptySet();
            }
        } finally {
            rwLock.readLock().unlock();
        }
    }

    @Override
    public List<Map<String, Object>> getTrackedCitiesInfo() {
        rwLock.readLock().lock();
        try {
            String sql = "SELECT city, city_key, COUNT(*) AS cnt, MAX(discovered_at) AS last_seen FROM properties GROUP BY city_key, city ORDER BY cnt DESC;";
            try (Connection conn = dbManager.getConnection();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                List<Map<String, Object>> result = new ArrayList<>();
                while (rs.next()) {
                    String city = rs.getString("city");
                    String cityKey = rs.getString("city_key");
                    int cnt = rs.getInt("cnt");
                    String lastSeen = rs.getString("last_seen");
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("key", cityKey);
                    map.put("cityKey", cityKey);
                    map.put("name", city);
                    map.put("city", city);
                    map.put("itemCount", cnt);
                    map.put("count", cnt);
                    map.put("lastSeen", lastSeen != null ? lastSeen : "");
                    map.put("lastModified", lastSeen != null ? lastSeen : "");
                    result.add(map);
                }
                result.sort((a, b) -> ((String) a.get("name")).compareToIgnoreCase((String) b.get("name")));
                return result;
            } catch (SQLException e) {
                LOGGER.error("Hiba a követett városok statisztikájának lekérésekor: {}", e.getMessage(), e);
                return Collections.emptyList();
            }
        } finally {
            rwLock.readLock().unlock();
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
            String sql = "DELETE FROM properties WHERE city_key = ?;";
            try (Connection conn = dbManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, cityKey);
                int deleted = ps.executeUpdate();
                LOGGER.info("[SQLite] Város előzményei törölve: {} ({} db rekord)", city, deleted);
            } catch (SQLException e) {
                LOGGER.error("Hiba a város előzményeinek törlésekor [{}]: {}", city, e.getMessage(), e);
            }
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    @Override
    public void clearHistory() {
        rwLock.writeLock().lock();
        try {
            String sql = "DELETE FROM properties;";
            try (Connection conn = dbManager.getConnection();
                 Statement stmt = conn.createStatement()) {
                int deleted = stmt.executeUpdate(sql);
                LOGGER.info("[SQLite] Teljes hirdetésadatbázis törölve ({} db rekord)", deleted);
            } catch (SQLException e) {
                LOGGER.error("Hiba az adatbázis törlésekor: {}", e.getMessage(), e);
            }
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    @Override
    public int getTotalCount() {
        rwLock.readLock().lock();
        try {
            String sql = "SELECT COUNT(*) FROM properties;";
            try (Connection conn = dbManager.getConnection();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
                return 0;
            } catch (SQLException e) {
                LOGGER.error("Hiba az összes hirdetésszám lekérdezésekor: {}", e.getMessage(), e);
                return 0;
            }
        } finally {
            rwLock.readLock().unlock();
        }
    }

    @Override
    public String getLastRunTime() {
        if (lastRunTime != null) {
            return lastRunTime;
        }
        rwLock.readLock().lock();
        try {
            String sql = "SELECT MAX(discovered_at) FROM properties;";
            try (Connection conn = dbManager.getConnection();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                if (rs.next()) {
                    return rs.getString(1);
                }
                return null;
            } catch (SQLException e) {
                return null;
            }
        } finally {
            rwLock.readLock().unlock();
        }
    }

    /**
     * Keresési futás rögzítése a search_runs táblába.
     */
    public void recordSearchRun(String executedAt, int totalFound, int newFound, String criteriaJson, String portalStatsJson) {
        String sql = "INSERT INTO search_runs (executed_at, total_found, new_found, criteria_json, portal_stats_json) VALUES (?, ?, ?, ?, ?);";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, executedAt != null ? executedAt : LocalDateTime.now().format(DATE_TIME_FORMATTER));
            ps.setInt(2, totalFound);
            ps.setInt(3, newFound);
            ps.setString(4, criteriaJson);
            ps.setString(5, portalStatsJson);
            ps.executeUpdate();
        } catch (SQLException e) {
            LOGGER.error("Hiba a keresési futás naplózásakor az SQLite-ba: {}", e.getMessage());
        }
    }

    private IngatlanItem mapResultSetToItem(ResultSet rs) throws SQLException {
        IngatlanItem item = new IngatlanItem();
        item.setId(rs.getString("id"));
        item.setPortal(rs.getString("portal"));
        item.setUrl(rs.getString("url"));
        item.setTitle(rs.getString("title"));

        String priceRaw = rs.getString("price_raw");
        long priceAmount = rs.getLong("price_amount");
        boolean hasPriceAmount = !rs.wasNull();
        double priceMillions = rs.getDouble("price_millions");
        boolean hasPriceMillions = !rs.wasNull();

        item.setPrice(priceRaw);
        if (priceRaw != null || hasPriceAmount || hasPriceMillions) {
            item.setPriceInfo(new PriceInfo(priceRaw, hasPriceAmount ? priceAmount : null, hasPriceMillions ? priceMillions : null));
        }

        item.setCity(rs.getString("city"));
        item.setLocation(rs.getString("location"));

        String details = rs.getString("details");
        double areaSqm = rs.getDouble("area_sqm");
        boolean hasAreaSqm = !rs.wasNull();
        double rooms = rs.getDouble("rooms");
        boolean hasRooms = !rs.wasNull();
        long pricePerSqm = rs.getLong("price_per_sqm");
        boolean hasPricePerSqm = !rs.wasNull();

        item.setDetails(details);
        if (details != null || hasAreaSqm || hasRooms || hasPricePerSqm) {
            item.setDetailsInfo(new PropertyDetails(
                    details,
                    hasAreaSqm ? areaSqm : null,
                    hasRooms ? rooms : null,
                    hasPricePerSqm ? pricePerSqm : null
            ));
        }

        item.setImageUrl(rs.getString("image_url"));
        item.setDiscoveredAt(rs.getString("discovered_at"));
        item.setViewed(rs.getInt("viewed") == 1);
        item.setViewedAt(rs.getString("viewed_at"));
        item.setNew(false);

        return item;
    }
}
