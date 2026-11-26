package ingatlanok.portal.sidecar;

import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class SidecarResponse {

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

  @Getter
  @Setter
  @NoArgsConstructor
  @AllArgsConstructor
  public static class SidecarItem {

    private String id;

    private String url;

    private String title;

    private Double priceMillion;

    private String priceRaw;

    private Double areaSquareMeters;

    private Double roomCount;

    private String imageUrl;

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
  }
}
