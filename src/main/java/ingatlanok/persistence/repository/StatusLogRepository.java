package ingatlanok.persistence.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import ingatlanok.persistence.entity.StatusLogEntity;

@Repository
public interface StatusLogRepository extends JpaRepository<StatusLogEntity, Long> {

  @Query(
      "SELECT sl FROM StatusLogEntity sl WHERE sl.url = :key OR sl.propertyId = :key ORDER BY sl.id"
          + " ASC")
  List<StatusLogEntity> findByUrlOrPropertyId(@Param("key") String key);

  @Modifying
  @Query("DELETE FROM StatusLogEntity sl WHERE sl.url IN :urls")
  void deleteByUrlIn(@Param("urls") List<String> urls);
}
