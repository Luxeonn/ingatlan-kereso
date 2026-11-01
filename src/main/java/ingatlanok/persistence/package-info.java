/**
 * Layer 4 — Storage abstraction and 3-tier Spring Data JPA persistence.
 *
 * <ul>
 *   <li>{@link ingatlanok.persistence.ListingStorage} &amp; {@link
 *       ingatlanok.persistence.ConfigStorage}: Public storage contracts consumed by higher layers.
 *   <li>{@link ingatlanok.persistence.JpaIngatlanStorage} &amp; {@link
 *       ingatlanok.persistence.service.PropertyPersistenceHelper}: Primary JPA storage service.
 *   <li>{@link ingatlanok.persistence.mapper.JpaEntityMapper}: MapStruct entity-to-domain mapper
 *       ({@code ReportingPolicy.ERROR}).
 * </ul>
 *
 * <p>Architectural constraint: JPA entities and Spring Data repositories in {@code entity} and
 * {@code repository} subpackages must remain encapsulated within {@code ingatlanok.persistence}.
 */
package ingatlanok.persistence;
