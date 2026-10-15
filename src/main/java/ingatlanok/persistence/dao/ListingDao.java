package ingatlanok.persistence.dao;

import ingatlanok.dto.ListingDto;
import ingatlanok.dto.PriceHistoryDto;
import ingatlanok.dto.StatusLogDto;
import ingatlanok.persistence.mapper.ListingRowMapper;
import ingatlanok.persistence.mapper.PriceHistoryRowMapper;
import ingatlanok.persistence.mapper.StatusLogRowMapper;
import org.jdbi.v3.sqlobject.config.RegisterConstructorMapper;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import java.util.List;
import java.util.Optional;

/**
 * Deklaratív, típusbiztos Jdbi SqlObject DAO a hirdetések, portálok és relációk kezelésére.
 */
@RegisterRowMapper(ListingRowMapper.class)
@RegisterRowMapper(PriceHistoryRowMapper.class)
@RegisterRowMapper(StatusLogRowMapper.class)
public interface ListingDao {

    record ExistingPropertyInfo(String id, String status, Long priceAmount, int historyCount) {}
    record PortalCountRecord(String portal, int count) {}
    record TrackedCityRecord(String cityKey, String cityName, int count) {}

    @SqlQuery("SELECT COUNT(*) FROM properties;")
    int getTotalCount();

    @SqlQuery("SELECT url FROM properties;")
    List<String> getAllUrls();

    @SqlQuery("SELECT url FROM properties WHERE location_id = :cityKey;")
    List<String> getUrlsByCity(@Bind("cityKey") String cityKey);

    @SqlQuery("SELECT id FROM properties WHERE url = :url LIMIT 1;")
    Optional<String> containsUrl(@Bind("url") String url);

    @SqlQuery("SELECT id FROM properties WHERE location_id = :cityKey AND url = :url LIMIT 1;")
    Optional<String> containsCityUrl(@Bind("cityKey") String cityKey, @Bind("url") String url);

    @SqlQuery("SELECT id FROM properties WHERE location_id = :cityKey LIMIT 1;")
    Optional<String> hasCityDatabase(@Bind("cityKey") String cityKey);

    @SqlQuery("SELECT viewed FROM properties WHERE url = :url LIMIT 1;")
    Optional<Integer> isViewed(@Bind("url") String url);

    @SqlUpdate("UPDATE properties SET viewed = 1, viewed_at = :viewedAt WHERE url = :url;")
    int markAsViewed(@Bind("url") String url, @Bind("viewedAt") String viewedAt);

    @SqlQuery("SELECT MAX(discovered_at) FROM properties;")
    Optional<String> getLastRunTime();

    @SqlQuery("""
        SELECT po.name AS portal, COUNT(p.id) AS count
        FROM properties p
        JOIN portals po ON p.portal_id = po.id
        GROUP BY po.name;
    """)
    @RegisterConstructorMapper(PortalCountRecord.class)
    List<PortalCountRecord> countByPortal();

    @SqlQuery("""
        SELECT loc.normalized_slug AS cityKey, loc.name AS cityName, COUNT(p.id) AS count
        FROM properties p
        JOIN locations loc ON p.location_id = loc.id
        GROUP BY loc.normalized_slug, loc.name;
    """)
    @RegisterConstructorMapper(TrackedCityRecord.class)
    List<TrackedCityRecord> getTrackedCities();

    @SqlQuery("""
        SELECT ph.id, ph.property_id, ph.recorded_at, ph.price_raw, ph.price_amount,
               ph.price_millions, ph.price_per_sqm, ph.previous_price_amount,
               ph.price_delta, ph.change_percent
        FROM property_price_history ph
        JOIN properties p ON ph.property_id = p.id
        WHERE p.id = :idOrUrl OR p.url = :idOrUrl
        ORDER BY ph.id ASC;
    """)
    List<PriceHistoryDto> getPriceHistory(@Bind("idOrUrl") String idOrUrl);

    @SqlQuery("""
        SELECT sl.id, sl.property_id, sl.status, sl.changed_at, sl.note
        FROM property_status_log sl
        JOIN properties p ON sl.property_id = p.id
        WHERE p.id = :idOrUrl OR p.url = :idOrUrl
        ORDER BY sl.id ASC;
    """)
    List<StatusLogDto> getStatusLog(@Bind("idOrUrl") String idOrUrl);

    @SqlQuery("""
        SELECT p.id, p.status, pa.price_amount as priceAmount,
               (SELECT COUNT(*) FROM property_price_history ph WHERE ph.property_id = p.id) AS historyCount
        FROM properties p
        LEFT JOIN property_attributes pa ON p.id = pa.property_id
        WHERE p.url = :url LIMIT 1;
    """)
    @RegisterConstructorMapper(ExistingPropertyInfo.class)
    Optional<ExistingPropertyInfo> findExistingPropertyInfo(@Bind("url") String url);

    @SqlUpdate("INSERT OR IGNORE INTO portals (id, name, base_url, color_hex, is_active, rate_limit_ms) VALUES (:id, :name, '', '#6c757d', 1, 1000);")
    void upsertPortal(@Bind("id") String id, @Bind("name") String name);

    @SqlUpdate("INSERT OR IGNORE INTO locations (id, name, normalized_slug) VALUES (:id, :name, :slug);")
    void upsertLocation(@Bind("id") String id, @Bind("name") String name, @Bind("slug") String slug);

    @SqlUpdate("""
        INSERT INTO properties (
            id, portal_id, location_id, external_id, url, title, status, discovered_at, viewed, viewed_at, created_at, updated_at
        ) VALUES (:id, :portalId, :locationId, :externalId, :url, :title, :status, :discoveredAt, :viewed, :viewedAt, :discoveredAt, CURRENT_TIMESTAMP)
        ON CONFLICT(url) DO UPDATE SET
            portal_id = excluded.portal_id,
            location_id = excluded.location_id,
            external_id = COALESCE(excluded.external_id, properties.external_id),
            title = excluded.title,
            status = excluded.status,
            updated_at = CURRENT_TIMESTAMP,
            viewed = CASE WHEN properties.viewed = 1 THEN 1 ELSE excluded.viewed END,
            viewed_at = CASE WHEN properties.viewed = 1 THEN properties.viewed_at ELSE excluded.viewed_at END;
    """)
    void upsertProperty(
            @Bind("id") String id,
            @Bind("portalId") String portalId,
            @Bind("locationId") String locationId,
            @Bind("externalId") String externalId,
            @Bind("url") String url,
            @Bind("title") String title,
            @Bind("status") String status,
            @Bind("discoveredAt") String discoveredAt,
            @Bind("viewed") int viewed,
            @Bind("viewedAt") String viewedAt
    );

    @SqlUpdate("""
        INSERT INTO property_attributes (
            property_id, price_raw, price_amount, price_millions, price_per_sqm, area_sqm, rooms, raw_description
        ) VALUES ((SELECT id FROM properties WHERE url = :url), :priceRaw, :priceAmount, :priceMillions, :pricePerSqm, :areaSqm, :rooms, :details)
        ON CONFLICT(property_id) DO UPDATE SET
            price_raw = excluded.price_raw,
            price_amount = excluded.price_amount,
            price_millions = excluded.price_millions,
            price_per_sqm = excluded.price_per_sqm,
            area_sqm = excluded.area_sqm,
            rooms = excluded.rooms,
            raw_description = excluded.raw_description;
    """)
    void upsertAttributes(
            @Bind("url") String url,
            @Bind("priceRaw") String priceRaw,
            @Bind("priceAmount") Long priceAmount,
            @Bind("priceMillions") Double priceMillions,
            @Bind("pricePerSqm") Long pricePerSqm,
            @Bind("areaSqm") Double areaSqm,
            @Bind("rooms") Double rooms,
            @Bind("details") String details
    );

    @SqlUpdate("""
        INSERT INTO property_images (property_id, image_url, order_index, is_primary)
        SELECT p.id, :imageUrl, 0, 1
        FROM properties p
        WHERE p.url = :url
          AND NOT EXISTS (
              SELECT 1 FROM property_images pi WHERE pi.property_id = p.id AND pi.image_url = :imageUrl
          );
    """)
    void insertPrimaryImage(@Bind("url") String url, @Bind("imageUrl") String imageUrl);

    @SqlUpdate("""
        INSERT INTO property_price_history (
            property_id, recorded_at, price_raw, price_amount, price_millions, price_per_sqm,
            previous_price_amount, price_delta, change_percent
        ) VALUES (:propertyId, :recordedAt, :priceRaw, :priceAmount, :priceMillions, :pricePerSqm, :prevAmount, :delta, :changePct);
    """)
    void insertPriceHistory(
            @Bind("propertyId") String propertyId,
            @Bind("recordedAt") String recordedAt,
            @Bind("priceRaw") String priceRaw,
            @Bind("priceAmount") Long priceAmount,
            @Bind("priceMillions") Double priceMillions,
            @Bind("pricePerSqm") Long pricePerSqm,
            @Bind("prevAmount") Long prevAmount,
            @Bind("delta") Long delta,
            @Bind("changePct") Double changePct
    );

    @SqlUpdate("""
        INSERT INTO property_status_log (
            property_id, status, changed_at, note
        ) VALUES (:propertyId, :status, :changedAt, :note);
    """)
    void insertStatusLog(
            @Bind("propertyId") String propertyId,
            @Bind("status") String status,
            @Bind("changedAt") String changedAt,
            @Bind("note") String note
    );

    @SqlUpdate("INSERT INTO search_runs (executed_at, total_found, new_found, criteria_json, portal_stats_json) VALUES (:executedAt, :totalFound, :newFound, :criteriaJson, :portalStatsJson);")
    void recordSearchRun(
            @Bind("executedAt") String executedAt,
            @Bind("totalFound") int totalFound,
            @Bind("newFound") int newFound,
            @Bind("criteriaJson") String criteriaJson,
            @Bind("portalStatsJson") String portalStatsJson
    );

    @SqlUpdate("DELETE FROM property_price_history;")
    void clearPriceHistory();

    @SqlUpdate("DELETE FROM property_status_log;")
    void clearStatusLog();

    @SqlUpdate("DELETE FROM property_images;")
    void clearPropertyImages();

    @SqlUpdate("DELETE FROM property_attributes;")
    void clearPropertyAttributes();

    @SqlUpdate("DELETE FROM properties;")
    int clearProperties();

    @SqlUpdate("DELETE FROM search_runs;")
    void clearSearchRuns();
}
