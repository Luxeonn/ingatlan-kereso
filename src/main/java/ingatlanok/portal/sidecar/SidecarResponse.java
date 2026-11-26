package ingatlanok.portal.sidecar;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import lombok.AllArgsConstructor;
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

    public IngatlanItem toIngatlanItem(
        PortalSource source, String city, String discoveredAt, boolean isNew) {
      PriceInfo priceInfo = buildPriceInfo();
      PropertyDetails detailsInfo = buildPropertyDetails(priceInfo);

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

    private PriceInfo buildPriceInfo() {
      if (priceMillion != null && priceMillion > 0) {
        long priceHuf = Math.round(priceMillion * 1_000_000L);
        NumberFormat nf = NumberFormat.getInstance(Locale.of("hu", "HU"));
        String formatted = nf.format(priceHuf) + " Ft";
        return PriceInfo.builder()
            .rawText(formatted)
            .amountHuf(priceHuf)
            .amountMillion(priceMillion)
            .build();
      }
      if (priceRaw != null && !priceRaw.isBlank()) {
        return PriceInfo.builder().rawText(priceRaw).build();
      }
      return PriceInfo.builder().rawText("Ár a hirdetésben").build();
    }

    private PropertyDetails buildPropertyDetails(PriceInfo priceInfo) {
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
      if (priceInfo.amountHuf() != null && areaSquareMeters != null && areaSquareMeters > 0) {
        sqmPrice = Math.round(priceInfo.amountHuf() / areaSquareMeters);
      }

      return PropertyDetails.builder()
          .rawText(detailsSb.toString())
          .areaSquareMeters(areaSquareMeters)
          .roomCount(roomCount)
          .pricePerSquareMeter(sqmPrice)
          .build();
    }
  }
}
