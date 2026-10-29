package ingatlanok.portal.sidecar;

import java.io.Serializable;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import ingatlanok.model.IngatlanItem;
import ingatlanok.model.PortalSource;
import ingatlanok.model.PriceInfo;
import ingatlanok.model.PropertyDetails;

@Getter
@Setter
@NoArgsConstructor
public class SidecarResponse implements Serializable {

  private String portalId;

  private String status;

  private List<SidecarItem> items = new ArrayList<>();

  private String errorMessage;

  private long elapsedMs;

  public boolean isSuccess() {
    return "SUCCESS".equalsIgnoreCase(status);
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

    public IngatlanItem toIngatlanItem(
        PortalSource source, String city, String discoveredAt, boolean isNew) {
      PriceInfo priceInfo;
      if (priceMillion != null && priceMillion > 0) {
        long priceHuf = Math.round(priceMillion * 1_000_000L);
        NumberFormat nf = NumberFormat.getInstance(new Locale("hu", "HU"));
        String formatted = nf.format(priceHuf) + " Ft";
        priceInfo =
            PriceInfo.builder()
                .rawText(formatted)
                .amountHuf(priceHuf)
                .amountMillion(priceMillion)
                .build();
      } else if (priceRaw != null && !priceRaw.isBlank()) {
        priceInfo = PriceInfo.builder().rawText(priceRaw).build();
      } else {
        priceInfo = PriceInfo.builder().rawText("Ár a hirdetésben").build();
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
          PropertyDetails.builder()
              .rawText(detailsSb.toString())
              .areaSquareMeters(areaSquareMeters)
              .roomCount(roomCount)
              .pricePerSquareMeter(sqmPrice)
              .build();

      String displayTitle =
          (title != null && !title.isBlank()) ? title : (city + " - Ingatlan (" + id + ")");

      return IngatlanItem.builder()
          .id(id)
          .source(source)
          .url(url)
          .title(displayTitle)
          .priceInfo(priceInfo)
          .city(city)
          .location(city)
          .detailsInfo(detailsInfo)
          .discoveredAt(discoveredAt)
          .isNew(isNew)
          .imageUrl(imageUrl)
          .build();
    }
  }
}
