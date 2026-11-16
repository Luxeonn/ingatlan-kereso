package ingatlanok.persistence;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ingatlanok.common.CityUtils;
import ingatlanok.model.DedupOverride;
import ingatlanok.model.DedupOverride.UrlPair;
import ingatlanok.model.DedupOverrideType;
import ingatlanok.persistence.entity.DedupOverrideEntity;
import ingatlanok.persistence.mapper.JpaEntityMapper;
import ingatlanok.persistence.repository.DedupOverrideRepository;

@Service
@Transactional
public class JpaDedupOverrideStorage implements DedupOverrideStorage {

  private static final DateTimeFormatter DATE_TIME_FORMATTER =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

  private final DedupOverrideRepository overrideRepo;
  private final JpaEntityMapper mapper;

  public JpaDedupOverrideStorage(
      DedupOverrideRepository overrideRepo, JpaEntityMapper mapper) {
    this.overrideRepo = overrideRepo;
    this.mapper = mapper;
  }

  @Override
  @Transactional(readOnly = true)
  public List<DedupOverride> getAllOverrides() {
    return overrideRepo.findAll().stream().map(mapper::toDedupOverride).toList();
  }

  @Override
  @Transactional(readOnly = true)
  public List<DedupOverride> getOverridesForUrls(Collection<String> urls) {
    if (urls == null || urls.isEmpty()) {
      return List.of();
    }
    List<String> trimmed =
        urls.stream().filter(u -> u != null && !u.isBlank()).map(String::trim).distinct().toList();
    if (trimmed.isEmpty()) {
      return List.of();
    }
    return overrideRepo.findByUrlAInOrUrlBIn(trimmed, trimmed).stream()
        .map(mapper::toDedupOverride)
        .toList();
  }

  @Override
  @Transactional(readOnly = true)
  public List<DedupOverride> getOverridesForUrl(String url) {
    if (url == null || url.isBlank()) {
      return List.of();
    }
    String trimmed = url.trim();
    return overrideRepo.findByUrlAOrUrlB(trimmed, trimmed).stream()
        .map(mapper::toDedupOverride)
        .toList();
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<DedupOverride> findOverrideById(Long id) {
    if (id == null) {
      return Optional.empty();
    }
    return overrideRepo.findById(id).map(mapper::toDedupOverride);
  }

  @Override
  public DedupOverride saveOverride(
      String url1, String url2, DedupOverrideType type, String normalizedCity) {
    if (type == null) {
      throw new IllegalArgumentException("A felülbírálás típusa kötelező");
    }
    UrlPair pair = DedupOverride.canonicalPair(url1, url2);
    String normCity =
        normalizedCity != null && !normalizedCity.isBlank()
            ? CityUtils.normalizeCityKey(normalizedCity)
            : null;
    String now = LocalDateTime.now().format(DATE_TIME_FORMATTER);

    DedupOverrideEntity entity =
        overrideRepo
            .findByUrlAAndUrlB(pair.urlA(), pair.urlB())
            .orElseGet(
                () ->
                    DedupOverrideEntity.builder()
                        .urlA(pair.urlA())
                        .urlB(pair.urlB())
                        .build());

    entity.setType(type);
    entity.setNormalizedCity(normCity);
    entity.setCreatedAt(now);

    return mapper.toDedupOverride(overrideRepo.save(entity));
  }

  @Override
  public boolean deleteOverrideById(Long id) {
    if (id == null || !overrideRepo.existsById(id)) {
      return false;
    }
    overrideRepo.deleteById(id);
    return true;
  }

  @Override
  public void clearCityOverrides(String city) {
    if (city == null || city.isBlank()) {
      return;
    }
    overrideRepo.deleteByNormalizedCity(CityUtils.normalizeCityKey(city));
  }

  @Override
  public void clearAllOverrides() {
    overrideRepo.deleteAllInBatch();
  }
}
