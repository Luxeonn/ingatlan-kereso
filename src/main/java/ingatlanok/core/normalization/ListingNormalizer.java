package ingatlanok.core.normalization;

import java.text.NumberFormat;
import java.util.Locale;
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

    boolean isNew = PropertyParser.isNewConstructionText(
        raw.title(),
        raw.descriptionText() != null ? raw.descriptionText() : raw.detailsText(),
        canonicalUrl
    );

    String imageUrl = raw.imageUrl() != null && !raw.imageUrl().isBlank()
        ? PortalSupport.resolveUrl(homepageUrl, raw.imageUrl())
        : null;

    return IngatlanItem.builder()
        .id(id)
        .source(source)
        .url(canonicalUrl)
        .title(title)
        .priceInfo(priceInfo)
        .city(city)
        .location(city)
        .detailsInfo(detailsInfo)
        .discoveredAt(time)
        .propertyType(resolvePropertyType(raw))
        .imageUrl(imageUrl)
        .newConstruction(isNew)
        .build();
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
      long priceHuf = priceInfo != null && priceInfo.getAmountHuf() != null ? priceInfo.getAmountHuf() : 0;
      long sqmPrice = area > 0 ? Math.round(priceHuf / area) : 0;
      String text = Math.round(area) + " m² • " + Math.round(rooms) + " szoba";
      return PropertyDetails.builder()
          .rawText(text)
          .areaSquareMeters(area)
          .roomCount(rooms)
          .pricePerSquareMeter(sqmPrice)
          .build();
    }

    PropertyDetails parsed = PropertyParser.parseDetails(raw.detailsText(), priceInfo);
    Double finalArea = area != null ? area : (parsed != null ? parsed.getAreaSquareMeters() : null);
    Double finalRooms = rooms != null ? rooms : (parsed != null ? parsed.getRoomCount() : null);

    long priceHuf = priceInfo != null && priceInfo.getAmountHuf() != null ? priceInfo.getAmountHuf() : 0;
    long sqmPrice = (finalArea != null && finalArea > 0) ? Math.round(priceHuf / finalArea) : 0;
    String text = parsed != null && parsed.getRawText() != null ? parsed.getRawText() : "";

    return PropertyDetails.builder()
        .rawText(text)
        .areaSquareMeters(finalArea)
        .roomCount(finalRooms)
        .pricePerSquareMeter(sqmPrice)
        .build();
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

    if (raw.extra() != null) {
      for (String key : new String[] {"propertyType", "category", "item_category", "type"}) {
        String val = raw.extra().get(key);
        if (val != null && !val.isBlank()) {
          PropertyType type = PropertyType.fromString(val);
          if (type != PropertyType.EGYEB_ISMERETLEN) {
            return type;
          }
        }
      }
    }

    String url = raw.url() != null ? raw.url().toLowerCase(Locale.ROOT) : "";
    if (!url.isBlank()) {
      PropertyType urlType = resolveTypeFromUrl(url);
      if (urlType != PropertyType.EGYEB_ISMERETLEN) {
        return urlType;
      }
    }

    String desc = raw.descriptionText() != null ? raw.descriptionText() : "";
    if (desc.contains("item_category")) {
      PropertyType ocType = extractOcCategory(desc);
      if (ocType != PropertyType.EGYEB_ISMERETLEN) {
        return ocType;
      }
    }

    String title = raw.title() != null ? raw.title() : "";
    String details = raw.detailsText() != null ? raw.detailsText() : "";
    String combined = title + " " + details;

    PropertyType compoundType = resolveTypeFromCompoundPatterns(combined);
    if (compoundType != PropertyType.EGYEB_ISMERETLEN) {
      return compoundType;
    }

    PropertyType titleWordType = resolveTypeFromWordPatterns(title);
    if (titleWordType != PropertyType.EGYEB_ISMERETLEN) {
      return titleWordType;
    }

    PropertyType detailsWordType = resolveTypeFromWordPatterns(details);
    if (detailsWordType != PropertyType.EGYEB_ISMERETLEN) {
      return detailsWordType;
    }

    return PropertyType.EGYEB_ISMERETLEN;
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
