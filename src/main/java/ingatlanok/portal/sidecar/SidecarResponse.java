package ingatlanok.portal.sidecar;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class SidecarResponse implements Serializable {

  private String portalId;

  private SidecarStatus status = SidecarStatus.FAILED;

  private List<SidecarItem> items = new ArrayList<>();

  private String errorMessage;

  private long elapsedMs;

  private Integer pagesProcessed;

  private String stopReason;

  public boolean isSuccess() {
    return status == SidecarStatus.SUCCESS;
  }

  public SidecarStatus getStatus() {
    return status != null ? status : SidecarStatus.FAILED;
  }

  public void setStatus(SidecarStatus status) {
    this.status = status != null ? status : SidecarStatus.FAILED;
  }

  public void setStatus(String raw) {
    this.status = SidecarStatus.fromString(raw);
  }

  public List<SidecarItem> getItems() {
    return items != null ? items : new ArrayList<>();
  }

  public static class SidecarItem implements Serializable {

    private String id;

    private String url;

    private String title;

    private Double priceMillion;

    private String priceRaw;

    private Double areaSquareMeters;

    private Double roomCount;

    private String imageUrl;

    public SidecarItem() {}

    public SidecarItem(
        String id,
        String url,
        String title,
        Double priceMillion,
        String priceRaw,
        Double areaSquareMeters,
        Double roomCount) {
      this(id, url, title, priceMillion, priceRaw, areaSquareMeters, roomCount, null);
    }

    public SidecarItem(
        String id,
        String url,
        String title,
        Double priceMillion,
        String priceRaw,
        Double areaSquareMeters,
        Double roomCount,
        String imageUrl) {
      this.id = id;
      this.url = url;
      this.title = title;
      this.priceMillion = priceMillion;
      this.priceRaw = priceRaw;
      this.areaSquareMeters = areaSquareMeters;
      this.roomCount = roomCount;
      this.imageUrl = imageUrl;
    }

    public String getId() {
      return id;
    }

    public String getUrl() {
      return url;
    }

    public String getTitle() {
      return title;
    }

    public Double getPriceMillion() {
      return priceMillion;
    }

    public String getPriceRaw() {
      return priceRaw;
    }

    public Double getAreaSquareMeters() {
      return areaSquareMeters;
    }

    public Double getRoomCount() {
      return roomCount;
    }

    public String getImageUrl() {
      return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
      this.imageUrl = imageUrl;
    }
  }
}
