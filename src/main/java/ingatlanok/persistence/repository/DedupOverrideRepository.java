package ingatlanok.persistence.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import ingatlanok.persistence.entity.DedupOverrideEntity;

public interface DedupOverrideRepository extends JpaRepository<DedupOverrideEntity, Long> {

  Optional<DedupOverrideEntity> findByUrlAAndUrlB(String urlA, String urlB);

  List<DedupOverrideEntity> findByUrlAInOrUrlBIn(Collection<String> urlsA, Collection<String> urlsB);

  List<DedupOverrideEntity> findByUrlAOrUrlB(String urlA, String urlB);

  void deleteByNormalizedCity(String normalizedCity);
}
