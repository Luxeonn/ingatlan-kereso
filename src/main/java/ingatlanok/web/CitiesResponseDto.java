package ingatlanok.web;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import ingatlanok.model.PredefinedCity;
import ingatlanok.model.TrackedCityInfo;

/**
 * Strongly typed response for the GET /api/cities endpoint (#401 / JAVA-017).
 */
public record CitiesResponseDto(
    List<PredefinedCity> predefined,
    Map<String, List<String>> presets,
    List<TrackedCityInfo> trackedCities)
    implements Serializable {}
