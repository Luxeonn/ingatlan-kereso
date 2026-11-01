/**
 * Layer 1a — Presentation facade and immutable REST response DTOs.
 *
 * <ul>
 *   <li>{@link ingatlanok.presentation.DashboardPresentationService}: Orchestrates search results,
 *       market analytics, and portal health into {@link
 *       ingatlanok.presentation.DashboardResponseDto}.
 *   <li>{@link ingatlanok.presentation.PresentationMapper}: MapStruct domain-to-DTO mapper ({@code
 *       ReportingPolicy.ERROR}).
 *   <li>Read-only DTOs ({@link ingatlanok.presentation.MarketStatsDto}, {@link
 *       ingatlanok.presentation.MarketSegmentStatsDto}, {@link
 *       ingatlanok.presentation.PriceDropItemDto}, {@link
 *       ingatlanok.presentation.PortalStatusDto}): Immutable {@code @Getter @Builder} value carriers
 *       without setters.
 * </ul>
 */
package ingatlanok.presentation;
