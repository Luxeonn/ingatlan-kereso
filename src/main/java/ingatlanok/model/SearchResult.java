package ingatlanok.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Keresési futtatási eredmény a webes felület felé.
 */
public class SearchResult implements Serializable {

    private List<IngatlanItem> items = new ArrayList<>();
    private int newCount;
    private int totalFound;
    private String executedAt;
    private Map<String, Integer> portalCounts = new HashMap<>();

    public SearchResult() {
    }

    public List<IngatlanItem> getItems() {
        return items;
    }

    public void setItems(List<IngatlanItem> items) {
        this.items = items;
    }

    public int getNewCount() {
        return newCount;
    }

    public void setNewCount(int newCount) {
        this.newCount = newCount;
    }

    public int getTotalFound() {
        return totalFound;
    }

    public void setTotalFound(int totalFound) {
        this.totalFound = totalFound;
    }

    public String getExecutedAt() {
        return executedAt;
    }

    public void setExecutedAt(String executedAt) {
        this.executedAt = executedAt;
    }

    public Map<String, Integer> getPortalCounts() {
        return portalCounts;
    }

    public void setPortalCounts(Map<String, Integer> portalCounts) {
        this.portalCounts = portalCounts;
    }
}
