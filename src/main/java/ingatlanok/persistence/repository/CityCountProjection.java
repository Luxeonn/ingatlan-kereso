package ingatlanok.persistence.repository;

/**
 * Projection record for counting listings grouped by city (#401 / JAVA-021).
 */
public record CityCountProjection(String city, String normalizedCity, long count) {}
