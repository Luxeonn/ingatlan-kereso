package ingatlanok.presentation;

import java.io.Serializable;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import ingatlanok.model.PortalScrapeStatus;

/**
 * Presentation DTO representing the scrape execution status of a single portal (#401 / JAVA-006).
 */
@Getter
@NoArgsConstructor
public class PortalStatusDto implements Serializable {

  private String portalId;
  private String portalName;
  private PortalScrapeStatus status;
  private int foundCount;
  private long latencyMs;
  private String fallbackUrl;
  private String message;
  private Integer httpStatusCode;
  private String technicalDetails;

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

  @Builder(toBuilder = true)
  public PortalStatusDto(
      String portalId,
      String portalName,
      PortalScrapeStatus status,
      int foundCount,
      long latencyMs,
      String fallbackUrl,
      String message,
      Integer httpStatusCode,
      String technicalDetails) {
    this.portalId = portalId;
    this.portalName = portalName;
    this.status = status != null ? status : PortalScrapeStatus.FAILED;
    this.foundCount = foundCount;
    this.latencyMs = latencyMs;
    this.fallbackUrl = fallbackUrl;
    this.message = message;
    this.httpStatusCode = httpStatusCode;
    this.technicalDetails = technicalDetails;
  }
}
