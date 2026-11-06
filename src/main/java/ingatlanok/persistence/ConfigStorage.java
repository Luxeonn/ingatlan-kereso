package ingatlanok.persistence;

import java.util.List;
import java.util.Optional;

import ingatlanok.model.SearchCriteria;
import ingatlanok.model.SearchPreset;

public interface ConfigStorage {

  SearchCriteria getLastCriteria();

  void saveLastCriteria(SearchCriteria criteria);

  default List<SearchPreset> getPresets() {
    return List.of();
  }

  default Optional<SearchPreset> getPresetById(Long id) {
    return Optional.empty();
  }

  default SearchPreset savePreset(SearchPreset preset) {
    return preset;
  }

  default Optional<SearchPreset> updatePreset(Long id, SearchPreset preset) {
    return Optional.empty();
  }

  default boolean deletePreset(Long id) {
    return false;
  }

  default Optional<SearchPreset> markPresetAsUsed(Long id) {
    return Optional.empty();
  }
}
