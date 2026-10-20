package ingatlanok.portal.sidecar;

import java.io.Serializable;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import ingatlanok.model.IngatlanItem;
import ingatlanok.model.PortalSource;
import ingatlanok.model.PriceInfo;
import ingatlanok.model.PropertyDetails;

public class SidecarResponse implements Serializable {

  private String portalId;

  private String status;

  private List<SidecarItem> items = new ArrayList<>();

  private String errorMessage;

  private long elapsedMs;

  public SidecarResponse() {}

  public boolean isSuccess() {
    return "SUCCESS".equalsIgnoreCase(status);
  }

  public String getPortalId() {
    return portalId;
  }

  public void setPortalId(String portalId) {
    this.portalId = portalId;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public List<SidecarItem> getItems() {
    return items != null ? items : new ArrayList<>();
  }

  public void setItems(List<SidecarItem> items) {
    this.items = items;
  }

  public String getErrorMessage() {
    return errorMessage;
  }

  public void setErrorMessage(String errorMessage) {
    this.errorMessage = errorMessage;
  }

  public long getElapsedMs() {
    return elapsedMs;
  }

  public void setElapsedMs(long elapsedMs) {
    this.elapsedMs = elapsedMs;
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

    public IngatlanItem toIngatlanItem(
        PortalSource source, String city, String discoveredAt, boolean isNew) {
      PriceInfo priceInfo;
      if (priceMillion != null && priceMillion > 0) {
        long priceHuf = Math.round(priceMillion * 1_000_000L);
        NumberFormat nf = NumberFormat.getInstance(new Locale("hu", "HU"));
        String formatted = nf.format(priceHuf) + " Ft";
        priceInfo = new PriceInfo(formatted, priceHuf, priceMillion);
      } else if (priceRaw != null && !priceRaw.isBlank()) {
        priceInfo = new PriceInfo(priceRaw, null, null);
      } else {
        priceInfo = new PriceInfo("Ár a hirdetésben", null, null);
      }

      StringBuilder detailsSb = new StringBuilder();
      if (areaSquareMeters != null && areaSquareMeters > 0) {
        detailsSb.append(Math.round(areaSquareMeters)).append(" m²");
      }
      if (roomCount != null && roomCount > 0) {
        if (!detailsSb.isEmpty()) {
          detailsSb.append(" • ");
        }
        if (roomCount % 1 == 0) {
          detailsSb.append(roomCount.intValue()).append(" szoba");
        } else {
          detailsSb.append(roomCount).append(" szoba");
        }
      }

      Long sqmPrice = null;
      if (priceInfo.getAmountHuf() != null && areaSquareMeters != null && areaSquareMeters > 0) {
        sqmPrice = Math.round(priceInfo.getAmountHuf() / areaSquareMeters);
      }

      PropertyDetails detailsInfo =
          new PropertyDetails(detailsSb.toString(), areaSquareMeters, roomCount, sqmPrice);

      String displayTitle =
          (title != null && !title.isBlank()) ? title : (city + " - Ingatlan (" + id + ")");

      IngatlanItem item =
          new IngatlanItem(
              id, source, url, displayTitle, priceInfo, city, detailsInfo, discoveredAt, isNew);
      item.setLocation(city);
      item.setImageUrl(imageUrl);
      return item;
    }
  }
}
