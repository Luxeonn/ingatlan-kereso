package ingatlanok.persistence.mapper;

import ingatlanok.dto.PriceHistoryDto;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Jdbi RowMapper a property_price_history tábla rekordjainak PriceHistoryDto-ra való konvertálásához.
 */
public class PriceHistoryRowMapper implements RowMapper<PriceHistoryDto> {

    @Override
    public PriceHistoryDto map(ResultSet rs, StatementContext ctx) throws SQLException {
        int id = rs.getInt("id");
        String propertyId = rs.getString("property_id");
        String recordedAt = rs.getString("recorded_at");
        String priceRaw = rs.getString("price_raw");
        long priceAmount = rs.getLong("price_amount");
        double priceMillions = rs.getDouble("price_millions");

        long pricePerSqmVal = rs.getLong("price_per_sqm");
        Long pricePerSqm = rs.wasNull() ? null : pricePerSqmVal;

        long prevVal = rs.getLong("previous_price_amount");
        Long prevAmount = rs.wasNull() ? null : prevVal;

        long deltaVal = rs.getLong("price_delta");
        Long delta = rs.wasNull() ? null : deltaVal;

        double changeVal = rs.getDouble("change_percent");
        Double changePct = rs.wasNull() ? null : changeVal;

        return new PriceHistoryDto(
                id,
                propertyId,
                recordedAt,
                priceRaw,
                priceAmount,
                priceMillions,
                pricePerSqm,
                prevAmount,
                delta,
                changePct
        );
    }
}
