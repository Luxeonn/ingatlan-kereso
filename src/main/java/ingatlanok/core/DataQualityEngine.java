package ingatlanok.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import ingatlanok.common.PropertyParser;
import ingatlanok.model.IngatlanItem;
import ingatlanok.model.PropertyType;
import ingatlanok.model.QualityDeduction;
import ingatlanok.model.QualityDeductionRule;
import ingatlanok.model.QualityScore;

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
   * Encapsulates the numerical quality score and list of deduction reasons (#401 / JAVA-010, JAVA-014).
   */
  public record QualityEvaluation(
      int score,
      List<String> deductions,
      List<QualityDeduction> structuredDeductions) {

    public QualityEvaluation(int score, List<String> deductions) {
      this(score, deductions, List.of());
    }

    public QualityEvaluation {
      deductions = deductions != null ? List.copyOf(deductions) : List.of();
      structuredDeductions =
          structuredDeductions != null ? List.copyOf(structuredDeductions) : List.of();
    }

    public QualityScore toQualityScore() {
      return QualityScore.of(score);
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

    DeductionCollector collector = new DeductionCollector();
    Long priceAmount = resolvePriceAmount(item);
    Double area = resolveArea(item);
    PropertyType type =
        item.getPropertyType() != null ? item.getPropertyType() : PropertyType.EGYEB_ISMERETLEN;

    evaluatePriceSanity(priceAmount, collector);
    evaluateAreaSanity(type, area, collector);
    evaluateLocationAndType(item, type, collector);
    evaluateConsistency(
        type, area, resolvePricePerM2(item, priceAmount, area), resolveRooms(item), collector);
    evaluateCompleteness(item, collector);

    int clampedScore = Math.max(0, Math.min(100, collector.score));
    return new QualityEvaluation(
        clampedScore, collector.deductions, collector.structuredDeductions);
  }

  private void evaluatePriceSanity(Long priceAmount, DeductionCollector collector) {
    if (priceAmount == null
        || priceAmount < MIN_SANITY_PRICE_HUF
        || priceAmount > MAX_SANITY_PRICE_HUF) {
      collector.deduct(
          QualityDeductionRule.INVALID_PRICE,
          30,
          "Érvénytelen vagy irreális ár (1M - 5B Ft tartományon kívül): -30 pont");
    }
  }

  private void evaluateAreaSanity(
      PropertyType type, Double area, DeductionCollector collector) {
    if (area == null || area <= 0) {
      collector.deduct(
          QualityDeductionRule.MISSING_AREA, 25, "Hiányzó vagy érvénytelen alapterület: -25 pont");
      return;
    }
    if (type.isPlot()) {
      if (area < MIN_PLOT_AREA_M2 || area > MAX_PLOT_AREA_M2) {
        collector.deduct(
            QualityDeductionRule.UNREALISTIC_AREA,
            25,
            "Irreális telekméret (50 m² - 500 000 m²-en kívül): -25 pont");
      }
    } else if (type.isResidential()) {
      if (area < MIN_RESIDENTIAL_AREA_M2 || area > MAX_RESIDENTIAL_AREA_M2) {
        collector.deduct(
            QualityDeductionRule.UNREALISTIC_AREA,
            25,
            "Irreális lakóterület (10 m² - 2000 m²-en kívül): -25 pont");
      }
    } else if (area < MIN_OTHER_AREA_M2 || area > MAX_PLOT_AREA_M2) {
      collector.deduct(QualityDeductionRule.UNREALISTIC_AREA, 25, "Irreális alapterület: -25 pont");
    }
  }

  private void evaluateLocationAndType(
      IngatlanItem item, PropertyType type, DeductionCollector collector) {
    String rawCity = item.getRawCity();
    String location = item.getLocation();
    boolean hasCity = isValidCityValue(rawCity) || isValidCityValue(location);
    if (!hasCity) {
      collector.deduct(
          QualityDeductionRule.MISSING_CITY, 20, "Hiányzó vagy érvénytelen település: -20 pont");
    }
    if (type == PropertyType.EGYEB_ISMERETLEN) {
      collector.deduct(
          QualityDeductionRule.UNKNOWN_TYPE, 15, "Ismeretlen vagy hiányzó ingatlantípus: -15 pont");
    }
  }

  private boolean isValidCityValue(String val) {
    return val != null && !val.isBlank() && !"Ismeretlen".equalsIgnoreCase(val.trim());
  }

  private void evaluateConsistency(
      PropertyType type,
      Double area,
      Long pricePerM2,
      Double rooms,
      DeductionCollector collector) {
    if (type.isResidential() && pricePerM2 != null) {
      if (pricePerM2 < MIN_RESIDENTIAL_PRICE_PER_M2 || pricePerM2 > MAX_RESIDENTIAL_PRICE_PER_M2) {
        collector.deduct(
            QualityDeductionRule.SUSPICIOUS_SQM_PRICE,
            20,
            "Irreális négyzetméterár lakóingatlannál (< 100 000 vagy > 4 000 000 Ft/m²): -20 pont");
      }
    }
    if (type.isResidential() && area != null && area > 0 && rooms != null && rooms > 0) {
      double areaPerRoom = area / rooms;
      if (areaPerRoom < MIN_AREA_PER_ROOM_M2 || areaPerRoom > MAX_AREA_PER_ROOM_M2) {
        collector.deduct(
            QualityDeductionRule.UNREALISTIC_ROOMS,
            15,
            "Inkonzisztens szobaszám-alapterület arány (< 12 vagy > 150 m²/szoba): -15 pont");
      }
    }
  }

  private void evaluateCompleteness(IngatlanItem item, DeductionCollector collector) {
    if (item.getTitle() == null || item.getTitle().isBlank()) {
      collector.deduct(QualityDeductionRule.MISSING_DESCRIPTION, 10, "Hiányzó hirdetéscím: -10 pont");
    }
    String url = item.getUrl();
    if (url == null || url.isBlank() || (!url.startsWith("http://") && !url.startsWith("https://"))) {
      collector.deduct(
          QualityDeductionRule.MISSING_DETAILS, 20, "Hiányzó vagy érvénytelen hirdetés URL: -20 pont");
    }
    if (item.getImageUrl() == null || item.getImageUrl().isBlank()) {
      collector.deduct(QualityDeductionRule.MISSING_IMAGES, 10, "Hiányzó fényképes dokumentáció: -10 pont");
    }
    if (item.getPriceChangePercent() != null
        && Math.abs(item.getPriceChangePercent()) > MAX_PRICE_CHANGE_PERCENT) {
      collector.deduct(
          QualityDeductionRule.INVALID_PRICE,
          25,
          "Extrém árváltozás az előzményekben (> 50% elmozdulás): -25 pont");
    }
  }

  private static final class DeductionCollector {
    private int score = BASE_SCORE;
    private final List<String> deductions = new ArrayList<>();
    private final List<QualityDeduction> structuredDeductions = new ArrayList<>();

    void deduct(QualityDeductionRule rule, int points, String reason) {
      this.score -= points;
      this.deductions.add(reason);
      this.structuredDeductions.add(new QualityDeduction(rule, points, reason));
    }

    void deduct(int points, String reason) {
      this.score -= points;
      this.deductions.add(reason);
    }
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
