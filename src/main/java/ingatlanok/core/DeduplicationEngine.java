package ingatlanok.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import ingatlanok.common.CityUtils;
import ingatlanok.model.IngatlanItem;
import ingatlanok.model.PropertyType;
import ingatlanok.model.SourceListing;
import ingatlanok.persistence.ListingStorage;

public class DeduplicationEngine {

  private static final double MAX_AREA_TOLERANCE_RATIO = 0.05; // ±5%
  private static final double MAX_ROOM_DIFFERENCE = 0.5; // within ±0.5 rooms
  private static final double MAX_PRICE_BAND_RATIO = 0.20; // within ±20%

  private final ListingStorage listingStorage;

  public DeduplicationEngine(ListingStorage listingStorage) {
    this.listingStorage = listingStorage;
  }

  /**
   * Fast O(1) URL deduplication preserving iteration order.
   */
  public List<IngatlanItem> deduplicateByUrl(Collection<IngatlanItem> items) {
    if (items == null || items.isEmpty()) {
      return Collections.emptyList();
    }
    Map<String, IngatlanItem> unique = new LinkedHashMap<>();
    for (IngatlanItem item : items) {
      if (item != null && item.getUrl() != null && !item.getUrl().isBlank()) {
        unique.putIfAbsent(item.getUrl().trim(), item);
      }
    }
    return new ArrayList<>(unique.values());
  }

  /**
   * Multi-signal cross-portal clustering and consolidation.
   */
  public List<IngatlanItem> deduplicateMultiSignal(Collection<IngatlanItem> items) {
    if (items == null || items.isEmpty()) {
      return Collections.emptyList();
    }

    // Stage 1: Fast O(1) URL deduplication
    List<IngatlanItem> stage1Items = deduplicateByUrl(items);
    if (stage1Items.isEmpty()) {
      return Collections.emptyList();
    }

    // Stage 2: Cross-portal multi-signal clustering
    List<List<IngatlanItem>> clusters = new ArrayList<>();
    for (IngatlanItem candidate : stage1Items) {
      boolean addedToCluster = false;
      for (List<IngatlanItem> cluster : clusters) {
        if (matchesCluster(candidate, cluster)) {
          cluster.add(candidate);
          addedToCluster = true;
          break;
        }
      }
      if (!addedToCluster) {
        List<IngatlanItem> newCluster = new ArrayList<>();
        newCluster.add(candidate);
        clusters.add(newCluster);
      }
    }

    // Stage 3: Consolidation into grouped IngatlanItem
    List<IngatlanItem> consolidatedItems = new ArrayList<>(clusters.size());
    for (List<IngatlanItem> cluster : clusters) {
      consolidatedItems.add(consolidateCluster(cluster));
    }

    return consolidatedItems;
  }

  public List<IngatlanItem> deduplicate(Collection<IngatlanItem> items) {
    return deduplicateMultiSignal(items);
  }

  private boolean matchesCluster(IngatlanItem candidate, List<IngatlanItem> cluster) {
    if (cluster == null || cluster.isEmpty() || candidate == null) {
      return false;
    }
    for (IngatlanItem member : cluster) {
      if (!isMatch(candidate, member)) {
        return false;
      }
    }
    return true;
  }

  public boolean isMatch(IngatlanItem a, IngatlanItem b) {
    if (a == null || b == null) {
      return false;
    }

    // Exact city match (normalized via CityUtils)
    String cityA = a.getCity();
    String cityB = b.getCity();
    if (cityA == null || cityB == null || cityA.isBlank() || cityB.isBlank()) {
      return false;
    }
    String normCityA = CityUtils.normalizeCityKey(cityA);
    String normCityB = CityUtils.normalizeCityKey(cityB);
    if (!normCityA.equals(normCityB)) {
      return false;
    }

    // Exact canonical propertyType match (focusing strictly on LAKAS, HAZ, TELEK)
    PropertyType typeA = a.getPropertyType();
    PropertyType typeB = b.getPropertyType();
    if (typeA == null || typeB == null || typeA != typeB) {
      return false;
    }
    if (typeA != PropertyType.LAKAS && typeA != PropertyType.HAZ && typeA != PropertyType.TELEK) {
      return false;
    }

    // Floor area within ±5% tolerance
    Double areaA = a.getDetailsInfo() != null ? a.getDetailsInfo().getAreaSquareMeters() : null;
    Double areaB = b.getDetailsInfo() != null ? b.getDetailsInfo().getAreaSquareMeters() : null;
    if (areaA == null || areaB == null || areaA <= 0 || areaB <= 0) {
      return false;
    }
    double maxArea = Math.max(areaA, areaB);
    double areaDiffRatio = Math.abs(areaA - areaB) / maxArea;
    if (areaDiffRatio > MAX_AREA_TOLERANCE_RATIO) {
      return false;
    }

    // Room count check (identical or within ±0.5 rooms; conflict if >= 1)
    if (typeA == PropertyType.TELEK) {
      Double roomsA = a.getDetailsInfo() != null ? a.getDetailsInfo().getRoomCount() : null;
      Double roomsB = b.getDetailsInfo() != null ? b.getDetailsInfo().getRoomCount() : null;
      if (roomsA != null && roomsB != null && roomsA > 0 && roomsB > 0) {
        if (Math.abs(roomsA - roomsB) >= 1.0) {
          return false;
        }
      }
    } else {
      Double roomsA = a.getDetailsInfo() != null ? a.getDetailsInfo().getRoomCount() : null;
      Double roomsB = b.getDetailsInfo() != null ? b.getDetailsInfo().getRoomCount() : null;
      if (roomsA == null || roomsB == null || roomsA <= 0 || roomsB <= 0) {
        return false;
      }
      double roomDiff = Math.abs(roomsA - roomsB);
      if (roomDiff > MAX_ROOM_DIFFERENCE) {
        return false;
      }
    }

    // Price sanity: within ±20% price band
    Long priceA = a.getPriceInfo() != null ? a.getPriceInfo().getAmountHuf() : null;
    Long priceB = b.getPriceInfo() != null ? b.getPriceInfo().getAmountHuf() : null;
    if (priceA == null || priceB == null || priceA <= 0 || priceB <= 0) {
      return false;
    }
    long maxPrice = Math.max(priceA, priceB);
    double priceDiffRatio = (double) Math.abs(priceA - priceB) / (double) maxPrice;
    if (priceDiffRatio > MAX_PRICE_BAND_RATIO) {
      return false;
    }

    return true;
  }

  private IngatlanItem consolidateCluster(List<IngatlanItem> cluster) {
    IngatlanItem primary = findLowestActivePriceSource(cluster);

    List<SourceListing> sourceListings = cluster.stream()
        .filter(Objects::nonNull)
        .map(this::toSourceListing)
        .sorted(Comparator.comparing(
            SourceListing::getPriceHuf,
            Comparator.nullsLast(Long::compareTo)))
        .toList();

    primary.setSourcesCount(cluster.size());
    primary.setSourceListings(sourceListings);

    // If primary has no image, adopt image from any cluster member that has one
    if (primary.getImageUrl() == null || primary.getImageUrl().isBlank()) {
      for (IngatlanItem member : cluster) {
        if (member != null && member.getImageUrl() != null && !member.getImageUrl().isBlank()) {
          primary.setImageUrl(member.getImageUrl());
          break;
        }
      }
    }

    boolean anyNew = cluster.stream().filter(Objects::nonNull).anyMatch(IngatlanItem::isNew);
    boolean allViewed = cluster.stream().filter(Objects::nonNull).allMatch(IngatlanItem::isViewed);
    primary.setNew(anyNew);
    primary.setViewed(allViewed);

    return primary;
  }

  private IngatlanItem findLowestActivePriceSource(List<IngatlanItem> cluster) {
    if (cluster == null || cluster.isEmpty()) {
      return null;
    }
    // 1. Minimum active source price
    List<IngatlanItem> activeWithPrice = cluster.stream()
        .filter(Objects::nonNull)
        .filter(i -> i.getStatus() == null || "ACTIVE".equalsIgnoreCase(i.getStatus()))
        .filter(i -> i.getPriceInfo() != null && i.getPriceInfo().getAmountHuf() != null
            && i.getPriceInfo().getAmountHuf() > 0)
        .toList();

    if (!activeWithPrice.isEmpty()) {
      return activeWithPrice.stream()
          .min(Comparator.comparingLong(i -> i.getPriceInfo().getAmountHuf()))
          .orElse(cluster.get(0));
    }

    // 2. Minimum price among all items
    List<IngatlanItem> anyWithPrice = cluster.stream()
        .filter(Objects::nonNull)
        .filter(i -> i.getPriceInfo() != null && i.getPriceInfo().getAmountHuf() != null
            && i.getPriceInfo().getAmountHuf() > 0)
        .toList();

    if (!anyWithPrice.isEmpty()) {
      return anyWithPrice.stream()
          .min(Comparator.comparingLong(i -> i.getPriceInfo().getAmountHuf()))
          .orElse(cluster.get(0));
    }

    return cluster.get(0);
  }

  public SourceListing toSourceListing(IngatlanItem item) {
    if (item == null) {
      return null;
    }
    String portal = item.getPortal();
    if ((portal == null || portal.isBlank()) && item.getSource() != null) {
      portal = item.getSource().getPortalName();
    }
    if (portal == null || portal.isBlank()) {
      portal = "Ismeretlen";
    }

    Long priceHuf = item.getPriceInfo() != null ? item.getPriceInfo().getAmountHuf() : null;
    Long pricePerM2 = null;
    if (item.getDetailsInfo() != null && item.getDetailsInfo().getPricePerSquareMeter() != null) {
      pricePerM2 = item.getDetailsInfo().getPricePerSquareMeter().longValue();
    } else if (priceHuf != null && item.getDetailsInfo() != null
        && item.getDetailsInfo().getAreaSquareMeters() != null
        && item.getDetailsInfo().getAreaSquareMeters() > 0) {
      pricePerM2 = Math.round(priceHuf / item.getDetailsInfo().getAreaSquareMeters());
    }

    return new SourceListing(
        portal,
        item.getId(),
        item.getUrl(),
        item.getTitle(),
        item.getImageUrl(),
        item.getDiscoveredAt(),
        item.getDiscoveredAt(),
        item.getStatus() != null ? item.getStatus() : "ACTIVE",
        priceHuf,
        pricePerM2,
        item.isViewed(),
        item.getViewedAt());
  }

  public void applyNewStatus(String city, Collection<IngatlanItem> items, boolean isFirstSearch) {
    if (items == null || items.isEmpty()) {
      return;
    }

    for (IngatlanItem item : items) {
      if (item == null || item.getUrl() == null) {
        continue;
      }

      if (isFirstSearch) {
        item.setNew(false);
      } else if (listingStorage != null) {
        boolean isNew = listingStorage.isNewUrl(city, item.getUrl());
        item.setNew(isNew);
      }
    }
  }
}
