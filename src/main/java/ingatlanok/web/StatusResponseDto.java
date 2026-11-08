package ingatlanok.web;

import java.io.Serializable;
import java.util.List;

import ingatlanok.model.TrackedCityInfo;

/**
 * Strongly typed response for the GET /api/status endpoint (#401 / JAVA-017).
 */
public record StatusResponseDto(
    String status,
    int port,
    int totalInDb,
    String lastRunTime,
    List<TrackedCityInfo> trackedCities)
    implements Serializable {}
