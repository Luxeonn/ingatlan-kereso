package ingatlanok.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class PortalScrapeResult implements Serializable {

  private String portalId;
  private String portalName;
  private PortalScrapeStatus status;
  private List<IngatlanItem> items = new ArrayList<>();
  private String fallbackUrl;
  private String message;
  private long executionTimeMs;
  private Integer httpStatusCode;
  private String technicalDetails;

  public PortalScrapeResult(
      String portalId,
      String portalName,
      PortalScrapeStatus status,
      List<IngatlanItem> items,
      String fallbackUrl,
      String message,
      long executionTimeMs) {
    this(portalId, portalName, status, items, fallbackUrl, message, executionTimeMs, null, null);
  }

  @Builder
  public PortalScrapeResult(
      String portalId,
      String portalName,
      PortalScrapeStatus status,
      List<IngatlanItem> items,
      String fallbackUrl,
      String message,
      long executionTimeMs,
      Integer httpStatusCode,
      String technicalDetails) {
    this.portalId = portalId;
    this.portalName = portalName;
    this.status = status;
    this.items = items != null ? items : new ArrayList<>();
    this.fallbackUrl = fallbackUrl;
    this.message = message;
    this.executionTimeMs = executionTimeMs;
    this.httpStatusCode = httpStatusCode;
    this.technicalDetails = technicalDetails;
  }

  public static PortalScrapeResult success(
      String portalId, String portalName, List<IngatlanItem> items, long executionTimeMs) {
    PortalScrapeStatus status =
        (items == null || items.isEmpty()) ? PortalScrapeStatus.EMPTY : PortalScrapeStatus.SUCCESS;
    int count = (items != null ? items.size() : 0);
    String msg =
        (count > 0) ? "Sikeres letöltés (" + count + " találat)" : "0 találat a megadott szűrőkre";
    return PortalScrapeResult.builder()
        .portalId(portalId)
        .portalName(portalName)
        .status(status)
        .items(items)
        .fallbackUrl(null)
        .message(msg)
        .executionTimeMs(executionTimeMs)
        .httpStatusCode(200)
        .technicalDetails("Sikeres HTTP 200 válasz")
        .build();
  }

  public static PortalScrapeResult empty(
      String portalId, String portalName, String fallbackUrl, long executionTimeMs) {
    return PortalScrapeResult.builder()
        .portalId(portalId)
        .portalName(portalName)
        .status(PortalScrapeStatus.EMPTY)
        .items(Collections.emptyList())
        .fallbackUrl(fallbackUrl)
        .message("Nincs találat a megadott feltételekkel")
        .executionTimeMs(executionTimeMs)
        .httpStatusCode(200)
        .technicalDetails("A lekérdezés 0 találatot eredményezett a keresési kritériumok alapján")
        .build();
  }

  public static PortalScrapeResult botProtected(
      String portalId, String portalName, String fallbackUrl, long executionTimeMs) {
    return PortalScrapeResult.builder()
        .portalId(portalId)
        .portalName(portalName)
        .status(PortalScrapeStatus.BOT_PROTECTED)
        .items(Collections.emptyList())
        .fallbackUrl(fallbackUrl)
        .message("Cloudflare WAF bot-védelem aktív; közvetlen keresőlink biztosítva")
        .executionTimeMs(executionTimeMs)
        .httpStatusCode(403)
        .technicalDetails("HTTP 403 Forbidden / Cloudflare Turnstile JavaScript challenge detektálva")
        .build();
  }

  public static PortalScrapeResult spaFallback(
      String portalId, String portalName, String fallbackUrl, long executionTimeMs) {
    return PortalScrapeResult.builder()
        .portalId(portalId)
        .portalName(portalName)
        .status(PortalScrapeStatus.SPA_FALLBACK)
        .items(Collections.emptyList())
        .fallbackUrl(fallbackUrl)
        .message("Kliensoldali SPA alkalmazás; közvetlen portállink biztosítva")
        .executionTimeMs(executionTimeMs)
        .httpStatusCode(200)
        .technicalDetails("Dinamikus kliensoldali JavaScript renderelés szükséges")
        .build();
  }

  public static PortalScrapeResult failure(
      String portalId, String portalName, String errorMsg, long executionTimeMs) {
    return PortalScrapeResult.builder()
        .portalId(portalId)
        .portalName(portalName)
        .status(PortalScrapeStatus.FAILED)
        .items(Collections.emptyList())
        .fallbackUrl(null)
        .message("Hiba a letöltés során: " + errorMsg)
        .executionTimeMs(executionTimeMs)
        .httpStatusCode(500)
        .technicalDetails(errorMsg)
        .build();
  }

  public static PortalScrapeResult failure(
      String portalId,
      String portalName,
      String errorMsg,
      long executionTimeMs,
      Integer httpStatusCode,
      String fallbackUrl) {
    return PortalScrapeResult.builder()
        .portalId(portalId)
        .portalName(portalName)
        .status(PortalScrapeStatus.FAILED)
        .items(Collections.emptyList())
        .fallbackUrl(fallbackUrl)
        .message("Hiba a letöltés során: " + errorMsg)
        .executionTimeMs(executionTimeMs)
        .httpStatusCode(httpStatusCode)
        .technicalDetails(errorMsg)
        .build();
  }
}
