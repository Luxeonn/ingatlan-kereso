package ingatlanok.core.dedup;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import ingatlanok.model.DedupOverride;
import ingatlanok.model.DedupOverride.UrlPair;
import ingatlanok.model.DedupOverrideType;
import ingatlanok.model.IngatlanItem;

/**
 * Immutable per-run lookup table of manual deduplication overrides ({@code FORCE_SPLIT} and
 * {@code FORCE_MERGE}) loaded once before clustering.
 */
public final class DedupOverrides {

  public static final DedupOverrides EMPTY = new DedupOverrides(List.of());

  private final Map<UrlPair, DedupOverrideType> pairTypes;
  private final Set<String> splitUrls;
  private final Set<String> mergedUrls;
  private final List<UrlPair> forceMergePairs;
  private final Map<String, Integer> forceMergeComponentId;

  private DedupOverrides(Collection<DedupOverride> overrides) {
    Map<UrlPair, DedupOverrideType> map = buildPairMap(overrides);
    Set<String> splits = new HashSet<>();
    Set<String> merges = new HashSet<>();
    populateUrlSets(map, splits, merges);
    this.pairTypes = Map.copyOf(map);
    this.splitUrls = Set.copyOf(splits);
    this.mergedUrls = Set.copyOf(merges);
    this.forceMergePairs =
        map.entrySet().stream()
            .filter(e -> e.getValue() == DedupOverrideType.FORCE_MERGE)
            .map(Map.Entry::getKey)
            .toList();
    this.forceMergeComponentId = buildForceMergeComponents(this.forceMergePairs);
  }

  private static Map<String, Integer> buildForceMergeComponents(List<UrlPair> mergePairs) {
    if (mergePairs.isEmpty()) {
      return Map.of();
    }
    Map<String, List<String>> adjacency = new HashMap<>();
    for (UrlPair pair : mergePairs) {
      adjacency.computeIfAbsent(pair.urlA(), ignored -> new ArrayList<>()).add(pair.urlB());
      adjacency.computeIfAbsent(pair.urlB(), ignored -> new ArrayList<>()).add(pair.urlA());
    }
    Map<String, Integer> componentByUrl = new HashMap<>();
    int componentSeq = 0;
    for (String start : adjacency.keySet()) {
      if (!componentByUrl.containsKey(start)) {
        assignComponentBfs(start, componentSeq++, adjacency, componentByUrl);
      }
    }
    return Map.copyOf(componentByUrl);
  }

  private static void assignComponentBfs(
      String start,
      int componentId,
      Map<String, List<String>> adjacency,
      Map<String, Integer> componentByUrl) {
    Queue<String> queue = new ArrayDeque<>();
    queue.add(start);
    componentByUrl.put(start, componentId);
    while (!queue.isEmpty()) {
      String current = queue.poll();
      for (String neighbor : adjacency.getOrDefault(current, List.of())) {
        if (!componentByUrl.containsKey(neighbor)) {
          componentByUrl.put(neighbor, componentId);
          queue.add(neighbor);
        }
      }
    }
  }

  private static Map<UrlPair, DedupOverrideType> buildPairMap(
      Collection<DedupOverride> overrides) {
    Map<UrlPair, DedupOverrideType> map = new HashMap<>();
    if (overrides == null) {
      return map;
    }
    for (DedupOverride ov : overrides) {
      if (ov == null || ov.type() == null) {
        continue;
      }
      UrlPair pair = toCanonicalPair(ov.urlA(), ov.urlB());
      if (pair != null) {
        map.put(pair, ov.type());
      }
    }
    return map;
  }

  private static void populateUrlSets(
      Map<UrlPair, DedupOverrideType> map, Set<String> splits, Set<String> merges) {
    for (Map.Entry<UrlPair, DedupOverrideType> entry : map.entrySet()) {
      Set<String> target =
          entry.getValue() == DedupOverrideType.FORCE_SPLIT ? splits : merges;
      target.add(entry.getKey().urlA());
      target.add(entry.getKey().urlB());
    }
  }

  private static UrlPair toCanonicalPair(String url1, String url2) {
    if (url1 == null || url2 == null) {
      return null;
    }
    String a = url1.trim();
    String b = url2.trim();
    if (a.isEmpty() || b.isEmpty() || a.equals(b)) {
      return null;
    }
    return a.compareTo(b) < 0 ? new UrlPair(a, b) : new UrlPair(b, a);
  }

  public static DedupOverrides of(Collection<DedupOverride> overrides) {
    if (overrides == null || overrides.isEmpty()) {
      return EMPTY;
    }
    return new DedupOverrides(overrides);
  }

  public boolean isEmpty() {
    return pairTypes.isEmpty();
  }

  public boolean isForceSplit(String url1, String url2) {
    return getType(url1, url2) == DedupOverrideType.FORCE_SPLIT;
  }

  public boolean isForceMerge(String url1, String url2) {
    return getType(url1, url2) == DedupOverrideType.FORCE_MERGE;
  }

  public boolean isTransitivelyForceMerged(String url1, String url2) {
    if (forceMergeComponentId.isEmpty() || url1 == null || url2 == null) {
      return false;
    }
    String a = url1.trim();
    String b = url2.trim();
    if (a.isEmpty() || b.isEmpty() || a.equals(b) || isForceSplit(a, b)) {
      return false;
    }
    Integer compA = forceMergeComponentId.get(a);
    return compA != null && compA.equals(forceMergeComponentId.get(b));
  }

  public Integer getForceMergeComponentId(String url) {
    if (url == null || forceMergeComponentId.isEmpty()) {
      return null;
    }
    return forceMergeComponentId.get(url.trim());
  }

  public boolean hasAnySplit(String url) {
    return url != null && splitUrls.contains(url.trim());
  }

  public boolean hasAnyMerge(String url) {
    return url != null && mergedUrls.contains(url.trim());
  }

  public boolean hasAnyForceMergePair(List<IngatlanItem> cluster) {
    if (pairTypes.isEmpty() || cluster == null || cluster.size() < 2) {
      return false;
    }
    for (int i = 0; i < cluster.size(); i++) {
      IngatlanItem a = cluster.get(i);
      if (a == null) {
        continue;
      }
      for (int j = i + 1; j < cluster.size(); j++) {
        IngatlanItem b = cluster.get(j);
        if (b != null
            && (isForceMerge(a.getUrl(), b.getUrl())
                || isTransitivelyForceMerged(a.getUrl(), b.getUrl()))) {
          return true;
        }
      }
    }
    return false;
  }

  public boolean hasAnySplitMember(List<IngatlanItem> cluster) {
    if (splitUrls.isEmpty() || cluster == null) {
      return false;
    }
    for (IngatlanItem item : cluster) {
      if (item != null && hasAnySplit(item.getUrl())) {
        return true;
      }
    }
    return false;
  }

  public List<UrlPair> getForceMergePairs() {
    return forceMergePairs;
  }

  private DedupOverrideType getType(String url1, String url2) {
    if (pairTypes.isEmpty()) {
      return null;
    }
    UrlPair pair = toCanonicalPair(url1, url2);
    return pair != null ? pairTypes.get(pair) : null;
  }
}
