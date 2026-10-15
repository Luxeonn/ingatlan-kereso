package ingatlanok.persistence.mapper;

import ingatlanok.dto.StatusLogDto;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Jdbi RowMapper a property_status_log tábla rekordjainak StatusLogDto-ra való konvertálásához.
 */
public class StatusLogRowMapper implements RowMapper<StatusLogDto> {

    @Override
    public StatusLogDto map(ResultSet rs, StatementContext ctx) throws SQLException {
        int id = rs.getInt("id");
        String propertyId = rs.getString("property_id");
        String status = rs.getString("status");
        String changedAt = rs.getString("changed_at");
        String note = rs.getString("note");

        return new StatusLogDto(id, propertyId, status, changedAt, note);
    }
}
