package ingatlanok.persistence.service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import ingatlanok.model.SearchPreset;
import ingatlanok.persistence.entity.SearchPresetEntity;
import ingatlanok.persistence.mapper.SearchPresetMapper;
import ingatlanok.persistence.repository.SearchPresetRepository;

@Service
@Transactional
@RequiredArgsConstructor
public class SearchPresetPersistenceHelper {

  private final SearchPresetRepository presetRepo;
  private final SearchPresetMapper presetMapper;
  private final ObjectMapper objectMapper;

  @Transactional(readOnly = true)
  public List<SearchPreset> getPresets() {
    return presetRepo.findAllByOrderByUpdatedAtDesc().stream()
        .map(entity -> presetMapper.toModel(entity, objectMapper))
        .toList();
  }

  @Transactional(readOnly = true)
  public Optional<SearchPreset> getPresetById(Long id) {
    if (id == null) {
      return Optional.empty();
    }
    return presetRepo.findById(id).map(entity -> presetMapper.toModel(entity, objectMapper));
  }

  public SearchPreset savePreset(SearchPreset preset) {
    if (preset == null) {
      throw new IllegalArgumentException("Preset cannot be null");
    }
    SearchPresetEntity entity = presetMapper.toNewEntity(preset, objectMapper);
    SearchPresetEntity saved = presetRepo.save(entity);
    return presetMapper.toModel(saved, objectMapper);
  }

  public Optional<SearchPreset> updatePreset(Long id, SearchPreset preset) {
    if (id == null || preset == null) {
      return Optional.empty();
    }
    return presetRepo
        .findById(id)
        .map(
            entity -> {
              presetMapper.updateEntityFromPreset(preset, entity, objectMapper);
              SearchPresetEntity updated = presetRepo.save(entity);
              return presetMapper.toModel(updated, objectMapper);
            });
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
    int updatedRows = presetRepo.updateLastUsedAt(id, Instant.now().toString());
    if (updatedRows == 0) {
      return Optional.empty();
    }
    return presetRepo.findById(id).map(entity -> presetMapper.toModel(entity, objectMapper));
  }
}
