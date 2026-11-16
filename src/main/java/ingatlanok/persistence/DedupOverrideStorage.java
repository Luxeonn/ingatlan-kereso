package ingatlanok.persistence;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import ingatlanok.model.DedupOverride;
import ingatlanok.model.DedupOverrideType;

/** Persistence abstraction for manual deduplication overrides ({@code FORCE_SPLIT} / {@code FORCE_MERGE}). */
public interface DedupOverrideStorage {

  List<DedupOverride> getAllOverrides();

  List<DedupOverride> getOverridesForUrls(Collection<String> urls);

  List<DedupOverride> getOverridesForUrl(String url);

  Optional<DedupOverride> findOverrideById(Long id);

  DedupOverride saveOverride(
      String url1, String url2, DedupOverrideType type, String normalizedCity);

  boolean deleteOverrideById(Long id);

  void clearCityOverrides(String city);

  void clearAllOverrides();
}
