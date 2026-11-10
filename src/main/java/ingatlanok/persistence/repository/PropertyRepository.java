package ingatlanok.persistence.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import ingatlanok.persistence.entity.PropertyEntity;

@Repository
public interface PropertyRepository
    extends JpaRepository<PropertyEntity, Long>, JpaSpecificationExecutor<PropertyEntity> {

  Optional<PropertyEntity> findByUrl(String url);

  List<PropertyEntity> findByUrlIn(java.util.Collection<String> urls);

  boolean existsByUrl(String url);

  boolean existsByUrlAndNormalizedCity(String url, String normalizedCity);

  boolean existsByNormalizedCity(String normalizedCity);

  List<PropertyEntity> findByNormalizedCityOrderByDiscoveredAtDesc(String normalizedCity);

  List<PropertyEntity> findByNormalizedCityAndHiddenFalseOrderByDiscoveredAtDesc(
      String normalizedCity);

  List<PropertyEntity> findAllByOrderByDiscoveredAtDesc();

  List<PropertyEntity> findAllByHiddenFalseOrderByDiscoveredAtDesc();

  Page<PropertyEntity> findAll(Specification<PropertyEntity> specification, Pageable pageable);

  List<PropertyEntity> findByPriceDeltaLessThanAndCityContainingIgnoreCaseOrderByDiscoveredAtDesc(
      long priceDelta, String city);

  List<PropertyEntity> findByHiddenTrueOrderByDiscoveredAtDesc();

  @Modifying
  @Query("UPDATE PropertyEntity p SET p.viewed = true, p.viewedAt = :viewedAt WHERE p.url = :url")
  int markAsViewed(@Param("url") String url, @Param("viewedAt") String viewedAt);

  @Modifying
  @Query("UPDATE PropertyEntity p SET p.hidden = true, p.hiddenAt = :hiddenAt WHERE p.url = :url")
  int markAsHidden(@Param("url") String url, @Param("hiddenAt") String hiddenAt);

  @Modifying
  @Query(
      "UPDATE PropertyEntity p SET p.hidden = true, p.hiddenAt = :hiddenAt"
          + " WHERE p.propertyId = :key OR p.url = :key OR p.canonicalId = :key")
  int markKeyAsHidden(@Param("key") String key, @Param("hiddenAt") String hiddenAt);

  @Modifying
  @Query("UPDATE PropertyEntity p SET p.hidden = false, p.hiddenAt = null WHERE p.url = :url")
  int restoreFromHidden(@Param("url") String url);

  @Modifying
  @Query(
      "UPDATE PropertyEntity p SET p.hidden = false, p.hiddenAt = null"
          + " WHERE p.propertyId = :key OR p.url = :key OR p.canonicalId = :key")
  int restoreKeyFromHidden(@Param("key") String key);

  @Modifying
  @Query("DELETE FROM PropertyEntity p WHERE p.normalizedCity = :normalizedCity")
  void deleteByNormalizedCity(@Param("normalizedCity") String normalizedCity);

  @Query("SELECT p.url FROM PropertyEntity p")
  List<String> findAllUrls();

  @Query(
      "SELECT new ingatlanok.persistence.repository.PortalCountProjection(p.portal, COUNT(p))"
          + " FROM PropertyEntity p GROUP BY p.portal")
  List<PortalCountProjection> countByPortalGroup();

  @Query(
      "SELECT new ingatlanok.persistence.repository.CityCountProjection(p.city, p.normalizedCity, COUNT(p))"
          + " FROM PropertyEntity p GROUP BY p.city, p.normalizedCity")
  List<CityCountProjection> countByCityGroup();

  @Query("SELECT MAX(p.discoveredAt) FROM PropertyEntity p")
  String findLatestDiscoveredAt();
}
