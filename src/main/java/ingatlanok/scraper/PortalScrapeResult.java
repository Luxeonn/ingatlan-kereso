package ingatlanok.scraper;

import ingatlanok.model.IngatlanItem;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Egy adott portál scraping futásának állapotgazdag és izolált eredménye.
 */
public class PortalScrapeResult implements Serializable {

    private String portalId;
    private String portalName;
    private PortalScrapeStatus status;
    private List<IngatlanItem> items = new ArrayList<>();
    private String fallbackUrl;
    private String message;
    private long executionTimeMs;

    public PortalScrapeResult() {
    }

    public PortalScrapeResult(String portalId, String portalName, PortalScrapeStatus status, List<IngatlanItem> items, String fallbackUrl, String message, long executionTimeMs) {
        this.portalId = portalId;
        this.portalName = portalName;
        this.status = status;
        this.items = items != null ? items : new ArrayList<>();
        this.fallbackUrl = fallbackUrl;
        this.message = message;
        this.executionTimeMs = executionTimeMs;
    }

    public static PortalScrapeResult success(String portalId, String portalName, List<IngatlanItem> items, long executionTimeMs) {
        PortalScrapeStatus status = (items == null || items.isEmpty()) ? PortalScrapeStatus.EMPTY : PortalScrapeStatus.SUCCESS;
        return new PortalScrapeResult(portalId, portalName, status, items, null, "Sikeres letöltés (" + (items != null ? items.size() : 0) + " találat)", executionTimeMs);
    }

    public static PortalScrapeResult empty(String portalId, String portalName, String fallbackUrl, long executionTimeMs) {
        return new PortalScrapeResult(portalId, portalName, PortalScrapeStatus.EMPTY, Collections.emptyList(), fallbackUrl, "Nincs találat a megadott feltételekkel", executionTimeMs);
    }

    public static PortalScrapeResult botProtected(String portalId, String portalName, String fallbackUrl, long executionTimeMs) {
        return new PortalScrapeResult(portalId, portalName, PortalScrapeStatus.BOT_PROTECTED, Collections.emptyList(), fallbackUrl, "Cloudflare WAF bot-védelem aktív; közvetlen keresőlink biztosítva", executionTimeMs);
    }

    public static PortalScrapeResult spaFallback(String portalId, String portalName, String fallbackUrl, long executionTimeMs) {
        return new PortalScrapeResult(portalId, portalName, PortalScrapeStatus.SPA_FALLBACK, Collections.emptyList(), fallbackUrl, "Kliensoldali SPA alkalmazás; közvetlen portállink biztosítva", executionTimeMs);
    }

    public static PortalScrapeResult failure(String portalId, String portalName, String errorMsg, long executionTimeMs) {
        return new PortalScrapeResult(portalId, portalName, PortalScrapeStatus.FAILED, Collections.emptyList(), null, "Hiba a letöltés során: " + errorMsg, executionTimeMs);
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
}
