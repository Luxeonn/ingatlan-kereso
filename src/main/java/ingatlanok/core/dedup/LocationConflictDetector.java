package ingatlanok.core.dedup;

import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ingatlanok.common.SlugUtils;
import ingatlanok.model.IngatlanItem;

/**
 * Detects location conflicts and alignments across districts, quarters, and street names.
 */
public final class LocationConflictDetector {

  private static final Pattern BP_DISTRICT_ROMAN_PATTERN =
      Pattern.compile(
          "(?iuU)\\b(XXIII|XXII|XXI|XX|XIX|XVIII|XVII|XVI|XV|XIV|XIII|XII|XI|X|IX|VIII|VII|VI|V|IV|III|II|I)"
              + "\\.?\\s*(?:kerület|ker\\b|kerületben)");

  private static final Pattern BP_DISTRICT_ARABIC_PATTERN =
      Pattern.compile("(?iuU)\\b([1-9]|1[0-9]|2[0-3])\\.?\\s*(?:kerület|ker\\b|kerületben)");

  private static final Pattern BP_DISTRICT_URL_PATTERN =
      Pattern.compile("(?iuU)(?:budapest|bp)[-_/]?([1-9]|1[0-9]|2[0-3])[-_]?(?:kerület|ker)");

  private static final Pattern STREET_NAME_PATTERN =
      Pattern.compile(
          "(?iuU)\\b([A-ZÁÉÍÓÖŐÚÜŰa-záéíóöőúüű0-9\\.\\-\\s]{2,30}?)\\s+"
              + "(utcai|utca|u\\.|u\\b|úti|útja|út|téri|tere|tér|fasori|fasor|körúti|korut|körút"
              + "|sori|sora|sor|sétányi|setany|sétány|dűlői|dűlő|közi|köz)\\b");

  private static final Map<String, Integer> BP_DISTRICT_NAMES =
      Map.ofEntries(
          Map.entry("varkerulet", 1), Map.entry("krisztinavaros", 1), Map.entry("taban", 1),
          Map.entry("rozsadomb", 2), Map.entry("pasaret", 2), Map.entry("pesthidegkut", 2),
          Map.entry("obuda", 3), Map.entry("bekasmegyer", 3), Map.entry("csillaghegy", 3),
          Map.entry("ujpest", 4), Map.entry("kaposztasmegyer", 4),
          Map.entry("belvaros", 5), Map.entry("lipotvaros", 5),
          Map.entry("terezvaros", 6),
          Map.entry("erzsebetvaros", 7),
          Map.entry("jozsefvaros", 8), Map.entry("corvin", 8), Map.entry("palotanegyed", 8),
          Map.entry("ferencvaros", 9),
          Map.entry("kobanya", 10), Map.entry("ohegy", 10), Map.entry("ujhegy", 10),
          Map.entry("ujbuda", 11), Map.entry("gazdagret", 11), Map.entry("kelenfold", 11),
          Map.entry("gellerthegy", 11), Map.entry("sasad", 11), Map.entry("madarhegy", 11),
          Map.entry("hegyvidek", 12), Map.entry("zugliget", 12), Map.entry("farkasret", 12),
          Map.entry("angyalfold", 13), Map.entry("ujlipotvaros", 13), Map.entry("vizafogo", 13),
          Map.entry("zuglo", 14), Map.entry("alsorakos", 14), Map.entry("herminamezo", 14),
          Map.entry("rakospalota", 15), Map.entry("pestujhely", 15), Map.entry("ujpalota", 15),
          Map.entry("matyasfold", 16), Map.entry("sashalom", 16), Map.entry("cinkota", 16),
          Map.entry("rakosmente", 17), Map.entry("rakoskeresztur", 17), Map.entry("rakoscsaba", 17),
          Map.entry("pestszentlorinc", 18), Map.entry("pestszentimre", 18), Map.entry("havanna", 18),
          Map.entry("kispest", 19), Map.entry("wekerle", 19),
          Map.entry("pesterzsebet", 20),
          Map.entry("csepel", 21),
          Map.entry("budafok", 22), Map.entry("nagyteteny", 22), Map.entry("budateteny", 22),
          Map.entry("soroksar", 23));

  private LocationConflictDetector() {}

  /**
   * Structured location details extracted from an IngatlanItem.
   */
  public record ExtractedLocation(
      Integer district,
      String quarter,
      String street) {

    public boolean hasDistrict() {
      return district != null && district > 0;
    }

    public boolean hasQuarter() {
      return quarter != null && !quarter.isBlank();
    }

    public boolean hasStreet() {
      return street != null && !street.isBlank();
    }
  }

  /**
   * Extracts location signals (Budapest district, city quarter, street name) from an item.
   */
  public static ExtractedLocation extractLocation(IngatlanItem item, String normalizedCity) {
    if (item == null) {
      return new ExtractedLocation(null, null, null);
    }

    String title = item.getTitle() != null ? item.getTitle() : "";
    String location = item.getLocation() != null ? item.getLocation() : "";
    String url = item.getUrl() != null ? item.getUrl() : "";
    String combined = (title + " " + location + " " + url).trim();

    Integer district = null;
    boolean isBudapest = "budapest".equals(normalizedCity);
    if (isBudapest) {
      district = extractBudapestDistrict(combined);
    }

    String quarter = extractQuarter(location, title, normalizedCity, isBudapest);
    String street = extractStreet(title, location);

    return new ExtractedLocation(district, quarter, street);
  }

  /**
   * Checks whether two items have conflicting location sub-units.
   */
  public static boolean hasConflict(ExtractedLocation locA, ExtractedLocation locB, String normalizedCity) {
    if (locA == null || locB == null) {
      return false;
    }

    // 1. Budapest district conflict
    if ("budapest".equals(normalizedCity) && locA.hasDistrict() && locB.hasDistrict()) {
      if (!locA.district().equals(locB.district())) {
        return true;
      }
    }

    // 2. City quarter conflict (e.g. Bánomkert vs Kertváros)
    if (locA.hasQuarter() && locB.hasQuarter()) {
      String qA = locA.quarter();
      String qB = locB.quarter();
      if (!qA.equals(qB) && !qA.contains(qB) && !qB.contains(qA)) {
        return true;
      }
    }

    // 3. Street name conflict (e.g. Muskátli utca vs Kossuth utca)
    if (locA.hasStreet() && locB.hasStreet()) {
      String sA = locA.street();
      String sB = locB.street();
      if (!sA.equals(sB) && !sA.contains(sB) && !sB.contains(sA)) {
        return true;
      }
    }

    return false;
  }

  /**
   * Checks if two items have positive affirmative location matching (same district, quarter, or street).
   */
  public static boolean hasAffirmativeMatch(ExtractedLocation locA, ExtractedLocation locB) {
    if (locA == null || locB == null) {
      return false;
    }
    if (locA.hasDistrict() && locB.hasDistrict() && locA.district().equals(locB.district())) {
      return true;
    }
    if (locA.hasStreet() && locB.hasStreet() && locA.street().equals(locB.street())) {
      return true;
    }
    if (locA.hasQuarter() && locB.hasQuarter() && locA.quarter().equals(locB.quarter())) {
      return true;
    }
    return false;
  }

  private static Integer extractBudapestDistrict(String text) {
    if (text == null || text.isBlank()) {
      return null;
    }

    // Check Roman numerals first
    Matcher mRoman = BP_DISTRICT_ROMAN_PATTERN.matcher(text);
    if (mRoman.find()) {
      Integer d = romanToNumber(mRoman.group(1));
      if (d != null) {
        return d;
      }
    }

    // Check Arabic numerals
    Matcher mArabic = BP_DISTRICT_ARABIC_PATTERN.matcher(text);
    if (mArabic.find()) {
      try {
        int d = Integer.parseInt(mArabic.group(1));
        if (d >= 1 && d <= 23) {
          return d;
        }
      } catch (NumberFormatException ignored) {
        // Fall through
      }
    }

    // Check URL pattern
    Matcher mUrl = BP_DISTRICT_URL_PATTERN.matcher(text);
    if (mUrl.find()) {
      try {
        int d = Integer.parseInt(mUrl.group(1));
        if (d >= 1 && d <= 23) {
          return d;
        }
      } catch (NumberFormatException ignored) {
        // Fall through
      }
    }

    // Check district names
    String slug = SlugUtils.toSlug(text);
    for (Map.Entry<String, Integer> entry : BP_DISTRICT_NAMES.entrySet()) {
      if (slug.contains(entry.getKey())) {
        return entry.getValue();
      }
    }

    return null;
  }

  private static String extractQuarter(String location, String title, String normalizedCity, boolean isBudapest) {
    if (isBudapest) {
      return null; // For Budapest, districts are primary
    }

    // 1. If location is formatted as "City, Quarter"
    if (location != null && location.contains(",")) {
      String[] parts = location.split(",", 2);
      if (parts.length > 1) {
        String q = SlugUtils.toSlug(parts[1].trim());
        if (!q.isBlank() && !q.equals(normalizedCity)) {
          return q;
        }
      }
    }

    // 2. Common quarters in provincial cities
    String textSlug = SlugUtils.toSlug((title + " " + location).trim());
    String[] quarters = {
        "banomkert", "kertvaros", "nagyerd", "nagyerdot", "tocoskert", "csapokert", "jozsa", "venkert",
        "ujszeged", "felsovaros", "tarjan", "rokus", "moravaros", "szentmihaly", "marosto",
        "avas", "diosgyor", "szirma", "goromboly", "tapolca", "komlosteto",
        "uranvaros", "mecsekoldal", "meszes", "patacs", "gyarvaros", "nadorvaros", "szabadhegy",
        "marcalvaros", "revfalu", "furdovezet", "belvaros"
    };

    for (String q : quarters) {
      if (textSlug.contains(q)) {
        return q;
      }
    }

    return null;
  }

  private static String extractStreet(String title, String location) {
    String combined = (title + " " + location).trim();
    Matcher m = STREET_NAME_PATTERN.matcher(combined);
    if (m.find()) {
      String street = SlugUtils.toSlug(m.group(1).trim());
      if (street.length() >= 3 && !street.equals("elado") && !street.equals("ingatlan")) {
        return street;
      }
    }
    return null;
  }

  private static Integer romanToNumber(String roman) {
    if (roman == null) {
      return null;
    }
    return switch (roman.toUpperCase(Locale.ROOT)) {
      case "I" -> 1;
      case "II" -> 2;
      case "III" -> 3;
      case "IV" -> 4;
      case "V" -> 5;
      case "VI" -> 6;
      case "VII" -> 7;
      case "VIII" -> 8;
      case "IX" -> 9;
      case "X" -> 10;
      case "XI" -> 11;
      case "XII" -> 12;
      case "XIII" -> 13;
      case "XIV" -> 14;
      case "XV" -> 15;
      case "XVI" -> 16;
      case "XVII" -> 17;
      case "XVIII" -> 18;
      case "XIX" -> 19;
      case "XX" -> 20;
      case "XXI" -> 21;
      case "XXII" -> 22;
      case "XXIII" -> 23;
      default -> null;
    };
  }
}
