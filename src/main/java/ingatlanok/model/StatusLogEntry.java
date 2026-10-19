package ingatlanok.model;

import java.io.Serializable;

/** Hirdetés életciklus és státuszkövetési naplóbejegyzés (Status Log Entry). */
public record StatusLogEntry(
    int id, String propertyId, String status, String changedAt, String note)
    implements Serializable {}
