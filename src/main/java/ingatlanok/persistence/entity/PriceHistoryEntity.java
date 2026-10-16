package ingatlanok.persistence.entity;

import jakarta.persistence.*;
import java.io.Serializable;

/**
 * Idősoros árváltozási rekord JPA entitás.
 */
@Entity
@Table(name = "price_history", indexes = {
    @Index(name = "idx_price_history_prop_id", columnList = "propertyId"),
    @Index(name = "idx_price_history_url", columnList = "url")
})
public class PriceHistoryEntity implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String propertyId;

    @Column(length = 2048)
    private String url;

    @Column(length = 50)
    private String recordedAt;

    @Column(length = 255)
    private String priceRaw;

    private Long priceAmount;

    private Double priceMillions;

    private Long pricePerSqm;

    private Long previousPriceAmount;

    private Long priceDelta;

    private Double changePercent;

    public PriceHistoryEntity() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPropertyId() {
        return propertyId;
    }

    public void setPropertyId(String propertyId) {
        this.propertyId = propertyId;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getRecordedAt() {
        return recordedAt;
    }

    public void setRecordedAt(String recordedAt) {
        this.recordedAt = recordedAt;
    }

    public String getPriceRaw() {
        return priceRaw;
    }

    public void setPriceRaw(String priceRaw) {
        this.priceRaw = priceRaw;
    }

    public Long getPriceAmount() {
        return priceAmount;
    }

    public void setPriceAmount(Long priceAmount) {
        this.priceAmount = priceAmount;
    }

    public Double getPriceMillions() {
        return priceMillions;
    }

    public void setPriceMillions(Double priceMillions) {
        this.priceMillions = priceMillions;
    }

    public Long getPricePerSqm() {
        return pricePerSqm;
    }

    public void setPricePerSqm(Long pricePerSqm) {
        this.pricePerSqm = pricePerSqm;
    }

    public Long getPreviousPriceAmount() {
        return previousPriceAmount;
    }

    public void setPreviousPriceAmount(Long previousPriceAmount) {
        this.previousPriceAmount = previousPriceAmount;
    }

    public Long getPriceDelta() {
        return priceDelta;
    }

    public void setPriceDelta(Long priceDelta) {
        this.priceDelta = priceDelta;
    }

    public Double getChangePercent() {
        return changePercent;
    }

    public void setChangePercent(Double changePercent) {
        this.changePercent = changePercent;
    }
}
