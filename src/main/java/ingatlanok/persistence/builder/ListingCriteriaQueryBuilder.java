package ingatlanok.persistence.builder;

import ingatlanok.common.CityUtils;
import ingatlanok.dto.ListingDto;
import ingatlanok.dto.ListingFilterDto;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.statement.Query;

import java.util.ArrayList;
import java.util.List;

/**
 * Típusbiztos, dinamikus SQL lekérdezés-építő (Criteria Query Builder) a Jdbi v3 motorhoz.
 * Indexelt 3NF relációkra optimalizált SQL feltételeket és rendezést generál név szerinti paraméterkötéssel.
 */
public class ListingCriteriaQueryBuilder {

    public static final String SELECT_3NF_BASE = """
        SELECT p.id, po.name AS portal, p.url, p.title, p.status,
               loc.name AS city, loc.normalized_slug AS city_key,
               p.discovered_at, p.viewed, p.viewed_at,
               pa.price_raw, pa.price_amount, pa.price_millions, pa.price_per_sqm,
               pa.area_sqm, pa.rooms, pa.raw_description AS details,
               pi.image_url,
               ph.price_delta, ph.change_percent, ph.recorded_at AS last_price_change_at
        FROM properties p
        JOIN portals po ON p.portal_id = po.id
        JOIN locations loc ON p.location_id = loc.id
        LEFT JOIN property_attributes pa ON p.id = pa.property_id
        LEFT JOIN property_images pi ON p.id = pi.property_id AND pi.is_primary = 1
        LEFT JOIN (
            SELECT property_id, price_delta, change_percent, recorded_at,
                   ROW_NUMBER() OVER (PARTITION BY property_id ORDER BY id DESC) as rn
            FROM property_price_history
        ) ph ON p.id = ph.property_id AND ph.rn = 1
    """;

    public List<ListingDto> execute(Handle handle, ListingFilterDto filter) {
        StringBuilder sql = new StringBuilder(SELECT_3NF_BASE);
        List<String> conditions = new ArrayList<>();

        List<String> normalizedCities = new ArrayList<>();
        if (filter != null && filter.cities() != null && !filter.cities().isEmpty()) {
            for (String c : filter.cities()) {
                if (c != null && !c.isBlank()) {
                    normalizedCities.add(CityUtils.normalizeCityKey(c));
                }
            }
        }

        if (!normalizedCities.isEmpty()) {
            conditions.add("loc.normalized_slug IN (<cities>)");
        }

        if (filter != null) {
            if (filter.minPriceMillion() != null) {
                conditions.add("pa.price_millions >= :minPrice");
            }
            if (filter.maxPriceMillion() != null) {
                conditions.add("pa.price_millions <= :maxPrice");
            }
            if (filter.minArea() != null) {
                conditions.add("pa.area_sqm >= :minArea");
            }
            if (filter.maxArea() != null) {
                conditions.add("pa.area_sqm <= :maxArea");
            }
            if (filter.minRooms() != null) {
                conditions.add("pa.rooms >= :minRooms");
            }
            if (filter.maxRooms() != null) {
                conditions.add("pa.rooms <= :maxRooms");
            }
            if (filter.portals() != null && !filter.portals().isEmpty()) {
                conditions.add("(po.id IN (<portals>) OR po.name IN (<portals>))");
            }
            if (filter.excludeViewed()) {
                conditions.add("p.viewed = 0");
            }
            if (filter.onlyDiscounted()) {
                conditions.add("ph.price_delta < 0");
            }
        }

        if (!conditions.isEmpty()) {
            sql.append(" WHERE ").append(String.join(" AND ", conditions));
        }

        // Rendezés (ORDER BY)
        String sortBy = filter != null && filter.sortBy() != null ? filter.sortBy().trim().toUpperCase() : "DISCOVERED_DESC";
        switch (sortBy) {
            case "PRICE_ASC" -> sql.append(" ORDER BY pa.price_millions ASC, p.discovered_at DESC");
            case "PRICE_DESC" -> sql.append(" ORDER BY pa.price_millions DESC, p.discovered_at DESC");
            case "AREA_DESC" -> sql.append(" ORDER BY pa.area_sqm DESC, p.discovered_at DESC");
            case "SQM_PRICE_ASC" -> sql.append(" ORDER BY pa.price_per_sqm ASC, p.discovered_at DESC");
            case "DISCOUNT_DESC" -> sql.append(" ORDER BY ph.price_delta ASC, p.discovered_at DESC");
            default -> sql.append(" ORDER BY p.discovered_at DESC");
        }

        if (filter != null && filter.limit() != null && filter.limit() > 0) {
            sql.append(" LIMIT :limit");
            if (filter.offset() != null && filter.offset() > 0) {
                sql.append(" OFFSET :offset");
            }
        }
        sql.append(";");

        Query query = handle.createQuery(sql.toString());

        if (!normalizedCities.isEmpty()) {
            query.bindList("cities", normalizedCities);
        }

        if (filter != null) {
            if (filter.minPriceMillion() != null) {
                query.bind("minPrice", filter.minPriceMillion());
            }
            if (filter.maxPriceMillion() != null) {
                query.bind("maxPrice", filter.maxPriceMillion());
            }
            if (filter.minArea() != null) {
                query.bind("minArea", filter.minArea());
            }
            if (filter.maxArea() != null) {
                query.bind("maxArea", filter.maxArea());
            }
            if (filter.minRooms() != null) {
                query.bind("minRooms", filter.minRooms());
            }
            if (filter.maxRooms() != null) {
                query.bind("maxRooms", filter.maxRooms());
            }
            if (filter.portals() != null && !filter.portals().isEmpty()) {
                query.bindList("portals", filter.portals());
            }
            if (filter.limit() != null && filter.limit() > 0) {
                query.bind("limit", filter.limit());
                if (filter.offset() != null && filter.offset() > 0) {
                    query.bind("offset", filter.offset());
                }
            }
        }

        return query.mapTo(ListingDto.class).list();
    }
}
