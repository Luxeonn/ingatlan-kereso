/**
 * Shared cross-cutting utilities and single-point network/parsing helpers.
 *
 * <ul>
 *   <li>{@link ingatlanok.common.HttpFetcher}: Single-point SSRF-validated HTTP/Jsoup client.
 *   <li>{@link ingatlanok.common.PropertyParser}: Deterministic price, floor area, and room count
 *       regex parser.
 *   <li>{@link ingatlanok.common.CityUtils} &amp; {@link ingatlanok.common.SlugUtils}: Hungarian
 *       city and diacritic normalization.
 * </ul>
 *
 * <p>Architectural constraint: Must remain independent of all higher application layers.
 */
package ingatlanok.common;
