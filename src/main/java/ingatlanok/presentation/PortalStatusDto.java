package ingatlanok.presentation;

import lombok.Builder;

import ingatlanok.model.PortalScrapeStatus;

/**
 * Presentation DTO representing the scrape execution status of a single portal (#401 / JAVA-006).
 */
@Builder(toBuilder = true)
public record PortalStatusDto(
    String portalId,
    String portalName,
    PortalScrapeStatus status,
    int foundCount,
    long latencyMs,
    String fallbackUrl,
    String message,
    Integer httpStatusCode,
    String technicalDetails) {

  public PortalStatusDto {
    status = status != null ? status : PortalScrapeStatus.FAILED;
  }

  public PortalStatusDto(
      String portalId,
      String portalName,
      PortalScrapeStatus status,
      int foundCount,
      long latencyMs,
      String fallbackUrl,
      String message) {
    this(portalId, portalName, status, foundCount, latencyMs, fallbackUrl, message, null, null);
  }

  public PortalStatusDto(
      String portalId,
      String portalName,
      String status,
      int foundCount,
      long latencyMs,
      String fallbackUrl,
      String message) {
    this(
        portalId,
        portalName,
        PortalScrapeStatus.fromString(status),
        foundCount,
        latencyMs,
        fallbackUrl,
        message,
        null,
        null);
  }
}
