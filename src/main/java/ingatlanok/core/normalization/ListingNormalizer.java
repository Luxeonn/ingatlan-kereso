package ingatlanok.core.normalization;

import java.text.NumberFormat;
import java.util.Locale;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ingatlanok.common.PropertyParser;
import ingatlanok.model.IngatlanItem;
import ingatlanok.model.PortalSource;
import ingatlanok.model.PriceInfo;
import ingatlanok.model.PropertyDetails;
import ingatlanok.model.PropertyType;
import ingatlanok.model.SearchCriteria;
import ingatlanok.model.raw.RawPropertyListing;
import ingatlanok.portal.PortalDescriptor;
import ingatlanok.portal.PortalSupport;
import ingatlanok.portal.RawListingNormalizer;

@Service
public class ListingNormalizer implements RawListingNormalizer {

  private final ListingValidator validator;

  public ListingNormalizer() {
    this(new ListingValidator());
  }

  @Autowired
  public ListingNormalizer(ListingValidator validator) {
    this.validator = validator != null ? validator : new ListingValidator();
  }

  @PostConstruct
  void registerAsDefaultNormalizer() {
    RawListingNormalizer.setDefault(this);
  }

  @Override
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
    String id = resolveId(raw, canonicalUrl);
    String city = resolveCity(raw, criteria);
    PriceInfo priceInfo = resolvePriceInfo(raw);

    return IngatlanItem.builder()
        .id(id)
        .source(resolvePortalSource(descriptor, raw))
        .url(canonicalUrl)
        .title(resolveTitle(raw, city, id))
        .priceInfo(priceInfo)
        .city(city)
        .location(resolveLocation(raw, city))
        .detailsInfo(resolveDetailsInfo(raw, priceInfo))
        .discoveredAt(resolveTime(executionTime))
        .propertyType(resolvePropertyType(raw))
        .imageUrl(resolveImageUrl(raw, homepageUrl))
        .newConstruction(resolveIsNew(raw, canonicalUrl))
        .build();
  }

  private String resolveId(RawPropertyListing raw, String canonicalUrl) {
    if (raw.nativeId() != null && !raw.nativeId().isBlank()) {
      return raw.nativeId().trim();
    }
    return Integer.toUnsignedString(canonicalUrl.hashCode());
  }

  private String resolveTitle(RawPropertyListing raw, String city, String id) {
    if (raw.title() != null && !raw.title().isBlank()) {
      return raw.title().trim();
    }
    return city + " - Eladó ingatlan (" + id + ")";
  }

  private String resolveTime(String executionTime) {
    if (executionTime != null && !executionTime.isBlank()) {
      return executionTime;
    }
    return PortalSupport.nowTimestamp();
  }

  private PortalSource resolvePortalSource(PortalDescriptor descriptor, RawPropertyListing raw) {
    if (descriptor != null) {
      return PortalSource.of(descriptor.name());
    }
    String name = raw.portalId() != null ? raw.portalId() : "Unknown";
    return PortalSource.of(name);
  }

  private String resolveImageUrl(RawPropertyListing raw, String homepageUrl) {
    if (raw.imageUrl() != null && !raw.imageUrl().isBlank()) {
      return PortalSupport.resolveUrl(homepageUrl, raw.imageUrl());
    }
    return null;
  }

  private boolean resolveIsNew(RawPropertyListing raw, String canonicalUrl) {
    String descOrDetails = raw.descriptionText() != null ? raw.descriptionText() : raw.detailsText();
    return PropertyParser.isNewConstructionText(raw.title(), descOrDetails, canonicalUrl);
  }

  private PriceInfo resolvePriceInfo(RawPropertyListing raw) {
    if (raw.priceHuf() != null && raw.priceHuf() > 0) {
      long huf = raw.priceHuf();
      double million = Math.round((huf / 1_000_000.0) * 100.0) / 100.0;
      NumberFormat nf = NumberFormat.getIntegerInstance(Locale.of("hu", "HU"));
      String formattedPrice = nf.format(huf) + " Ft";
      return PriceInfo.builder().rawText(formattedPrice).amountHuf(huf).amountMillion(million).build();
    }
    return PropertyParser.parsePrice(raw.priceText());
  }

  private PropertyDetails resolveDetailsInfo(RawPropertyListing raw, PriceInfo priceInfo) {
    Double area = raw.areaSquareMeters();
    Double rooms = raw.roomCount();
    if (area != null && rooms != null) {
      Long sqmPrice = calculateSqmPrice(priceInfo, area);
      String text = Math.round(area) + " m² • " + Math.round(rooms) + " szoba";
      return PropertyDetails.builder()
          .rawText(text)
          .areaSquareMeters(area)
          .roomCount(rooms)
          .pricePerSquareMeter(sqmPrice)
          .build();
    }
    return parseDetailsInfo(raw, priceInfo, area, rooms);
  }

  private PropertyDetails parseDetailsInfo(
      RawPropertyListing raw, PriceInfo priceInfo, Double area, Double rooms) {
    PropertyDetails parsed = PropertyParser.parseDetails(raw.detailsText(), priceInfo);
    Double finalArea = area != null ? area : (parsed != null ? parsed.areaSquareMeters() : null);
    Double finalRooms = rooms != null ? rooms : (parsed != null ? parsed.roomCount() : null);
    Long sqmPrice = calculateSqmPrice(priceInfo, finalArea);
    String text = parsed != null && parsed.rawText() != null ? parsed.rawText() : "";
    if (text.isBlank()) {
      if (finalArea != null && finalRooms != null) {
        text = Math.round(finalArea) + " m² • " + Math.round(finalRooms) + " szoba";
      } else if (finalArea != null) {
        text = Math.round(finalArea) + " m²";
      } else if (finalRooms != null) {
        text = Math.round(finalRooms) + " szoba";
      }
    }
    return PropertyDetails.builder()
        .rawText(text)
        .areaSquareMeters(finalArea)
        .roomCount(finalRooms)
        .pricePerSquareMeter(sqmPrice)
        .build();
  }

  private Long calculateSqmPrice(PriceInfo priceInfo, Double area) {
    if (area == null || area <= 0 || priceInfo == null
        || priceInfo.amountHuf() == null || priceInfo.amountHuf() <= 0) {
      return null;
    }
    return Math.round((double) priceInfo.amountHuf() / area);
  }

  private String resolveCity(RawPropertyListing raw, SearchCriteria criteria) {
    if (raw.locationText() != null && !raw.locationText().isBlank()) {
      String rawLocation = raw.locationText().trim();
      String targetCity = criteria != null && criteria.getCity() != null
          ? criteria.getCity().trim()
          : "";
      if (rawLocation.contains(",")) {
        return extractCityFromCommaLocation(rawLocation, targetCity);
      }
      String extracted = PropertyParser.extractCityFromPostalAddress(rawLocation);
      if (extracted != null && !extracted.isBlank()) {
        return extracted;
      }
      return rawLocation;
    }
    return PortalSupport.resolveDefaultCity(criteria);
  }

  private String extractCityFromCommaLocation(String rawLocation, String targetCity) {
    String[] parts = rawLocation.split(",");
    String firstCandidate = "";
    for (String part : parts) {
      String trimmed = part != null ? part.trim() : "";
      if (trimmed.isBlank()) {
        continue;
      }
      String postalCity = PropertyParser.extractCityFromPostalAddress(trimmed);
      String candidate = postalCity != null && !postalCity.isBlank() ? postalCity : trimmed;
      if (firstCandidate.isBlank()) {
        firstCandidate = candidate;
      }
      if (!targetCity.isBlank() && candidate.equalsIgnoreCase(targetCity)) {
        return candidate;
      }
    }
    return firstCandidate.isBlank() ? rawLocation : firstCandidate;
  }

  private String resolveLocation(RawPropertyListing raw, String city) {
    if (raw != null && raw.locationText() != null && !raw.locationText().isBlank()) {
      return raw.locationText().trim();
    }
    return city;
  }

  public PropertyType resolvePropertyType(RawPropertyListing raw) {
    return PropertyTypeClassifier.resolve(raw);
  }

  public ListingValidator getValidator() {
    return validator;
  }
}
