package ingatlanok.persistence.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import ingatlanok.model.SearchCriteria;
import ingatlanok.model.SearchPreset;
import ingatlanok.persistence.entity.SearchPresetEntity;
import ingatlanok.persistence.repository.SearchPresetRepository;

@Service
@Transactional
public class SearchPresetPersistenceHelper {

  private final SearchPresetRepository presetRepo;
  private final ObjectMapper objectMapper;

  public SearchPresetPersistenceHelper(
      SearchPresetRepository presetRepo, ObjectMapper objectMapper) {
    this.presetRepo = presetRepo;
    this.objectMapper = objectMapper;
  }

  @Transactional(readOnly = true)
  public List<SearchPreset> getPresets() {
    List<SearchPresetEntity> entities = presetRepo.findAllByOrderByUpdatedAtDesc();
    List<SearchPreset> result = new ArrayList<>(entities.size());
    for (SearchPresetEntity entity : entities) {
      result.add(toModel(entity));
    }
    return result;
  }

  @Transactional(readOnly = true)
  public Optional<SearchPreset> getPresetById(Long id) {
    if (id == null) {
      return Optional.empty();
    }
    return presetRepo.findById(id).map(this::toModel);
  }

  public SearchPreset savePreset(SearchPreset preset) {
    if (preset == null) {
      throw new IllegalArgumentException("Preset cannot be null");
    }
    String now = Instant.now().toString();
    SearchPresetEntity entity =
        SearchPresetEntity.builder()
            .name(preset.name() != null ? preset.name().trim() : "Névtelen sablon")
            .criteriaJson(toJson(preset.criteria()))
            .createdAt(now)
            .updatedAt(now)
            .lastUsedAt(preset.lastUsedAt())
            .build();
    SearchPresetEntity saved = presetRepo.save(entity);
    return toModel(saved);
  }

  public Optional<SearchPreset> updatePreset(Long id, SearchPreset preset) {
    if (id == null || preset == null) {
      return Optional.empty();
    }
    Optional<SearchPresetEntity> existingOpt = presetRepo.findById(id);
    if (existingOpt.isEmpty()) {
      return Optional.empty();
    }
    SearchPresetEntity entity = existingOpt.get();
    if (preset.name() != null && !preset.name().isBlank()) {
      entity.setName(preset.name().trim());
    }
    if (preset.criteria() != null) {
      entity.setCriteriaJson(toJson(preset.criteria()));
    }
    entity.setUpdatedAt(Instant.now().toString());
    SearchPresetEntity updated = presetRepo.save(entity);
    return Optional.of(toModel(updated));
  }

  public boolean deletePreset(Long id) {
    if (id == null || !presetRepo.existsById(id)) {
      return false;
    }
    presetRepo.deleteById(id);
    return true;
  }

  public Optional<SearchPreset> markPresetAsUsed(Long id) {
    if (id == null) {
      return Optional.empty();
    }
    Optional<SearchPresetEntity> existingOpt = presetRepo.findById(id);
    if (existingOpt.isEmpty()) {
      return Optional.empty();
    }
    SearchPresetEntity entity = existingOpt.get();
    entity.setLastUsedAt(Instant.now().toString());
    SearchPresetEntity updated = presetRepo.save(entity);
    return Optional.of(toModel(updated));
  }

  private SearchPreset toModel(SearchPresetEntity entity) {
    if (entity == null) {
      return null;
    }
    SearchCriteria criteria = null;
    if (entity.getCriteriaJson() != null && !entity.getCriteriaJson().isBlank()) {
      try {
        criteria = objectMapper.readValue(entity.getCriteriaJson(), SearchCriteria.class);
      } catch (Exception e) {
        criteria = new SearchCriteria();
      }
    } else {
      criteria = new SearchCriteria();
    }
    return new SearchPreset(
        entity.getId(),
        entity.getName(),
        criteria,
        entity.getCreatedAt(),
        entity.getUpdatedAt(),
        entity.getLastUsedAt());
  }

  private String toJson(SearchCriteria criteria) {
    if (criteria == null) {
      return "{}";
    }
    try {
      return objectMapper.writeValueAsString(criteria);
    } catch (Exception e) {
      return "{}";
    }
  }
}
