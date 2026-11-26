package ingatlanok.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ingatlanok.core.dedup.ClusterConsolidationHelper;
import ingatlanok.core.dedup.ClusterMatchScorer;
import ingatlanok.core.dedup.DedupOverrides;
import ingatlanok.core.dedup.PairwiseMatchEvaluator;
import ingatlanok.model.IngatlanItem;
import ingatlanok.model.ListingStatus;
import ingatlanok.model.SourceListing;
import ingatlanok.persistence.DedupOverrideStorage;
import ingatlanok.persistence.ListingStorage;

@Service
public class DeduplicationEngine {

  private final ListingStorage listingStorage;
  private final DedupOverrideStorage dedupOverrideStorage;

  public DeduplicationEngine(
      ListingStorage listingStorage,
      @Autowired(required = false) DedupOverrideStorage dedupOverrideStorage) {
    this.listingStorage = listingStorage;
    this.dedupOverrideStorage = dedupOverrideStorage;
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

    DedupOverrides overrides = loadOverrides();

    // Stage 2: Score-ordered cross-portal multi-signal clustering (order-independent best match)
    List<List<IngatlanItem>> clusters =
        ClusterMatchScorer.buildBestMatchClusters(
            stage1Items, (a, b) -> isMatch(a, b, overrides), overrides);

    // Stage 3: Consolidation into grouped IngatlanItem
    List<IngatlanItem> consolidatedItems = new ArrayList<>(clusters.size());
    for (List<IngatlanItem> cluster : clusters) {
      consolidatedItems.add(consolidateCluster(cluster, overrides));
    }

    return consolidatedItems;
  }

  private DedupOverrides loadOverrides() {
    if (dedupOverrideStorage == null) {
      return DedupOverrides.EMPTY;
    }
    return DedupOverrides.of(dedupOverrideStorage.getAllOverrides());
  }

  public List<IngatlanItem> deduplicate(Collection<IngatlanItem> items) {
    return deduplicateMultiSignal(items);
  }

  public boolean isMatch(IngatlanItem a, IngatlanItem b) {
    return isMatch(a, b, DedupOverrides.EMPTY);
  }

  public boolean isMatch(IngatlanItem a, IngatlanItem b, DedupOverrides overrides) {
    if (a == null || b == null) {
      return false;
    }
    DedupOverrides effectiveOverrides = overrides != null ? overrides : DedupOverrides.EMPTY;
    if (effectiveOverrides.isForceSplit(a.getUrl(), b.getUrl())) {
      return false;
    }
    if (effectiveOverrides.isForceMerge(a.getUrl(), b.getUrl())
        || effectiveOverrides.isTransitivelyForceMerged(a.getUrl(), b.getUrl())) {
      return PairwiseMatchEvaluator.isMatchingCity(a, b);
    }
    return PairwiseMatchEvaluator.isHeuristicMatch(a, b);
  }

  private IngatlanItem consolidateCluster(List<IngatlanItem> cluster, DedupOverrides overrides) {
    IngatlanItem primary = ClusterConsolidationHelper.findLowestActivePriceSource(cluster);

    ClusterConsolidationHelper.adoptMissingDetailsFromCluster(primary, cluster);

    Double clusterArea = primary != null && primary.getDetailsInfo() != null
        ? primary.getDetailsInfo().areaSquareMeters()
        : null;

    List<SourceListing> sourceListings = cluster.stream()
        .filter(Objects::nonNull)
        .map(item -> {
          SourceListing sl = toSourceListing(item, clusterArea);
          if (sl != null && overrides.hasAnySplit(item.getUrl())) {
            sl.setManuallySplit(true);
          }
          return sl;
        })
        .sorted(Comparator.comparing(
            SourceListing::getPriceHuf,
            Comparator.nullsLast(Long::compareTo)))
        .toList();

    primary.setSourcesCount(cluster.size());
    primary.setSourceListings(sourceListings);
    primary.setManuallyMerged(overrides.hasAnyForceMergePair(cluster));
    primary.setManuallySplit(overrides.hasAnySplitMember(cluster));

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
    boolean anyNewConstruction =
        cluster.stream().filter(Objects::nonNull).anyMatch(IngatlanItem::isNewConstruction);
    boolean anyHidden =
        cluster.stream().filter(Objects::nonNull).anyMatch(IngatlanItem::isHidden);
    primary.setNew(anyNew);
    primary.setViewed(allViewed);
    primary.setNewConstruction(anyNewConstruction);
    primary.setHidden(anyHidden);

    return primary;
  }

  public SourceListing toSourceListing(IngatlanItem item) {
    return toSourceListing(item, null);
  }

  public SourceListing toSourceListing(IngatlanItem item, Double fallbackArea) {
    if (item == null) {
      return null;
    }
    String portal = resolveSourcePortal(item);
    Long priceHuf = item.getPriceInfo() != null ? item.getPriceInfo().amountHuf() : null;
    Long pricePerM2 = ClusterConsolidationHelper.resolveSourcePricePerM2(item, priceHuf, fallbackArea);

    return SourceListing.builder()
        .portal(portal)
        .externalListingId(item.getId())
        .url(item.getUrl())
        .title(item.getTitle())
        .imageUrl(item.getImageUrl())
        .firstSeenAt(item.getDiscoveredAt())
        .lastSeenAt(item.getDiscoveredAt())
        .status(item.getStatus() != null ? item.getStatus() : ListingStatus.ACTIVE)
        .priceHuf(priceHuf)
        .pricePerM2(pricePerM2)
        .build();
  }

  private String resolveSourcePortal(IngatlanItem item) {
    String portal = item.getPortal();
    if ((portal == null || portal.isBlank()) && item.getSource() != null) {
      portal = item.getSource().portalName();
    }
    return (portal != null && !portal.isBlank()) ? portal : "Ismeretlen";
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
