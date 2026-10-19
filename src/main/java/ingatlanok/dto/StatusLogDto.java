package ingatlanok.dto;

import java.io.Serializable;

import ingatlanok.model.StatusLogEntry;

/** Típusbiztos státusznapló DTO (Record). */
public record StatusLogDto(int id, String propertyId, String status, String changedAt, String note)
    implements Serializable {

  public StatusLogEntry toModel() {
    return new StatusLogEntry(id, propertyId, status, changedAt, note);
  }
}
