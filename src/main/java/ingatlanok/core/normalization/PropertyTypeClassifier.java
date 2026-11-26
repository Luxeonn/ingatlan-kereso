package ingatlanok.core.normalization;

import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

import ingatlanok.model.PropertyType;
import ingatlanok.model.raw.RawPropertyListing;

/**
 * Classifies {@link RawPropertyListing} entries into canonical {@link PropertyType} values
 * using structured metadata, URL signals, OC JSON categories, and Hungarian compound/word regexes.
 */
final class PropertyTypeClassifier {

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
          "(?iuU)\\b(családi\\s*h(?:áz|á)?|csaladi\\s*h(?:az|a)?|ikerh(?:áz|az|á|a)?"
              + "|sorh(?:áz|az|á|a)?|házrész|hazresz|kertes\\s*h(?:áz|á)?|kertes\\s*h(?:az|a)?"
              + "|paraszth(?:áz|az|á|a)?|nyaraló|nyaralo|üdülő|udulo"
              + "|üdülőh(?:áz|á)?|uduloh(?:az|a)?"
              + "|tanya|villa|hétvégi\\s*h(?:áz|á)?|hetvegi\\s*h(?:az|a)?)\\b");

  private static final Pattern FLAT_COMPOUND_PATTERN =
      Pattern.compile(
          "(?iuU)\\b(társasházi\\s*lak(?:ás|á)?|tarsashazi\\s*lak(?:as|a)?"
              + "|tégla\\s*lak(?:ás|á)?|tegla\\s*lak(?:as|a)?|téglalak(?:ás|á)?|teglalak(?:as|a)?"
              + "|panellak(?:ás|as|á|a)?|panel\\s*lak(?:ás|as|á|a)?|garzonlak(?:ás|as|á|a)?|garzon"
              + "|penthouse|öröklak(?:ás|á)?|oroklak(?:as|a)?"
              + "|tetőtéri\\s*lak(?:ás|á)?|tetoteri\\s*lak(?:as|a)?|duplex\\s*lak(?:ás|as|á|a)?)\\b");

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

  private PropertyTypeClassifier() {}

  static PropertyType resolve(RawPropertyListing raw) {
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

  private static PropertyType resolveTypeFromExtra(Map<String, String> extra) {
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

  private static PropertyType resolveTypeFromUrlString(String rawUrl) {
    if (rawUrl == null || rawUrl.isBlank()) {
      return PropertyType.EGYEB_ISMERETLEN;
    }
    return resolveTypeFromUrl(rawUrl.toLowerCase(Locale.ROOT));
  }

  private static PropertyType resolveTypeFromDescription(String desc) {
    if (desc != null && desc.contains("item_category")) {
      PropertyType ocType = extractOcCategory(desc);
      if (ocType != PropertyType.EGYEB_ISMERETLEN) {
        return ocType;
      }
    }
    return PropertyType.EGYEB_ISMERETLEN;
  }

  private static PropertyType resolveTypeFromText(String title, String details) {
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

  private static PropertyType resolveTypeFromUrl(String url) {
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

  private static PropertyType extractOcCategory(String desc) {
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

  private static PropertyType resolveTypeFromCompoundPatterns(String text) {
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

  private static PropertyType resolveTypeFromWordPatterns(String text) {
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
}
