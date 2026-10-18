package ingatlanok.dto;

import ingatlanok.model.StatusLogEntry;

import java.io.Serializable;

/**
 * Típusbiztos státusznapló DTO (Record).
 */
public record StatusLogDto(
        int id,
        String propertyId,
        String status,
        String changedAt,
        String note
) implements Serializable {

    public StatusLogEntry toModel() {
        return new StatusLogEntry(id, propertyId, status, changedAt, note);
    }
}
