package ingatlanok.model;

import java.io.Serializable;

/**
 * Summary record of a tracked city and its active listing count (#401 / JAVA-022).
 */
public record TrackedCityInfo(String name, String key, long count) implements Serializable {}
