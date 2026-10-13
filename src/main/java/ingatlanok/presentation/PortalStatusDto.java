package ingatlanok.presentation;

import java.io.Serializable;

public class PortalStatusDto implements Serializable {

    private String portalId;
    private String portalName;
    private String status;
    private int foundCount;
    private long latencyMs;
    private String fallbackUrl;
    private String message;

    public PortalStatusDto() {
    }

    public PortalStatusDto(String portalId, String portalName, String status, int foundCount, long latencyMs, String fallbackUrl, String message) {
        this.portalId = portalId;
        this.portalName = portalName;
        this.status = status;
        this.foundCount = foundCount;
        this.latencyMs = latencyMs;
        this.fallbackUrl = fallbackUrl;
        this.message = message;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getFoundCount() {
        return foundCount;
    }

    public void setFoundCount(int foundCount) {
        this.foundCount = foundCount;
    }

    public long getLatencyMs() {
        return latencyMs;
    }

    public void setLatencyMs(long latencyMs) {
        this.latencyMs = latencyMs;
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
}
