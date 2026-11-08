package ingatlanok.model;

import java.io.Serializable;

/**
 * Predefined geographic city metadata with county and region description (#401 / JAVA-023).
 */
public record PredefinedCity(String key, String name, String county, String description)
    implements Serializable {}
