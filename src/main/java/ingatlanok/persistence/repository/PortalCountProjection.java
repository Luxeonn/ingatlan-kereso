package ingatlanok.persistence.repository;

/**
 * Projection record for counting listings grouped by portal (#401 / JAVA-021).
 */
public record PortalCountProjection(String portal, long count) {}
