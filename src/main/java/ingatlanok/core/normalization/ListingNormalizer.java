package ingatlanok.core.normalization;

import java.text.NumberFormat;
import java.util.Locale;

import ingatlanok.common.PropertyParser;
import ingatlanok.model.IngatlanItem;
import ingatlanok.model.PortalSource;
import ingatlanok.model.PriceInfo;
import ingatlanok.model.PropertyDetails;
import ingatlanok.model.SearchCriteria;
import ingatlanok.model.raw.RawPropertyListing;
import ingatlanok.portal.PortalDescriptor;
import ingatlanok.portal.PortalSupport;

public class ListingNormalizer {

  private final ListingValidator validator;

  public ListingNormalizer() {
    this(new ListingValidator());
  }

  public ListingNormalizer(ListingValidator validator) {
    this.validator = validator != null ? validator : new ListingValidator();
  }

  public IngatlanItem normalize(
      RawPropertyListing raw,
      PortalDescriptor descriptor,
      SearchCriteria criteria,
      String executionTime) {
    if (raw == null || !validator.isValid(raw, descriptor)) {
      return null;
    }

    String homepageUrl = descriptor != null ? descriptor.homepageUrl() : "";
    String canonicalUrl = PortalSupport.resolveUrl(homepageUrl, raw.url());

    String id;
    if (raw.nativeId() != null && !raw.nativeId().isBlank()) {
      id = raw.nativeId().trim();
    } else {
      id = Integer.toUnsignedString(canonicalUrl.hashCode());
    }

    PriceInfo priceInfo = resolvePriceInfo(raw);
    PropertyDetails detailsInfo = resolveDetailsInfo(raw, priceInfo);

    String city = resolveCity(raw, criteria);

    String title = raw.title() != null && !raw.title().isBlank()
        ? raw.title().trim()
        : city + " - Eladó ingatlan (" + id + ")";

    String time = executionTime != null && !executionTime.isBlank()
        ? executionTime
        : PortalSupport.nowTimestamp();

    PortalSource source = descriptor != null
        ? PortalSource.of(descriptor.name())
        : PortalSource.of(raw.portalId() != null ? raw.portalId() : "Unknown");

    IngatlanItem item = new IngatlanItem(
        id,
        source,
        canonicalUrl,
        title,
        priceInfo,
        city,
        detailsInfo,
        time,
        false
    );

    item.setLocation(city);

    if (raw.imageUrl() != null && !raw.imageUrl().isBlank()) {
      item.setImageUrl(PortalSupport.resolveUrl(homepageUrl, raw.imageUrl()));
    }

    boolean isNew = PropertyParser.isNewConstructionText(
        raw.title(),
        raw.descriptionText() != null ? raw.descriptionText() : raw.detailsText(),
        canonicalUrl
    );
    item.setNewConstruction(isNew);

    return item;
  }

  private PriceInfo resolvePriceInfo(RawPropertyListing raw) {
    if (raw.priceHuf() != null && raw.priceHuf() > 0) {
      long huf = raw.priceHuf();
      double million = Math.round((huf / 1_000_000.0) * 100.0) / 100.0;
      NumberFormat nf = NumberFormat.getIntegerInstance(Locale.of("hu", "HU"));
      String formattedPrice = nf.format(huf) + " Ft";
      return new PriceInfo(formattedPrice, huf, million);
    }
    return PropertyParser.parsePrice(raw.priceText());
  }

  private PropertyDetails resolveDetailsInfo(RawPropertyListing raw, PriceInfo priceInfo) {
    Double area = raw.areaSquareMeters();
    Double rooms = raw.roomCount();

    if (area != null && rooms != null) {
      long priceHuf = priceInfo != null && priceInfo.getAmountHuf() != null ? priceInfo.getAmountHuf() : 0;
      long sqmPrice = area > 0 ? Math.round(priceHuf / area) : 0;
      String text = Math.round(area) + " m² • " + Math.round(rooms) + " szoba";
      return new PropertyDetails(text, area, rooms, sqmPrice);
    }

    PropertyDetails parsed = PropertyParser.parseDetails(raw.detailsText(), priceInfo);
    Double finalArea = area != null ? area : (parsed != null ? parsed.getAreaSquareMeters() : null);
    Double finalRooms = rooms != null ? rooms : (parsed != null ? parsed.getRoomCount() : null);

    long priceHuf = priceInfo != null && priceInfo.getAmountHuf() != null ? priceInfo.getAmountHuf() : 0;
    long sqmPrice = (finalArea != null && finalArea > 0) ? Math.round(priceHuf / finalArea) : 0;
    String text = parsed != null && parsed.getRawText() != null ? parsed.getRawText() : "";

    return new PropertyDetails(text, finalArea, finalRooms, sqmPrice);
  }

  private String resolveCity(RawPropertyListing raw, SearchCriteria criteria) {
    if (raw.locationText() != null && !raw.locationText().isBlank()) {
      String extracted = PropertyParser.extractCityFromPostalAddress(raw.locationText());
      if (extracted != null && !extracted.isBlank()) {
        return extracted;
      }
      return raw.locationText().trim();
    }
    return PortalSupport.resolveDefaultCity(criteria);
  }

  public ListingValidator getValidator() {
    return validator;
  }
}
