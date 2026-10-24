package ingatlanok.model.raw;

import java.util.Map;

public record RawPropertyListing(
    String portalId,
    String nativeId,
    String url,
    String title,
    String priceText,
    Long priceHuf,
    String detailsText,
    Double areaSquareMeters,
    Double roomCount,
    String locationText,
    String imageUrl,
    String descriptionText,
    ExtractionMethod method,
    Map<String, String> extra
) {

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private String portalId;
    private String nativeId;
    private String url;
    private String title;
    private String priceText;
    private Long priceHuf;
    private String detailsText;
    private Double areaSquareMeters;
    private Double roomCount;
    private String locationText;
    private String imageUrl;
    private String descriptionText;
    private ExtractionMethod method;
    private Map<String, String> extra;

    public Builder portalId(String portalId) {
      this.portalId = portalId;
      return this;
    }

    public Builder nativeId(String nativeId) {
      this.nativeId = nativeId;
      return this;
    }

    public Builder url(String url) {
      this.url = url;
      return this;
    }

    public Builder title(String title) {
      this.title = title;
      return this;
    }

    public Builder priceText(String priceText) {
      this.priceText = priceText;
      return this;
    }

    public Builder priceHuf(Long priceHuf) {
      this.priceHuf = priceHuf;
      return this;
    }

    public Builder detailsText(String detailsText) {
      this.detailsText = detailsText;
      return this;
    }

    public Builder areaSquareMeters(Double areaSquareMeters) {
      this.areaSquareMeters = areaSquareMeters;
      return this;
    }

    public Builder roomCount(Double roomCount) {
      this.roomCount = roomCount;
      return this;
    }

    public Builder locationText(String locationText) {
      this.locationText = locationText;
      return this;
    }

    public Builder imageUrl(String imageUrl) {
      this.imageUrl = imageUrl;
      return this;
    }

    public Builder descriptionText(String descriptionText) {
      this.descriptionText = descriptionText;
      return this;
    }

    public Builder method(ExtractionMethod method) {
      this.method = method;
      return this;
    }

    public Builder extra(Map<String, String> extra) {
      this.extra = extra;
      return this;
    }

    public RawPropertyListing build() {
      return new RawPropertyListing(
          portalId,
          nativeId,
          url,
          title,
          priceText,
          priceHuf,
          detailsText,
          areaSquareMeters,
          roomCount,
          locationText,
          imageUrl,
          descriptionText,
          method,
          extra
      );
    }
  }
}
