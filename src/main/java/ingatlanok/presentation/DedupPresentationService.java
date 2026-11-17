package ingatlanok.presentation;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
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
public class DedupPresentationService {

  private static final int MAX_BATCH_URLS = 30;

  private final ListingStorage listingStorage;
  private final DedupOverrideStorage overrideStorage;
  private final DeduplicationEngine dedupEngine;
  private final DataQualityEngine qualityEngine;

  @Autowired
  public DedupPresentationService(
      ListingStorage listingStorage,
      DedupOverrideStorage overrideStorage,
      DeduplicationEngine dedupEngine,
      DataQualityEngine qualityEngine) {
    this.listingStorage = Objects.requireNonNull(listingStorage);
    this.overrideStorage = Objects.requireNonNull(overrideStorage);
    this.dedupEngine = Objects.requireNonNull(dedupEngine);
    this.qualityEngine = Objects.requireNonNull(qualityEngine);
  }

  public DedupActionResponseDto splitCluster(DedupSplitRequestDto request) {
    if (request == null || request.sourceUrl() == null || request.sourceUrl().isBlank()) {
      throw new IllegalArgumentException("A leválasztandó forrás URL kötelező");
    }
    String sourceUrl = request.sourceUrl().trim();
    List<String> clusterUrls = sanitizeAndValidateUrls(request.clusterUrls(), 1, MAX_BATCH_URLS);

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

    for (String peerUrl : peerUrls) {
      overrideStorage.saveOverride(sourceUrl, peerUrl, DedupOverrideType.FORCE_SPLIT, normCity);
    }

    List<IngatlanItem> reclustered = reclusterAffectedItems(allUrls);
    List<DedupOverrideDto> overrides = toOverrideDtos(overrideStorage.getOverridesForUrls(allUrls));
    return new DedupActionResponseDto(reclustered, overrides, "Forrás sikeresen leválasztva");
  }

  public DedupActionResponseDto mergeListings(DedupMergeRequestDto request) {
    if (request == null) {
      throw new IllegalArgumentException("Az összevonási kérés törzse kötelező");
    }
    List<String> urls = sanitizeAndValidateUrls(request.urls(), 2, MAX_BATCH_URLS);
    Map<String, IngatlanItem> found = fetchAndVerifyExistingUrls(urls);
    String normCity = validateSameNormalizedCity(urls, found);

    for (int i = 0; i < urls.size(); i++) {
      for (int j = i + 1; j < urls.size(); j++) {
        overrideStorage.saveOverride(
            urls.get(i), urls.get(j), DedupOverrideType.FORCE_MERGE, normCity);
      }
    }

    List<IngatlanItem> reclustered = reclusterAffectedItems(urls);
    List<DedupOverrideDto> overrides = toOverrideDtos(overrideStorage.getOverridesForUrls(urls));
    return new DedupActionResponseDto(reclustered, overrides, "Hirdetések sikeresen összevonva");
  }

  public List<DedupOverrideDto> getOverridesForUrl(String url) {
    if (url == null || url.isBlank()) {
      throw new IllegalArgumentException("A lekérdezett URL paraméter kötelező");
    }
    return toOverrideDtos(overrideStorage.getOverridesForUrl(url.trim()));
  }

  public DedupActionResponseDto deleteOverride(Long id) {
    if (id == null || id <= 0) {
      throw new IllegalArgumentException("Érvénytelen felülbírálás azonosító");
    }
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

  private List<String> sanitizeAndValidateUrls(
      List<String> rawUrls, int minSize, int maxSize) {
    if (rawUrls == null || rawUrls.size() < minSize) {
      throw new IllegalArgumentException(
          "Legalább " + minSize + " érvényes URL megadása kötelező");
    }
    if (rawUrls.size() > maxSize) {
      throw new IllegalArgumentException(
          "Egyszerre legfeljebb " + maxSize + " URL adható meg");
    }
    Set<String> unique = new LinkedHashSet<>();
    for (String raw : rawUrls) {
      if (raw == null || raw.isBlank()) {
        throw new IllegalArgumentException("Üres URL nem engedélyezett");
      }
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

  private List<DedupOverrideDto> toOverrideDtos(List<DedupOverride> overrides) {
    return overrides.stream().map(PresentationMapper.INSTANCE::toDedupOverrideDto).toList();
  }
}
