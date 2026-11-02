package ingatlanok.persistence.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import ingatlanok.persistence.entity.ListingObservationEntity;

@Repository
public interface ListingObservationRepository extends JpaRepository<ListingObservationEntity, Long> {

  @Query(
      "SELECT lo FROM ListingObservationEntity lo WHERE lo.url = :key OR lo.propertyId = :key"
          + " OR lo.canonicalId = :key ORDER BY lo.id ASC")
  List<ListingObservationEntity> findByUrlOrPropertyIdOrCanonicalId(@Param("key") String key);

  List<ListingObservationEntity> findByUrlOrderByIdAsc(String url);

  List<ListingObservationEntity> findByCanonicalIdOrderByIdAsc(String canonicalId);

  @Modifying
  @Query("DELETE FROM ListingObservationEntity lo WHERE lo.url IN :urls")
  void deleteByUrlIn(@Param("urls") List<String> urls);
}
