package ingatlanok.scraper;

import ingatlanok.model.IngatlanItem;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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

    public PortalScrapeResult() {
    }

    public PortalScrapeResult(String portalId, String portalName, PortalScrapeStatus status, List<IngatlanItem> items, String fallbackUrl, String message, long executionTimeMs) {
        this(portalId, portalName, status, items, fallbackUrl, message, executionTimeMs, null, null);
    }

    public PortalScrapeResult(String portalId, String portalName, PortalScrapeStatus status, List<IngatlanItem> items, String fallbackUrl, String message, long executionTimeMs, Integer httpStatusCode, String technicalDetails) {
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

    public static PortalScrapeResult success(String portalId, String portalName, List<IngatlanItem> items, long executionTimeMs) {
        PortalScrapeStatus status = (items == null || items.isEmpty()) ? PortalScrapeStatus.EMPTY : PortalScrapeStatus.SUCCESS;
        int count = (items != null ? items.size() : 0);
        String msg = (count > 0) ? "Sikeres letöltés (" + count + " találat)" : "0 találat a megadott szűrőkre";
        return new PortalScrapeResult(portalId, portalName, status, items, null, msg, executionTimeMs, 200, "Sikeres HTTP 200 válasz");
    }

    public static PortalScrapeResult empty(String portalId, String portalName, String fallbackUrl, long executionTimeMs) {
        return new PortalScrapeResult(portalId, portalName, PortalScrapeStatus.EMPTY, Collections.emptyList(), fallbackUrl, "Nincs találat a megadott feltételekkel", executionTimeMs, 200, "A lekérdezés 0 találatot eredményezett a keresési kritériumok alapján");
    }

    public static PortalScrapeResult botProtected(String portalId, String portalName, String fallbackUrl, long executionTimeMs) {
        return new PortalScrapeResult(portalId, portalName, PortalScrapeStatus.BOT_PROTECTED, Collections.emptyList(), fallbackUrl, "Cloudflare WAF bot-védelem aktív; közvetlen keresőlink biztosítva", executionTimeMs, 403, "HTTP 403 Forbidden / Cloudflare Turnstile JavaScript challenge detektálva");
    }

    public static PortalScrapeResult spaFallback(String portalId, String portalName, String fallbackUrl, long executionTimeMs) {
        return new PortalScrapeResult(portalId, portalName, PortalScrapeStatus.SPA_FALLBACK, Collections.emptyList(), fallbackUrl, "Kliensoldali SPA alkalmazás; közvetlen portállink biztosítva", executionTimeMs, 200, "Dinamikus kliensoldali JavaScript renderelés szükséges");
    }

    public static PortalScrapeResult failure(String portalId, String portalName, String errorMsg, long executionTimeMs) {
        return new PortalScrapeResult(portalId, portalName, PortalScrapeStatus.FAILED, Collections.emptyList(), null, "Hiba a letöltés során: " + errorMsg, executionTimeMs, 500, errorMsg);
    }

    public static PortalScrapeResult failure(String portalId, String portalName, String errorMsg, long executionTimeMs, Integer httpStatusCode, String fallbackUrl) {
        return new PortalScrapeResult(portalId, portalName, PortalScrapeStatus.FAILED, Collections.emptyList(), fallbackUrl, "Hiba a letöltés során: " + errorMsg, executionTimeMs, httpStatusCode, errorMsg);
    }

    public String getPortalId() {
        return portalId;
    }

    public void setPortalId(String portalId) {
        this.portalId = portalId;
    }

    public String getPortalName() {
        return portalName;
    }

    public void setPortalName(String portalName) {
        this.portalName = portalName;
    }

    public PortalScrapeStatus getStatus() {
        return status;
    }

    public void setStatus(PortalScrapeStatus status) {
        this.status = status;
    }

    public List<IngatlanItem> getItems() {
        return items;
    }

    public void setItems(List<IngatlanItem> items) {
        this.items = items;
    }

    public String getFallbackUrl() {
        return fallbackUrl;
    }

    public void setFallbackUrl(String fallbackUrl) {
        this.fallbackUrl = fallbackUrl;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public long getExecutionTimeMs() {
        return executionTimeMs;
    }

    public void setExecutionTimeMs(long executionTimeMs) {
        this.executionTimeMs = executionTimeMs;
    }

    public Integer getHttpStatusCode() {
        return httpStatusCode;
    }

    public void setHttpStatusCode(Integer httpStatusCode) {
        this.httpStatusCode = httpStatusCode;
    }

    public String getTechnicalDetails() {
        return technicalDetails;
    }

    public void setTechnicalDetails(String technicalDetails) {
        this.technicalDetails = technicalDetails;
    }
}
