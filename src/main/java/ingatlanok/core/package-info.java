/**
 * Layer 2 — Pure business logic and analytical engines.
 *
 * <ul>
 *   <li>{@link ingatlanok.core.SearchExecutionEngine}: Parallel virtual-thread scraping coordinator
 *       with {@link ingatlanok.core.SearchPersistenceSynchronizer}.
 *   <li>{@link ingatlanok.core.ListingNormalizer}: Canonical {@link ingatlanok.model.PropertyType}
 *       normalizer.
 *   <li>{@link ingatlanok.core.PropertyFilterEngine}: Price, area, city, and view-state filtering.
 *   <li>{@link ingatlanok.core.DeduplicationEngine}: Multi-signal cross-portal clustering.
 *   <li>{@link ingatlanok.core.DataQualityEngine}: 0–100 deterministic scoring and outlier quarantine.
 *   <li>{@link ingatlanok.core.MarketAnalyticsEngine} &amp; {@link
 *       ingatlanok.core.MarketStatisticsCalculator}: Median Ft/m² and bargain detection.
 * </ul>
 *
 * <p>Architectural constraint: Must never depend on {@code presentation}, {@code web}, {@code
 * config}, or direct JPA repositories/entities.
 */
package ingatlanok.core;
