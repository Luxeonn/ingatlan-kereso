package ingatlanok.core.dedup;

import java.util.List;
import java.util.Locale;

import ingatlanok.model.IngatlanItem;
import ingatlanok.model.PropertyDetails;

/**
 * Helper for cluster detail consolidation, missing attribute inheritance,
 * and media comparison.
 */
public final class ClusterConsolidationHelper {

  private ClusterConsolidationHelper() {}

  public static boolean hasPositiveArea(IngatlanItem item) {
    return item != null
        && item.getDetailsInfo() != null
        && item.getDetailsInfo().getAreaSquareMeters() != null
        && item.getDetailsInfo().getAreaSquareMeters() > 0;
  }

  public static boolean hasPositiveRooms(IngatlanItem item) {
    return item != null
        && item.getDetailsInfo() != null
        && item.getDetailsInfo().getRoomCount() != null
        && item.getDetailsInfo().getRoomCount() > 0;
  }

  public static void adoptMissingDetailsFromCluster(IngatlanItem primary, List<IngatlanItem> cluster) {
    if (primary == null || cluster == null || cluster.isEmpty()) {
      return;
    }
    Double area = findFirstPositiveArea(primary, cluster);
    Double rooms = findFirstPositiveRooms(primary, cluster);

    if (area != null || rooms != null) {
      enrichPrimaryDetails(primary, area, rooms);
    }
  }

  private static Double findFirstPositiveArea(IngatlanItem primary, List<IngatlanItem> cluster) {
    if (hasPositiveArea(primary)) {
      return primary.getDetailsInfo().getAreaSquareMeters();
    }
    for (IngatlanItem member : cluster) {
      if (hasPositiveArea(member)) {
        return member.getDetailsInfo().getAreaSquareMeters();
      }
    }
    return null;
  }

  private static Double findFirstPositiveRooms(IngatlanItem primary, List<IngatlanItem> cluster) {
    if (hasPositiveRooms(primary)) {
      return primary.getDetailsInfo().getRoomCount();
    }
    for (IngatlanItem member : cluster) {
      if (hasPositiveRooms(member)) {
        return member.getDetailsInfo().getRoomCount();
      }
    }
    return null;
  }

  private static void enrichPrimaryDetails(IngatlanItem primary, Double area, Double rooms) {
    Long priceHuf = primary.getPriceInfo() != null ? primary.getPriceInfo().getAmountHuf() : null;
    Long sqmPrice = (area != null && area > 0 && priceHuf != null && priceHuf > 0)
        ? Math.round((double) priceHuf / area)
        : null;

    String currentRaw = primary.getDetailsInfo() != null ? primary.getDetailsInfo().getRawText() : null;
    String newRaw = (currentRaw != null && !currentRaw.isBlank())
        ? currentRaw
        : formatDetailsText(area, rooms);

    PropertyDetails enriched = PropertyDetails.builder()
        .rawText(newRaw)
        .areaSquareMeters(area)
        .roomCount(rooms)
        .pricePerSquareMeter(sqmPrice)
        .build();

    primary.setDetailsInfo(enriched);
    if (primary.getDetails() == null || primary.getDetails().isBlank()) {
      primary.setDetails(newRaw);
    }
  }

  private static String formatDetailsText(Double area, Double rooms) {
    if (area != null && rooms != null) {
      return Math.round(area) + " m² • " + Math.round(rooms) + " szoba";
    }
    if (area != null) {
      return Math.round(area) + " m²";
    }
    if (rooms != null) {
      return Math.round(rooms) + " szoba";
    }
    return "";
  }

  public static Long resolveSourcePricePerM2(IngatlanItem item, Long priceHuf, Double fallbackArea) {
    if (item.getDetailsInfo() != null && item.getDetailsInfo().getPricePerSquareMeter() != null
        && item.getDetailsInfo().getPricePerSquareMeter() > 0) {
      return item.getDetailsInfo().getPricePerSquareMeter().longValue();
    }
    Double area = hasPositiveArea(item)
        ? item.getDetailsInfo().getAreaSquareMeters()
        : fallbackArea;
    if (priceHuf != null && area != null && area > 0) {
      return Math.round((double) priceHuf / area);
    }
    return null;
  }

  public static boolean hasMatchingImage(IngatlanItem a, IngatlanItem b) {
    String imgA = a != null ? a.getImageUrl() : null;
    String imgB = b != null ? b.getImageUrl() : null;
    if (imgA == null || imgB == null || imgA.isBlank() || imgB.isBlank()) {
      return false;
    }
    String normA = normalizeImageUrl(imgA.trim());
    String normB = normalizeImageUrl(imgB.trim());
    return !normA.isBlank() && normA.equalsIgnoreCase(normB);
  }

  public static String normalizeImageUrl(String url) {
    if (url == null || url.isBlank()) {
      return "";
    }
    String cleaned = url.replaceAll("\\?.*$", "").replaceAll("^https?://", "");
    String lower = cleaned.toLowerCase(Locale.ROOT);
    if (lower.contains("placeholder") || lower.contains("no-image") || lower.contains("default")) {
      return "";
    }
    return cleaned.trim();
  }
}
