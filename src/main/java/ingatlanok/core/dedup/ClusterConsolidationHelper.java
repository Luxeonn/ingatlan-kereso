package ingatlanok.core.dedup;

import java.util.List;
import java.util.Locale;

import ingatlanok.model.IngatlanItem;
import ingatlanok.model.PortalSource;
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
      return Math.round(area) + " m² • " + formatRoomCount(rooms) + " szoba";
    }
    if (area != null) {
      return Math.round(area) + " m²";
    }
    if (rooms != null) {
      return formatRoomCount(rooms) + " szoba";
    }
    return "";
  }

  private static String formatRoomCount(double rooms) {
    long rounded = Math.round(rooms);
    if (Math.abs(rooms - rounded) < 0.01) {
      return Long.toString(rounded);
    }
    return Double.toString(rooms);
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

  public static boolean isSamePortalDifferentUrl(IngatlanItem a, IngatlanItem b) {
    if (a == null || b == null) {
      return false;
    }
    String portalA = resolvePortalKey(a);
    String portalB = resolvePortalKey(b);
    if (portalA.isEmpty() || !portalA.equals(portalB)) {
      return false;
    }
    String urlA = a.getUrl() != null ? a.getUrl().trim() : "";
    String urlB = b.getUrl() != null ? b.getUrl().trim() : "";
    return !urlA.isEmpty() && !urlB.isEmpty() && !urlA.equalsIgnoreCase(urlB);
  }

  private static String resolvePortalKey(IngatlanItem item) {
    String urlHost = extractPortalFromUrl(item.getUrl());
    if (!urlHost.isEmpty()) {
      return urlHost;
    }
    String portal = item.getPortal();
    if (portal != null && !portal.isBlank()) {
      return normalizePortalName(portal);
    }
    if (item.getSource() != null) {
      String sourceId = item.getSource().getPortalId();
      if (sourceId != null && !sourceId.isBlank()) {
        return normalizePortalName(sourceId);
      }
      String sourceName = item.getSource().getPortalName();
      if (sourceName != null && !sourceName.isBlank()) {
        return normalizePortalName(sourceName);
      }
    }
    return "";
  }

  private static String normalizePortalName(String raw) {
    String lower = raw.trim().toLowerCase(Locale.ROOT);
    String portalId = PortalSource.of(lower).getPortalId();
    if ("jf".equals(portalId)) {
      return "jofogas";
    }
    if ("ib".equals(portalId)) {
      return "ingatlanbazar";
    }
    return portalId != null ? portalId : lower;
  }

  private static String extractPortalFromUrl(String url) {
    if (url == null || url.isBlank()) {
      return "";
    }
    String lower = url.toLowerCase(Locale.ROOT);
    if (lower.contains("dh.hu")) {
      return "dh";
    }
    if (lower.contains("oc.hu")) {
      return "oc";
    }
    if (lower.contains("jofogas.hu")) {
      return "jofogas";
    }
    if (lower.contains("koltozzbe.hu")) {
      return "koltozzbe";
    }
    if (lower.contains("ingatlanbazar.hu")) {
      return "ingatlanbazar";
    }
    if (lower.contains("ingatlan.com")) {
      return "ingatlancom";
    }
    return "";
  }
}
