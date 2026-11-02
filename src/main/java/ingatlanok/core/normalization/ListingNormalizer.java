package ingatlanok.core.normalization;

import java.text.NumberFormat;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

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

public class ListingNormalizer {

  private static final Pattern OC_CATEGORY_PATTERN =
      Pattern.compile("\"item_category\"\\s*:\\s*\"([^\"]+)\"");

  private static final Pattern GARAGE_COMPOUND_PATTERN =
      Pattern.compile(
          "(?iuU)\\b(önálló\\s*garázs|onallo\\s*garazs|teremgarázs|teremgarazs"
              + "|felszíni\\s*beálló(?!val|vel)|felszini\\s*beallo(?!val|vel)"
              + "|gépkocsibeálló(?!val|vel)|gepkocsibeallo(?!val|vel)"
              + "|parkolóhely|parkolohely|duplagarázs|duplagarazs)\\b");

  private static final Pattern COMMERCIAL_COMPOUND_PATTERN =
      Pattern.compile(
          "(?iuU)\\b(ipari\\s*ingatlan|ipari\\s*csarnok|ipari\\s*telephely|ipari\\s*park"
              + "|raktárépület|raktarepulet|raktárbázis|raktarbazis|üzlethelyiség|uzlethelyiseg"
              + "|irodahelyiség|irodahelyiseg|irodaház|irodahaz|gyárépület|gyarepulet|műhely|muhely)\\b");

  private static final Pattern PLOT_COMPOUND_PATTERN =
      Pattern.compile(
          "(?iuU)\\b(építési\\s*telek|epitesi\\s*telek|zártkert|zartkert"
              + "|külterület|kulterulet|szántó|szanto|fejlesztési\\s*terület|fejlesztesi\\s*terulet"
              + "|ipari\\s*telek|lakóövezeti\\s*telek|lakoovezeti\\s*telek)\\b");

  private static final Pattern HOUSE_COMPOUND_PATTERN =
      Pattern.compile(
          "(?iuU)\\b(családi\\s*ház|csaladi\\s*haz|ikerház|ikerhaz|sorház|sorhaz"
              + "|házrész|hazresz|kertes\\s*ház|kertes\\s*haz|parasztház|paraszthaz|nyaraló|nyaralo"
              + "|üdülő|udulo|üdülőház|udulohaz"
              + "|tanya|villa|hétvégi\\s*ház|hetvegi\\s*haz)\\b");

  private static final Pattern FLAT_COMPOUND_PATTERN =
      Pattern.compile(
          "(?iuU)\\b(társasházi\\s*lakás|tarsashazi\\s*lakas|tégla\\s*lakás|tegla\\s*lakas"
              + "|téglalakás|teglalakas|panellakás|panellakas|garzonlakás|garzon"
              + "|penthouse|öröklakás|oroklakas|tetőtéri\\s*lakás|tetoteri\\s*lakas|duplex\\s*lakás)\\b");

  private static final Pattern FLAT_WORD_PATTERN =
      Pattern.compile("(?iuU)\\b(lakás|lakas|lakások|lakasok|garzon|penthouse|loft)\\b");

  private static final Pattern HOUSE_WORD_PATTERN =
      Pattern.compile(
          "(?iuU)(?<!társas)(?<!tarsas)\\b(ház|haz|házak|hazak|üdülő|udulo|üdülőház|udulohaz)\\b");

  private static final Pattern PLOT_WORD_PATTERN =
      Pattern.compile("(?iuU)\\b(telek|telkek)\\b(?!kel|lel)");

  private static final Pattern GARAGE_WORD_PATTERN =
      Pattern.compile("(?iuU)\\b(garázs|garazs|garázsok|garazsok)\\b(?!zal|zel|zsal|zsel)");

  private static final Pattern COMMERCIAL_WORD_PATTERN =
      Pattern.compile("(?iuU)\\b(ipari|raktár|raktar|üzlet|uzlet|iroda|csarnok|telephely|műhely|muhely)\\b");

  private static final Pattern URL_GARAGE_PATTERN =
      Pattern.compile("(?iuU)([-+/_:;]elado[-+])?garazs([-_+/:;]|$)");

  private static final Pattern URL_COMMERCIAL_PATTERN =
      Pattern.compile("(?iuU)([-+/_:;]elado[-+])?(ipari|iroda|raktar)([-_+/:;]|$)");

  private static final Pattern URL_PLOT_PATTERN =
      Pattern.compile("(?iuU)([-+/_:;]elado[-+])?telek([-_+/:;]|$)");

  private static final Pattern URL_HOUSE_PATTERN =
      Pattern.compile("(?iuU)([-+/_:;]elado[-+])?haz([-_+/:;]|$)");

  private static final Pattern URL_FLAT_PATTERN =
      Pattern.compile("(?iuU)([-+/_:;]elado[-+])?lakas([-_+/:;]|$)");

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
        .location(city)
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
      long sqmPrice = calculateSqmPrice(priceInfo, area);
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
    Double finalArea = area != null ? area : (parsed != null ? parsed.getAreaSquareMeters() : null);
    Double finalRooms = rooms != null ? rooms : (parsed != null ? parsed.getRoomCount() : null);
    long sqmPrice = calculateSqmPrice(priceInfo, finalArea);
    String text = parsed != null && parsed.getRawText() != null ? parsed.getRawText() : "";
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

  private long calculateSqmPrice(PriceInfo priceInfo, Double area) {
    if (area == null || area <= 0 || priceInfo == null || priceInfo.getAmountHuf() == null) {
      return 0;
    }
    return Math.round(priceInfo.getAmountHuf() / area);
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

  public PropertyType resolvePropertyType(RawPropertyListing raw) {
    if (raw == null) {
      return PropertyType.EGYEB_ISMERETLEN;
    }
    PropertyType fromExtra = resolveTypeFromExtra(raw.extra());
    if (fromExtra != PropertyType.EGYEB_ISMERETLEN) {
      return fromExtra;
    }
    PropertyType fromUrl = resolveTypeFromUrlString(raw.url());
    if (fromUrl != PropertyType.EGYEB_ISMERETLEN) {
      return fromUrl;
    }
    PropertyType fromDesc = resolveTypeFromDescription(raw.descriptionText());
    if (fromDesc != PropertyType.EGYEB_ISMERETLEN) {
      return fromDesc;
    }
    return resolveTypeFromText(raw.title(), raw.detailsText());
  }

  private PropertyType resolveTypeFromExtra(Map<String, String> extra) {
    if (extra == null) {
      return PropertyType.EGYEB_ISMERETLEN;
    }
    for (String key : new String[] {"propertyType", "category", "item_category", "type"}) {
      String val = extra.get(key);
      if (val != null && !val.isBlank()) {
        PropertyType type = PropertyType.fromString(val);
        if (type != PropertyType.EGYEB_ISMERETLEN) {
          return type;
        }
      }
    }
    return PropertyType.EGYEB_ISMERETLEN;
  }

  private PropertyType resolveTypeFromUrlString(String rawUrl) {
    if (rawUrl == null || rawUrl.isBlank()) {
      return PropertyType.EGYEB_ISMERETLEN;
    }
    return resolveTypeFromUrl(rawUrl.toLowerCase(Locale.ROOT));
  }

  private PropertyType resolveTypeFromDescription(String desc) {
    if (desc != null && desc.contains("item_category")) {
      PropertyType ocType = extractOcCategory(desc);
      if (ocType != PropertyType.EGYEB_ISMERETLEN) {
        return ocType;
      }
    }
    return PropertyType.EGYEB_ISMERETLEN;
  }

  private PropertyType resolveTypeFromText(String title, String details) {
    String t = title != null ? title : "";
    String d = details != null ? details : "";
    String combined = t + " " + d;

    PropertyType compoundType = resolveTypeFromCompoundPatterns(combined);
    if (compoundType != PropertyType.EGYEB_ISMERETLEN) {
      return compoundType;
    }
    PropertyType titleWordType = resolveTypeFromWordPatterns(t);
    if (titleWordType != PropertyType.EGYEB_ISMERETLEN) {
      return titleWordType;
    }
    return resolveTypeFromWordPatterns(d);
  }

  private PropertyType resolveTypeFromUrl(String url) {
    if (url.contains("property__2=4") || URL_GARAGE_PATTERN.matcher(url).find()) {
      return PropertyType.GARAZS;
    }
    if (URL_COMMERCIAL_PATTERN.matcher(url).find()) {
      return PropertyType.IPARI_KERESKEDELMI;
    }
    if (url.contains("property__2=5") || URL_PLOT_PATTERN.matcher(url).find()) {
      return PropertyType.TELEK;
    }
    if (url.contains("property__2=2") || URL_HOUSE_PATTERN.matcher(url).find()) {
      return PropertyType.HAZ;
    }
    if (url.contains("property__2=3") || URL_FLAT_PATTERN.matcher(url).find()) {
      return PropertyType.LAKAS;
    }
    return PropertyType.EGYEB_ISMERETLEN;
  }

  private PropertyType extractOcCategory(String desc) {
    var matcher = OC_CATEGORY_PATTERN.matcher(desc);
    if (matcher.find()) {
      String cat = matcher.group(1);
      PropertyType type = PropertyType.fromString(cat);
      if (type != PropertyType.EGYEB_ISMERETLEN) {
        return type;
      }
      return resolveTypeFromCompoundPatterns(cat);
    }
    return PropertyType.EGYEB_ISMERETLEN;
  }

  private PropertyType resolveTypeFromCompoundPatterns(String text) {
    if (text == null || text.isBlank()) {
      return PropertyType.EGYEB_ISMERETLEN;
    }
    if (HOUSE_COMPOUND_PATTERN.matcher(text).find()) {
      return PropertyType.HAZ;
    }
    if (FLAT_COMPOUND_PATTERN.matcher(text).find()) {
      return PropertyType.LAKAS;
    }
    if (COMMERCIAL_COMPOUND_PATTERN.matcher(text).find()) {
      return PropertyType.IPARI_KERESKEDELMI;
    }
    if (PLOT_COMPOUND_PATTERN.matcher(text).find()) {
      return PropertyType.TELEK;
    }
    if (GARAGE_COMPOUND_PATTERN.matcher(text).find()) {
      return PropertyType.GARAZS;
    }
    return PropertyType.EGYEB_ISMERETLEN;
  }

  private PropertyType resolveTypeFromWordPatterns(String text) {
    if (text == null || text.isBlank()) {
      return PropertyType.EGYEB_ISMERETLEN;
    }
    if (FLAT_WORD_PATTERN.matcher(text).find()) {
      return PropertyType.LAKAS;
    }
    if (HOUSE_WORD_PATTERN.matcher(text).find()) {
      return PropertyType.HAZ;
    }
    if (PLOT_WORD_PATTERN.matcher(text).find()) {
      return PropertyType.TELEK;
    }
    if (GARAGE_WORD_PATTERN.matcher(text).find()) {
      return PropertyType.GARAZS;
    }
    if (COMMERCIAL_WORD_PATTERN.matcher(text).find()) {
      return PropertyType.IPARI_KERESKEDELMI;
    }
    return PropertyType.EGYEB_ISMERETLEN;
  }

  public ListingValidator getValidator() {
    return validator;
  }
}
