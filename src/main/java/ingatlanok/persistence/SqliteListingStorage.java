package ingatlanok.persistence;

import ingatlanok.common.AppLogger;
import ingatlanok.common.CityUtils;
import ingatlanok.common.PropertyParser;
import ingatlanok.common.SlugUtils;
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
 * 3. Normálformás (3NF) relációs SQLite adatbázisra épülő ListingStorage implementáció.
 * Entitás-kapcsolatok (portals, locations, properties, property_attributes, property_images),
 * idegen kulcsok, tranzakciós batch mentés és indexelt JOIN lekérdezések.
 */
public class SqliteListingStorage implements ListingStorage {

    private static final AppLogger LOGGER = AppLogger.getLogger(SqliteListingStorage.class);
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final String SELECT_3NF_BASE = """
        SELECT p.id, po.name AS portal, p.url, p.title,
               loc.name AS city, loc.normalized_slug AS city_key,
               p.discovered_at, p.viewed, p.viewed_at,
               pa.price_raw, pa.price_amount, pa.price_millions, pa.price_per_sqm,
               pa.area_sqm, pa.rooms, pa.raw_description AS details,
               pi.image_url
        FROM properties p
        JOIN portals po ON p.portal_id = po.id
        JOIN locations loc ON p.location_id = loc.id
        LEFT JOIN property_attributes pa ON p.id = pa.property_id
        LEFT JOIN property_images pi ON p.id = pi.property_id AND pi.is_primary = 1
    """;

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
        String portalSql = "INSERT OR IGNORE INTO portals (id, name, base_url, color_hex, is_active, rate_limit_ms) VALUES (?, ?, '', '#6c757d', 1, 1000);";
        String locationSql = "INSERT OR IGNORE INTO locations (id, name, normalized_slug) VALUES (?, ?, ?);";
        String propertySql = """
            INSERT INTO properties (
                id, portal_id, location_id, external_id, url, title, status, discovered_at, viewed, viewed_at, created_at, updated_at
            ) VALUES (?, ?, ?, ?, ?, ?, 'ACTIVE', ?, ?, ?, ?, CURRENT_TIMESTAMP)
            ON CONFLICT(url) DO UPDATE SET
                portal_id = excluded.portal_id,
                location_id = excluded.location_id,
                external_id = COALESCE(excluded.external_id, properties.external_id),
                title = excluded.title,
                updated_at = CURRENT_TIMESTAMP,
                viewed = CASE WHEN properties.viewed = 1 THEN 1 ELSE excluded.viewed END,
                viewed_at = CASE WHEN properties.viewed = 1 THEN properties.viewed_at ELSE excluded.viewed_at END;
        """;
        String attrSql = """
            INSERT INTO property_attributes (
                property_id, price_raw, price_amount, price_millions, price_per_sqm, area_sqm, rooms, raw_description
            ) VALUES ((SELECT id FROM properties WHERE url = ?), ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT(property_id) DO UPDATE SET
                price_raw = excluded.price_raw,
                price_amount = excluded.price_amount,
                price_millions = excluded.price_millions,
                price_per_sqm = excluded.price_per_sqm,
                area_sqm = excluded.area_sqm,
                rooms = excluded.rooms,
                raw_description = excluded.raw_description;
        """;
        String imageSql = """
            INSERT INTO property_images (property_id, image_url, order_index, is_primary)
            SELECT p.id, ?, 0, 1
            FROM properties p
            WHERE p.url = ?
              AND NOT EXISTS (
                  SELECT 1 FROM property_images pi WHERE pi.property_id = p.id AND pi.image_url = ?
              );
        """;

        try (Connection conn = dbManager.getConnection()) {
            conn.setAutoCommit(false);

            try (PreparedStatement psPortal = conn.prepareStatement(portalSql);
                 PreparedStatement psLocation = conn.prepareStatement(locationSql);
                 PreparedStatement psProperty = conn.prepareStatement(propertySql);
                 PreparedStatement psAttr = conn.prepareStatement(attrSql);
                 PreparedStatement psImage = conn.prepareStatement(imageSql)) {

                for (IngatlanItem item : items) {
                    if (item == null || item.getUrl() == null || item.getUrl().isBlank()) {
                        continue;
                    }

                    String url = item.getUrl().trim();
                    String rawPortal = item.getPortal();
                    String portalId = resolvePortalId(rawPortal);
                    String portalName = resolvePortalName(portalId, rawPortal);

                    // 1. Portál batch
                    psPortal.setString(1, portalId);
                    psPortal.setString(2, portalName);
                    psPortal.addBatch();

                    // 2. Település batch
                    String city = item.getCity() != null && !item.getCity().isBlank() ? item.getCity() : "Debrecen";
                    String cityKey = CityUtils.normalizeCityKey(city);
                    psLocation.setString(1, cityKey);
                    psLocation.setString(2, city);
                    psLocation.setString(3, cityKey);
                    psLocation.addBatch();

                    // 3. Hirdetés alapadatok batch
                    String rawId = item.getId();
                    String id;
                    if (rawId != null && !rawId.isBlank() && !"N/A".equalsIgnoreCase(rawId)) {
                        id = portalId + "_" + rawId + "_" + Integer.toHexString(url.hashCode());
                    } else {
                        id = UUID.nameUUIDFromBytes(url.getBytes(StandardCharsets.UTF_8)).toString();
                    }
                    String title = item.getTitle() != null ? item.getTitle() : portalName + " hirdetés";
                    String discoveredAt = item.getDiscoveredAt() != null && !item.getDiscoveredAt().isBlank()
                            ? item.getDiscoveredAt()
                            : LocalDateTime.now().format(DATE_TIME_FORMATTER);
                    int viewed = item.isViewed() ? 1 : 0;
                    String viewedAt = item.getViewedAt();

                    psProperty.setString(1, id);
                    psProperty.setString(2, portalId);
                    psProperty.setString(3, cityKey);
                    psProperty.setString(4, rawId);
                    psProperty.setString(5, url);
                    psProperty.setString(6, title);
                    psProperty.setString(7, discoveredAt);
                    psProperty.setInt(8, viewed);
                    psProperty.setString(9, viewedAt);
                    psProperty.setString(10, discoveredAt);
                    psProperty.addBatch();

                    // 4. Hirdetés attribútumok batch
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

                    psAttr.setString(1, url);
                    psAttr.setString(2, priceRaw);
                    if (priceAmount != null) psAttr.setLong(3, priceAmount); else psAttr.setNull(3, Types.BIGINT);
                    if (priceMillions != null) psAttr.setDouble(4, priceMillions); else psAttr.setNull(4, Types.REAL);
                    if (pricePerSqm != null) psAttr.setLong(5, pricePerSqm); else psAttr.setNull(5, Types.BIGINT);
                    if (areaSqm != null) psAttr.setDouble(6, areaSqm); else psAttr.setNull(6, Types.REAL);
                    if (rooms != null) psAttr.setDouble(7, rooms); else psAttr.setNull(7, Types.REAL);
                    psAttr.setString(8, details);
                    psAttr.addBatch();

                    // 5. Kép batch
                    String imageUrl = item.getImageUrl();
                    if (imageUrl != null && !imageUrl.isBlank()) {
                        psImage.setString(1, imageUrl);
                        psImage.setString(2, url);
                        psImage.setString(3, imageUrl);
                        psImage.addBatch();
                    }
                }

                psPortal.executeBatch();
                psLocation.executeBatch();
                psProperty.executeBatch();
                psAttr.executeBatch();
                psImage.executeBatch();
            }
            conn.commit();
        } catch (SQLException e) {
            LOGGER.error("Hiba a hirdetések 3NF SQLite mentésekor: {}", e.getMessage(), e);
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
        rwLock.readLock().lock();
        try {
            String cityKey = CityUtils.normalizeCityKey(city);
            String sql = SELECT_3NF_BASE + " WHERE loc.normalized_slug = ? ORDER BY p.discovered_at DESC;";
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
            String sql = SELECT_3NF_BASE + " ORDER BY p.discovered_at DESC;";
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
            String sql = "SELECT 1 FROM properties p JOIN locations loc ON p.location_id = loc.id WHERE loc.normalized_slug = ? LIMIT 1;";
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
                String sql = "SELECT 1 FROM properties p JOIN locations loc ON p.location_id = loc.id WHERE p.url = ? AND loc.normalized_slug = ? LIMIT 1;";
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
            String sql = """
                SELECT loc.name AS city, loc.normalized_slug AS city_key, COUNT(*) AS cnt, MAX(p.discovered_at) AS last_seen
                FROM properties p
                JOIN locations loc ON p.location_id = loc.id
                GROUP BY loc.normalized_slug, loc.name
                ORDER BY cnt DESC;
            """;
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
            String sql = "DELETE FROM properties WHERE location_id = (SELECT id FROM locations WHERE normalized_slug = ? LIMIT 1);";
            try (Connection conn = dbManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, cityKey);
                int deleted = ps.executeUpdate();
                LOGGER.info("[SQLite 3NF] Város előzményei törölve: {} ({} db rekord)", city, deleted);
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
            try (Connection conn = dbManager.getConnection();
                 Statement stmt = conn.createStatement()) {
                stmt.execute("DELETE FROM property_images;");
                stmt.execute("DELETE FROM property_attributes;");
                int deleted = stmt.executeUpdate("DELETE FROM properties;");
                LOGGER.info("[SQLite 3NF] Teljes hirdetésadatbázis törölve ({} db rekord)", deleted);
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
    public Map<String, Long> countByPortal() {
        rwLock.readLock().lock();
        Map<String, Long> result = new LinkedHashMap<>();
        String sql = """
            SELECT po.name as portal, COUNT(*) as cnt
            FROM properties p
            JOIN portals po ON p.portal_id = po.id
            GROUP BY po.name
            ORDER BY cnt DESC;
        """;
        try (Connection conn = dbManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                String portal = rs.getString("portal");
                long count = rs.getLong("cnt");
                if (portal != null && !portal.isBlank()) {
                    result.put(portal.trim().toLowerCase(Locale.ROOT), count);
                }
            }
        } catch (SQLException e) {
            LOGGER.error("Hiba a portálonkénti hirdetésszám lekérdezésekor: {}", e.getMessage(), e);
        } finally {
            rwLock.readLock().unlock();
        }
        return result;
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
        item.setLocation(rs.getString("city"));

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
