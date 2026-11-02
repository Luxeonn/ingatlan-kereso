/**
 * Layer 3 — Pluggable real-estate portal scraper engines (Open/Closed Principle).
 *
 * <ul>
 *   <li>{@link ingatlanok.portal.PortalEngine}: Uniform scraper contract returning resilient {@link
 *       ingatlanok.model.PortalScrapeResult} status cards.
 *   <li>{@link ingatlanok.portal.PortalEngineRegistry}: Dynamic provider registry for active
 *       portals ({@code dh}, {@code oc}, {@code jofogas}, {@code koltozzbe}, {@code
 *       ingatlanbazar}, {@code ingatlancom}).
 * </ul>
 *
 * <p>Architectural constraint: All external Jsoup HTTP connections must route exclusively through
 * {@link ingatlanok.common.HttpFetcher} for SSRF protection.
 */
package ingatlanok.portal;
