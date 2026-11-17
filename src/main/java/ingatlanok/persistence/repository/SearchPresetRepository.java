package ingatlanok.persistence.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import ingatlanok.persistence.entity.SearchPresetEntity;

@Repository
public interface SearchPresetRepository extends JpaRepository<SearchPresetEntity, Long> {

  List<SearchPresetEntity> findAllByOrderByUpdatedAtDesc();

  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query("UPDATE SearchPresetEntity p SET p.lastUsedAt = :lastUsedAt WHERE p.id = :id")
  int updateLastUsedAt(@Param("id") Long id, @Param("lastUsedAt") String lastUsedAt);
}
