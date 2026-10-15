package ingatlanok.persistence.mapper;

import ingatlanok.dto.ListingDto;
import ingatlanok.dto.PriceInfoDto;
import ingatlanok.dto.PropertyDetailsDto;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * 3NF relációs ResultSet-et ListingDto rekordra leképező típusbiztos Jdbi RowMapper.
 */
public class ListingRowMapper implements RowMapper<ListingDto> {

    @Override
    public ListingDto map(ResultSet rs, StatementContext ctx) throws SQLException {
        String id = rs.getString("id");
        String portal = rs.getString("portal");
        String url = rs.getString("url");
        String title = rs.getString("title");
        String status = rs.getString("status");
        String city = rs.getString("city");
        String location = city;
        String discoveredAt = rs.getString("discovered_at");
        boolean viewed = rs.getInt("viewed") == 1;
        String viewedAt = rs.getString("viewed_at");
        String details = rs.getString("details");
        String imageUrl = rs.getString("image_url");

        String priceRaw = rs.getString("price_raw");
        long priceAmount = rs.getLong("price_amount");
        Long amountFt = rs.wasNull() ? null : priceAmount;
        double priceMillions = rs.getDouble("price_millions");
        Double amountMillion = rs.wasNull() ? null : priceMillions;

        PriceInfoDto priceInfo = (priceRaw != null || amountFt != null || amountMillion != null)
                ? new PriceInfoDto(priceRaw, amountFt, amountMillion)
                : null;

        double areaSqm = rs.getDouble("area_sqm");
        Double floorAreaSqm = rs.wasNull() ? null : areaSqm;
        double rooms = rs.getDouble("rooms");
        Double roomCount = rs.wasNull() ? null : rooms;
        long pricePerSqm = rs.getLong("price_per_sqm");
        Long pricePerSqmFt = rs.wasNull() ? null : pricePerSqm;

        PropertyDetailsDto detailsInfo = (details != null || floorAreaSqm != null || roomCount != null || pricePerSqmFt != null)
                ? new PropertyDetailsDto(details, floorAreaSqm, roomCount, pricePerSqmFt)
                : null;

        long priceDelta = rs.getLong("price_delta");
        Long delta = rs.wasNull() ? null : priceDelta;
        double changePercent = rs.getDouble("change_percent");
        Double changePct = rs.wasNull() ? null : changePercent;
        String lastPriceChangeAt = rs.getString("last_price_change_at");

        return new ListingDto(
                id,
                portal,
                url,
                title,
                priceRaw,
                location,
                city,
                details,
                discoveredAt,
                false,
                viewed,
                viewedAt,
                imageUrl,
                status != null ? status : "ACTIVE",
                delta,
                changePct,
                lastPriceChangeAt,
                priceInfo,
                detailsInfo
        );
    }
}
