package ingatlanok.common;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ingatlanok.model.PriceInfo;
import ingatlanok.model.PropertyDetails;

public final class PropertyParser {

  private static final Pattern PRICE_PATTERN =
      Pattern.compile("(\\d+[,.]?\\d*\\s*M\\s*Ft|\\d[\\d\\s]*\\s*Ft)", Pattern.CASE_INSENSITIVE);
  private static final Pattern DETAILS_PATTERN =
      Pattern.compile(
          "(\\d+[,.]?\\d*\\s*m²[^\\n"
              + "•|]*\\d+\\s*szoba|\\d+\\s*szoba[^\\d•|]*[•|]?\\s*\\d+[,.]?\\d*\\s*m²|\\d+[,.]?\\d*\\s*m²)",
          Pattern.CASE_INSENSITIVE);

  private static final Pattern MILLION_PRICE_PATTERN =
      Pattern.compile("(\\d+[,.]?\\d*)\\s*M\\s*Ft", Pattern.CASE_INSENSITIVE);
  private static final Pattern HUF_PRICE_PATTERN =
      Pattern.compile("(\\d[\\d\\s]*)\\s*Ft", Pattern.CASE_INSENSITIVE);

  private static final Pattern AREA_PATTERN =
      Pattern.compile("(\\d+[,.]?\\d*)\\s*m²", Pattern.CASE_INSENSITIVE);
  private static final Pattern ROOM_PATTERN =
      Pattern.compile("(\\d+[,.]?\\d*)\\s*szoba", Pattern.CASE_INSENSITIVE);

  private PropertyParser() {}

  public static String extractPrice(String text) {
    if (text == null || text.isBlank()) {
      return "Ár a hirdetésben";
    }
    Matcher matcher = PRICE_PATTERN.matcher(text);
    return matcher.find() ? matcher.group(1).trim() : "Ár a hirdetésben";
  }

  public static PriceInfo parsePrice(String text) {
    String raw = extractPrice(text);
    if ("Ár a hirdetésben".equals(raw)) {
      return new PriceInfo(raw, null, null);
    }

    Matcher mMillion = MILLION_PRICE_PATTERN.matcher(raw);
    if (mMillion.find()) {
      try {
        double million = Double.parseDouble(mMillion.group(1).replace(",", "."));
        long huf = Math.round(million * 1_000_000.0);
        return new PriceInfo(raw, huf, million);
      } catch (NumberFormatException ignored) {
        // Ignore malformed numeric values and continue with the next supported price format.
      }
    }

    Matcher mHuf = HUF_PRICE_PATTERN.matcher(raw);
    if (mHuf.find()) {
      try {
        String cleanDigits = mHuf.group(1).replaceAll("\\s+", "");
        long huf = Long.parseLong(cleanDigits);
        double million = Math.round((huf / 1_000_000.0) * 100.0) / 100.0;
        return new PriceInfo(raw, huf, million);
      } catch (NumberFormatException ignored) {
        // Ignore malformed numeric values and return an unknown price below.
      }
    }

    return new PriceInfo(raw, null, null);
  }

  public static String extractDetails(String text) {
    if (text == null || text.isBlank()) {
      return "";
    }
    Matcher matcher = DETAILS_PATTERN.matcher(text);
    return matcher.find() ? matcher.group(1).trim() : "";
  }

  public static PropertyDetails parseDetails(String text, PriceInfo priceInfo) {
    String raw = extractDetails(text);
    Double area = null;
    Double rooms = null;

    if (text != null) {
      Matcher mArea = AREA_PATTERN.matcher(text);
      if (mArea.find()) {
        try {
          area = Double.parseDouble(mArea.group(1).replace(",", "."));
        } catch (NumberFormatException ignored) {
          // Keep area unknown when the source value is malformed.
        }
      }

      Matcher mRooms = ROOM_PATTERN.matcher(text);
      if (mRooms.find()) {
        try {
          rooms = Double.parseDouble(mRooms.group(1).replace(",", "."));
        } catch (NumberFormatException ignored) {
          // Keep room count unknown when the source value is malformed.
        }
      }
    }

    Long pricePerM2 = null;
    if (area != null && area > 0 && priceInfo != null && priceInfo.getAmountHuf() != null) {
      pricePerM2 = Math.round((double) priceInfo.getAmountHuf() / area);
    }

    return new PropertyDetails(raw, area, rooms, pricePerM2);
  }

  public static String extractFirstGroup(String text, Pattern pattern, String fallback) {
    if (text == null || pattern == null) {
      return fallback;
    }
    Matcher matcher = pattern.matcher(text);
    return matcher.find() ? matcher.group(1).trim() : fallback;
  }

  private static final Pattern POSTAL_CITY_PATTERN =
      Pattern.compile("\\b(\\d{4})\\s+([A-ZÁÉÍÓÖŐÚÜŰ][a-záéíóöőúüűA-ZÁÉÍÓÖŐÚÜŰ\\-]+)");
  private static final Pattern DH_URL_CITY_PATTERN =
      Pattern.compile(
          "/ingatlan/[A-Za-z0-9\\-]+/elado-[a-z]+-(?:[a-z\\-]+-megye-)?"
              + "([a-z\\-]+?)(?:-\\d{4}|-falu|-varos|-utca|-kertvaros|/|$)");

  public static boolean isPriceInRange(
      PriceInfo priceInfo, Integer minMillion, Integer maxMillion) {
    if (minMillion == null && maxMillion == null) {
      return true;
    }
    if (priceInfo == null || priceInfo.getAmountMillion() == null) {
      return false;
    }
    double amount = priceInfo.getAmountMillion();
    if (minMillion != null && amount < (minMillion - 0.001)) {
      return false;
    }
    if (maxMillion != null && amount > (maxMillion + 0.001)) {
      return false;
    }
    return true;
  }

  public static boolean isAreaInRange(PropertyDetails details, Integer minArea, Integer maxArea) {
    Double area = details != null ? details.getAreaSquareMeters() : null;
    return isAreaInRange(area, minArea, maxArea);
  }

  public static boolean isAreaInRange(Double area, Integer minArea, Integer maxArea) {
    if (minArea == null && maxArea == null) {
      return true;
    }
    if (area == null) {
      return true;
    }
    if (minArea != null && area < (minArea - 0.001)) {
      return false;
    }
    if (maxArea != null && area > (maxArea + 0.001)) {
      return false;
    }
    return true;
  }

  public static String extractCityFromPostalAddress(String text) {
    if (text == null || text.isBlank()) {
      return null;
    }
    Matcher matcher = POSTAL_CITY_PATTERN.matcher(text);
    if (matcher.find()) {
      return matcher.group(2).trim();
    }
    return null;
  }

  public static String extractCityFromDhUrl(String url) {
    if (url == null || url.isBlank()) {
      return null;
    }
    Matcher matcher = DH_URL_CITY_PATTERN.matcher(url);
    if (matcher.find()) {
      return matcher.group(1).trim();
    }
    return null;
  }

  public static String extractImageUrl(org.jsoup.nodes.Element element) {
    if (element == null) {
      return null;
    }
    org.jsoup.nodes.Element img = element.is("img") ? element : element.selectFirst("img");
    if (img == null) {
      return null;
    }
    String[] attrs = {"data-src", "data-original", "data-lazy-src", "src", "data-srcset", "srcset"};
    for (String attr : attrs) {
      String src = img.attr(attr);
      if (src != null && !src.isBlank() && !src.startsWith("data:image")) {
        if (attr.endsWith("srcset")) {
          src = src.split(",")[0].trim().split("\\s+")[0].trim();
        }
        if (src.isBlank() || src.startsWith("data:image")) {
          continue;
        }
        if (src.startsWith("//")) {
          return "https:" + src;
        }
        return src;
      }
    }
    return null;
  }

  public static boolean isNewConstructionText(String... texts) {
    if (texts == null) {
      return false;
    }
    for (String text : texts) {
      if (text == null || text.isBlank()) {
        continue;
      }
      String slug = SlugUtils.toSlug(text);
      if (slug.contains("uj-epitesu")
          || slug.contains("ujepitesu")
          || slug.contains("szerkezetkesz")
          || slug.contains("kulcsrakesz-uj")
          || slug.contains("uj-projekt")
          || slug.contains("lakopark-projekt")) {
        return true;
      }
    }
    return false;
  }
}
