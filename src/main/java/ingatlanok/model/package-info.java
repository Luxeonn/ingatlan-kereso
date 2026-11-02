/**
 * Foundational domain models, canonical enums, and immutable value objects.
 *
 * <ul>
 *   <li>{@link ingatlanok.model.IngatlanItem}, {@link ingatlanok.model.Property}, {@link
 *       ingatlanok.model.SourceListing}, {@link ingatlanok.model.ListingObservation}: 3-tier
 *       listing and observation models.
 *   <li>{@link ingatlanok.model.MarketStats}, {@link ingatlanok.model.MarketSegmentStats}, {@link
 *       ingatlanok.model.PriceDropItem}: Immutable market analytics domain models.
 *   <li>{@link ingatlanok.model.PriceInfo}, {@link ingatlanok.model.PropertyDetails}, {@link
 *       ingatlanok.model.PortalSource}: Read-only value objects without setters.
 * </ul>
 *
 * <p>Architectural constraint: Must remain independent of all higher application layers.
 */
package ingatlanok.model;
