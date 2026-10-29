package ingatlanok.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import ingatlanok.common.PropertyParser;
import ingatlanok.model.IngatlanItem;
import ingatlanok.model.PropertyType;

/**
 * Deterministic rule-based data quality and reliability scoring engine (#267).
 * Evaluates listings on a 0-100 scale based on sanity, plausibility, and anomaly checks.
 */
public class DataQualityEngine {

  public static final int BASE_SCORE = 100;
  public static final int PASSING_THRESHOLD = 70;

  // Sanity thresholds
  public static final long MIN_SANITY_PRICE_HUF = 1_000_000L; // 1M HUF
  public static final long MAX_SANITY_PRICE_HUF = 5_000_000_000L; // 5B HUF
  public static final double MIN_RESIDENTIAL_AREA_M2 = 10.0;
  public static final double MAX_RESIDENTIAL_AREA_M2 = 2_000.0;
  public static final double MIN_PLOT_AREA_M2 = 50.0;
  public static final double MAX_PLOT_AREA_M2 = 500_000.0;
  public static final double MIN_OTHER_AREA_M2 = 5.0;

  // Plausibility thresholds
  public static final long MIN_RESIDENTIAL_PRICE_PER_M2 = 100_000L;
  public static final long MAX_RESIDENTIAL_PRICE_PER_M2 = 4_000_000L;
  public static final double MIN_AREA_PER_ROOM_M2 = 12.0;
  public static final double MAX_AREA_PER_ROOM_M2 = 150.0;

  // Anomaly thresholds
  public static final double MAX_PRICE_CHANGE_PERCENT = 50.0;

  /**
   * Encapsulates the numerical quality score and list of deduction reasons.
   */
  public record QualityEvaluation(int score, List<String> deductions) {
    public QualityEvaluation {
      deductions = deductions != null ? List.copyOf(deductions) : List.of();
    }
  }

  /**
   * Evaluates a listing against all sanity, plausibility, and anomaly rules.
   *
   * @param item the property listing to evaluate
   * @return deterministic QualityEvaluation containing score (0-100) and itemized deductions
   */
  public QualityEvaluation evaluate(IngatlanItem item) {
    if (item == null) {
      return new QualityEvaluation(0, List.of("Hiányzó hirdetés objektum: -100 pont"));
    }

    int score = BASE_SCORE;
    List<String> deductions = new ArrayList<>();

    // 1. Price Sanity (-30)
    Long priceAmount = resolvePriceAmount(item);
    if (priceAmount == null || priceAmount < MIN_SANITY_PRICE_HUF || priceAmount > MAX_SANITY_PRICE_HUF) {
      score -= 30;
      deductions.add("Érvénytelen vagy irreális ár (1M - 5B Ft tartományon kívül): -30 pont");
    }

    // 2. Area Sanity (-25)
    PropertyType type = item.getPropertyType() != null ? item.getPropertyType() : PropertyType.EGYEB_ISMERETLEN;
    Double area = resolveArea(item);
    if (area == null || area <= 0) {
      score -= 25;
      deductions.add("Hiányzó vagy érvénytelen alapterület: -25 pont");
    } else if (type.isPlot()) {
      if (area < MIN_PLOT_AREA_M2 || area > MAX_PLOT_AREA_M2) {
        score -= 25;
        deductions.add("Irreális telekméret (50 m² - 500 000 m²-en kívül): -25 pont");
      }
    } else if (type.isResidential()) {
      if (area < MIN_RESIDENTIAL_AREA_M2 || area > MAX_RESIDENTIAL_AREA_M2) {
        score -= 25;
        deductions.add("Irreális lakóterület (10 m² - 2000 m²-en kívül): -25 pont");
      }
    } else {
      if (area < MIN_OTHER_AREA_M2 || area > MAX_PLOT_AREA_M2) {
        score -= 25;
        deductions.add("Irreális alapterület: -25 pont");
      }
    }

    // 3. City Presence (-20)
    String rawCity = item.getRawCity();
    String location = item.getLocation();
    boolean hasCity = (rawCity != null && !rawCity.isBlank() && !"Ismeretlen".equalsIgnoreCase(rawCity.trim()))
        || (location != null && !location.isBlank() && !"Ismeretlen".equalsIgnoreCase(location.trim()));
    if (!hasCity) {
      score -= 20;
      deductions.add("Hiányzó vagy érvénytelen település: -20 pont");
    }

    // 4. Type Presence (-15)
    if (type == PropertyType.EGYEB_ISMERETLEN) {
      score -= 15;
      deductions.add("Ismeretlen vagy hiányzó ingatlantípus: -15 pont");
    }

    // 5. Price/m² Plausibility (-20) - Residential only
    Long pricePerM2 = resolvePricePerM2(item, priceAmount, area);
    if (type.isResidential() && pricePerM2 != null) {
      if (pricePerM2 < MIN_RESIDENTIAL_PRICE_PER_M2 || pricePerM2 > MAX_RESIDENTIAL_PRICE_PER_M2) {
        score -= 20;
        deductions.add("Irreális négyzetméterár lakóingatlannál (< 100 000 vagy > 4 000 000 Ft/m²): -20 pont");
      }
    }

    // 6. Room Count to Area Consistency (-15) - Residential only
    Double rooms = resolveRooms(item);
    if (type.isResidential() && area != null && area > 0 && rooms != null && rooms > 0) {
      double areaPerRoom = area / rooms;
      if (areaPerRoom < MIN_AREA_PER_ROOM_M2 || areaPerRoom > MAX_AREA_PER_ROOM_M2) {
        score -= 15;
        deductions.add("Inkonzisztens szobaszám-alapterület arány (< 12 vagy > 150 m²/szoba): -15 pont");
      }
    }

    // 7. Title Presence (-10)
    if (item.getTitle() == null || item.getTitle().isBlank()) {
      score -= 10;
      deductions.add("Hiányzó hirdetéscím: -10 pont");
    }

    // 8. URL Validity (-20)
    String url = item.getUrl();
    if (url == null || url.isBlank() || (!url.startsWith("http://") && !url.startsWith("https://"))) {
      score -= 20;
      deductions.add("Hiányzó vagy érvénytelen hirdetés URL: -20 pont");
    }

    // 9. Photo Presence (-10)
    if (item.getImageUrl() == null || item.getImageUrl().isBlank()) {
      score -= 10;
      deductions.add("Hiányzó fényképes dokumentáció: -10 pont");
    }

    // 10. Extreme Price Shift in History (-25)
    if (item.getPriceChangePercent() != null && Math.abs(item.getPriceChangePercent()) > MAX_PRICE_CHANGE_PERCENT) {
      score -= 25;
      deductions.add("Extrém árváltozás az előzményekben (> 50% elmozdulás): -25 pont");
    }

    int clampedScore = Math.max(0, Math.min(100, score));
    return new QualityEvaluation(clampedScore, deductions);
  }

  /**
   * Evaluates and mutates the given item by populating qualityScore and qualityDeductions.
   *
   * @param item the property listing to score
   */
  public void applyQualityScore(IngatlanItem item) {
    if (item != null) {
      QualityEvaluation eval = evaluate(item);
      item.setQualityScore(eval.score());
      item.setQualityDeductions(eval.deductions());
    }
  }

  /**
   * Batch scores all items in the given collection.
   *
   * @param items collection of property listings
   */
  public void evaluateAll(Collection<IngatlanItem> items) {
    if (items != null) {
      for (IngatlanItem item : items) {
        applyQualityScore(item);
      }
    }
  }

  private Long resolvePriceAmount(IngatlanItem item) {
    if (item.getPriceInfo() != null && item.getPriceInfo().getAmountHuf() != null) {
      return item.getPriceInfo().getAmountHuf();
    }
    if (item.getPrice() != null && !item.getPrice().isBlank()) {
      return PropertyParser.parsePrice(item.getPrice()).getAmountHuf();
    }
    return null;
  }

  private Double resolveArea(IngatlanItem item) {
    if (item.getDetailsInfo() != null && item.getDetailsInfo().getAreaSquareMeters() != null) {
      return item.getDetailsInfo().getAreaSquareMeters();
    }
    if (item.getDetails() != null && !item.getDetails().isBlank()) {
      return PropertyParser.parseDetails(item.getDetails(), item.getPriceInfo()).getAreaSquareMeters();
    }
    return null;
  }

  private Double resolveRooms(IngatlanItem item) {
    if (item.getDetailsInfo() != null && item.getDetailsInfo().getRoomCount() != null) {
      return item.getDetailsInfo().getRoomCount();
    }
    if (item.getDetails() != null && !item.getDetails().isBlank()) {
      return PropertyParser.parseDetails(item.getDetails(), item.getPriceInfo()).getRoomCount();
    }
    return null;
  }

  private Long resolvePricePerM2(IngatlanItem item, Long priceHuf, Double areaM2) {
    if (item.getDetailsInfo() != null && item.getDetailsInfo().getPricePerSquareMeter() != null
        && item.getDetailsInfo().getPricePerSquareMeter() > 0) {
      return item.getDetailsInfo().getPricePerSquareMeter();
    }
    if (priceHuf != null && priceHuf > 0 && areaM2 != null && areaM2 > 0) {
      return Math.round((double) priceHuf / areaM2);
    }
    return null;
  }
}
