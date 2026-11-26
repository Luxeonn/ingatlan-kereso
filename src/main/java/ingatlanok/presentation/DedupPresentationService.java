package ingatlanok.presentation;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import ingatlanok.common.CityUtils;
import ingatlanok.core.DataQualityEngine;
import ingatlanok.core.DeduplicationEngine;
import ingatlanok.model.DedupOverride;
import ingatlanok.model.DedupOverrideType;
import ingatlanok.model.IngatlanItem;
import ingatlanok.persistence.DedupOverrideStorage;
import ingatlanok.persistence.ListingStorage;

@Service
@RequiredArgsConstructor
public class DedupPresentationService {

  private static final int MAX_BATCH_URLS = 30;

  private final ListingStorage listingStorage;
  private final DedupOverrideStorage overrideStorage;
  private final DeduplicationEngine dedupEngine;
  private final DataQualityEngine qualityEngine;
  private final PresentationMapper presentationMapper;

  public DedupActionResponseDto splitCluster(DedupSplitRequestDto request) {
    String sourceUrl = request.sourceUrl().trim();
    List<String> clusterUrls = trimAndEnsureUniqueUrls(request.clusterUrls());

    List<String> peerUrls = new ArrayList<>();
    for (String url : clusterUrls) {
      if (!url.equals(sourceUrl)) {
        peerUrls.add(url);
      }
    }
    if (peerUrls.isEmpty()) {
      throw new IllegalArgumentException(
          "A szétválasztáshoz legalább egy másik klaszter-forrás URL szükséges");
    }

    List<String> allUrls = new ArrayList<>(peerUrls.size() + 1);
    allUrls.add(sourceUrl);
    allUrls.addAll(peerUrls);
    if (allUrls.size() > MAX_BATCH_URLS) {
      throw new IllegalArgumentException(
          "Egyszerre legfeljebb " + MAX_BATCH_URLS + " URL kezelhető");
    }

    Map<String, IngatlanItem> found = fetchAndVerifyExistingUrls(allUrls);
    String normCity = resolveNormalizedCity(found.get(sourceUrl));
    List<DedupOverride> existingOverrides = overrideStorage.getOverridesForUrls(allUrls);
    List<String> relinkedPeers = collectForceMergedPeers(sourceUrl, peerUrls, existingOverrides);

    for (String peerUrl : peerUrls) {
      overrideStorage.saveOverride(sourceUrl, peerUrl, DedupOverrideType.FORCE_SPLIT, normCity);
    }
    preserveMergedPeersAfterHubSplit(relinkedPeers, existingOverrides, normCity);

    List<IngatlanItem> reclustered = reclusterAffectedItems(allUrls);
    List<DedupOverrideDto> overrides = toOverrideDtos(overrideStorage.getOverridesForUrls(allUrls));
    return new DedupActionResponseDto(reclustered, overrides, "Forrás sikeresen leválasztva");
  }

  public DedupActionResponseDto mergeListings(DedupMergeRequestDto request) {
    List<String> urls = trimAndEnsureUniqueUrls(request.urls());
    Map<String, IngatlanItem> found = fetchAndVerifyExistingUrls(urls);
    String normCity = validateSameNormalizedCity(urls, found);

    persistMinimalSpanningMergeEdges(urls, normCity);

    List<IngatlanItem> reclustered = reclusterAffectedItems(urls);
    List<DedupOverrideDto> overrides = toOverrideDtos(overrideStorage.getOverridesForUrls(urls));
    return new DedupActionResponseDto(reclustered, overrides, "Hirdetések sikeresen összevonva");
  }

  public List<DedupOverrideDto> getOverridesForUrl(String url) {
    return getOverridesForQuery(url, null);
  }

  public List<DedupOverrideDto> getOverridesForQuery(String url, List<String> urls) {
    Set<String> queryUrls = new LinkedHashSet<>();
    if (url != null && !url.isBlank()) {
      queryUrls.add(url.trim());
    }
    if (urls != null) {
      for (String candidate : urls) {
        if (candidate != null && !candidate.isBlank()) {
          queryUrls.add(candidate.trim());
        }
      }
    }
    return toOverrideDtos(overrideStorage.getOverridesForUrls(queryUrls));
  }

  public DedupActionResponseDto deleteOverride(Long id) {
    DedupOverride existing =
        overrideStorage
            .findOverrideById(id)
            .orElseThrow(() -> new IllegalArgumentException("A felülbírálás nem található: " + id));
    overrideStorage.deleteOverrideById(id);

    List<String> seedUrls = List.of(existing.urlA(), existing.urlB());
    List<IngatlanItem> reclustered = reclusterAffectedItems(seedUrls);
    List<DedupOverrideDto> remaining =
        toOverrideDtos(overrideStorage.getOverridesForUrls(seedUrls));
    return new DedupActionResponseDto(
        reclustered, remaining, "Felülbírálás sikeresen visszavonva");
  }

  private List<String> trimAndEnsureUniqueUrls(List<String> rawUrls) {
    Set<String> unique = new LinkedHashSet<>();
    for (String raw : rawUrls) {
      String trimmed = raw.trim();
      if (!unique.add(trimmed)) {
        throw new IllegalArgumentException("Duplikált URL nem engedélyezett: " + trimmed);
      }
    }
    return new ArrayList<>(unique);
  }

  private Map<String, IngatlanItem> fetchAndVerifyExistingUrls(List<String> urls) {
    Map<String, IngatlanItem> found = listingStorage.findByUrls(urls);
    for (String url : urls) {
      if (!found.containsKey(url)) {
        throw new IllegalArgumentException("Ismeretlen hirdetés URL: " + url);
      }
    }
    return found;
  }

  private String validateSameNormalizedCity(List<String> urls, Map<String, IngatlanItem> found) {
    String expectedCity = null;
    for (String url : urls) {
      String normCity = resolveNormalizedCity(found.get(url));
      if (normCity == null || normCity.isBlank()) {
        throw new IllegalArgumentException(
            "A hirdetés települése hiányzik az összevonáshoz: " + url);
      }
      if (expectedCity == null) {
        expectedCity = normCity;
      } else if (!expectedCity.equals(normCity)) {
        throw new IllegalArgumentException(
            "Csak azonos településen belüli hirdetések vonhatók össze");
      }
    }
    return expectedCity;
  }

  private String resolveNormalizedCity(IngatlanItem item) {
    if (item == null || item.getCity() == null || item.getCity().isBlank()) {
      return null;
    }
    return CityUtils.normalizeCityKey(item.getCity());
  }

  private List<IngatlanItem> reclusterAffectedItems(Collection<String> seedUrls) {
    Set<String> expanded = expandConnectedForceMergeUrls(seedUrls);
    Map<String, IngatlanItem> itemsMap = listingStorage.findByUrls(expanded);
    if (itemsMap.isEmpty()) {
      return List.of();
    }
    List<IngatlanItem> reclustered = dedupEngine.deduplicateMultiSignal(itemsMap.values());
    qualityEngine.evaluateAll(reclustered);
    return reclustered;
  }

  private Set<String> expandConnectedForceMergeUrls(Collection<String> seedUrls) {
    Set<String> expanded = new LinkedHashSet<>(seedUrls);
    Queue<String> frontier = new ArrayDeque<>(expanded);
    while (!frontier.isEmpty()) {
      List<String> batch = new ArrayList<>();
      while (!frontier.isEmpty()) {
        batch.add(frontier.poll());
      }
      for (DedupOverride ov : overrideStorage.getOverridesForUrls(batch)) {
        if (ov.type() == DedupOverrideType.FORCE_MERGE) {
          if (expanded.add(ov.urlA())) {
            frontier.add(ov.urlA());
          }
          if (expanded.add(ov.urlB())) {
            frontier.add(ov.urlB());
          }
        }
      }
    }
    return expanded;
  }

  private void persistMinimalSpanningMergeEdges(List<String> urls, String normCity) {
    Set<String> urlSet = new LinkedHashSet<>(urls);
    List<DedupOverride> existingOverrides = overrideStorage.getOverridesForUrls(urls);
    deleteInternalForceSplits(urlSet, existingOverrides);

    Map<String, String> parent = initUnionFind(urls, urlSet, existingOverrides);
    String anchor = urls.get(0);
    for (int i = 1; i < urls.size(); i++) {
      String candidate = urls.get(i);
      if (!findRoot(parent, anchor).equals(findRoot(parent, candidate))) {
        overrideStorage.saveOverride(anchor, candidate, DedupOverrideType.FORCE_MERGE, normCity);
        unionRoots(parent, anchor, candidate);
      }
    }
  }

  private void deleteInternalForceSplits(
      Set<String> urlSet, List<DedupOverride> existingOverrides) {
    for (DedupOverride ov : existingOverrides) {
      if (ov.type() == DedupOverrideType.FORCE_SPLIT
          && ov.id() != null
          && urlSet.contains(ov.urlA())
          && urlSet.contains(ov.urlB())) {
        overrideStorage.deleteOverrideById(ov.id());
      }
    }
  }

  private List<String> collectForceMergedPeers(
      String sourceUrl, List<String> peerUrls, List<DedupOverride> existingOverrides) {
    List<String> linked = new ArrayList<>();
    for (String peer : peerUrls) {
      if (hasDirectOverride(existingOverrides, sourceUrl, peer, DedupOverrideType.FORCE_MERGE)) {
        linked.add(peer);
      }
    }
    return linked;
  }

  private void preserveMergedPeersAfterHubSplit(
      List<String> relinkedPeers, List<DedupOverride> existingOverrides, String normCity) {
    if (relinkedPeers.size() < 2) {
      return;
    }
    Set<String> peerSet = new LinkedHashSet<>(relinkedPeers);
    Map<String, String> parent = initUnionFind(relinkedPeers, peerSet, existingOverrides);
    String anchor = relinkedPeers.get(0);
    for (int i = 1; i < relinkedPeers.size(); i++) {
      String candidate = relinkedPeers.get(i);
      if (!findRoot(parent, anchor).equals(findRoot(parent, candidate))
          && !hasDirectOverride(
              existingOverrides, anchor, candidate, DedupOverrideType.FORCE_SPLIT)) {
        overrideStorage.saveOverride(anchor, candidate, DedupOverrideType.FORCE_MERGE, normCity);
        unionRoots(parent, anchor, candidate);
      }
    }
  }

  private boolean hasDirectOverride(
      List<DedupOverride> overrides, String url1, String url2, DedupOverrideType type) {
    for (DedupOverride ov : overrides) {
      if (ov.type() == type && ov.matchesPair(url1, url2)) {
        return true;
      }
    }
    return false;
  }

  private Map<String, String> initUnionFind(
      List<String> urls, Set<String> allowedSet, List<DedupOverride> existingOverrides) {
    Map<String, String> parent = new HashMap<>(urls.size());
    for (String u : urls) {
      parent.put(u, u);
    }
    for (DedupOverride ov : existingOverrides) {
      if (ov.type() == DedupOverrideType.FORCE_MERGE
          && allowedSet.contains(ov.urlA())
          && allowedSet.contains(ov.urlB())) {
        unionRoots(parent, ov.urlA(), ov.urlB());
      }
    }
    return parent;
  }

  private String findRoot(Map<String, String> parent, String url) {
    String root = url;
    while (!root.equals(parent.get(root))) {
      String grand = parent.get(parent.get(root));
      parent.put(root, grand);
      root = grand;
    }
    return root;
  }

  private void unionRoots(Map<String, String> parent, String a, String b) {
    String rootA = findRoot(parent, a);
    String rootB = findRoot(parent, b);
    if (!rootA.equals(rootB)) {
      parent.put(rootB, rootA);
    }
  }

  private List<DedupOverrideDto> toOverrideDtos(List<DedupOverride> overrides) {
    return overrides.stream().map(presentationMapper::toDedupOverrideDto).toList();
  }
}
