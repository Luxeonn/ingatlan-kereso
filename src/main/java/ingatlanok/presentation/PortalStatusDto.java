package ingatlanok.presentation;

import java.io.Serializable;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class PortalStatusDto implements Serializable {

  private String portalId;
  private String portalName;
  private String status;
  private int foundCount;
  private long latencyMs;
  private String fallbackUrl;
  private String message;
  private Integer httpStatusCode;
  private String technicalDetails;

  public PortalStatusDto(
      String portalId,
      String portalName,
      String status,
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
      String message,
      Integer httpStatusCode,
      String technicalDetails) {
    this.portalId = portalId;
    this.portalName = portalName;
    this.status = status;
    this.foundCount = foundCount;
    this.latencyMs = latencyMs;
    this.fallbackUrl = fallbackUrl;
    this.message = message;
    this.httpStatusCode = httpStatusCode;
    this.technicalDetails = technicalDetails;
  }
}
