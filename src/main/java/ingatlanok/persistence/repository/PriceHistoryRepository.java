package ingatlanok.persistence.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import ingatlanok.persistence.entity.PriceHistoryEntity;

@Repository
public interface PriceHistoryRepository extends JpaRepository<PriceHistoryEntity, Long> {

  @Query(
      "SELECT ph FROM PriceHistoryEntity ph WHERE ph.url = :key OR ph.propertyId = :key ORDER BY"
          + " ph.id ASC")
  List<PriceHistoryEntity> findByUrlOrPropertyId(@Param("key") String key);

  @Modifying
  @Query("DELETE FROM PriceHistoryEntity ph WHERE ph.url IN :urls")
  void deleteByUrlIn(@Param("urls") List<String> urls);
}
