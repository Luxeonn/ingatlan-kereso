package ingatlanok.persistence.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import ingatlanok.persistence.entity.SearchPresetEntity;

@Repository
public interface SearchPresetRepository extends JpaRepository<SearchPresetEntity, Long> {

  List<SearchPresetEntity> findAllByOrderByUpdatedAtDesc();
}
