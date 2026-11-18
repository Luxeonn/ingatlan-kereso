package ingatlanok.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;

import ingatlanok.persistence.ListingStorage;
import ingatlanok.testsupport.TestFixtures;

@SpringBootTest
@DisplayName("CacheConfigIntegrationTest - Spring Boot Caffeine Cache auto-konfiguráció (#473)")
class CacheConfigIntegrationTest {

  @Autowired private CacheManager cacheManager;
  @Autowired private ListingStorage listingStorage;

  @Test
  @DisplayName("CaffeineCacheManager automatikusan konfigurálva a portals és cities cache-ekre")
  void testCaffeineCacheAutoConfigured() {
    assertInstanceOf(CaffeineCacheManager.class, cacheManager);
    assertEquals(Set.of("portals", "cities"), Set.copyOf(cacheManager.getCacheNames()));
    assertNotNull(cacheManager.getCache("portals"));
    assertNotNull(cacheManager.getCache("cities"));
  }

  @Test
  @DisplayName("A ListingStorage @CacheEvict törli a portals és cities cache bejegyzéseket")
  void testCacheEvictClearsPortalsAndCitiesCaches() {
    Cache portals = cacheManager.getCache("portals");
    Cache cities = cacheManager.getCache("cities");
    assertNotNull(portals);
    assertNotNull(cities);

    portals.put("test-key", "cached-portals");
    cities.put("test-key", "cached-cities");
    assertEquals("cached-portals", portals.get("test-key", String.class));
    assertEquals("cached-cities", cities.get("test-key", String.class));

    listingStorage.saveAll(List.of(TestFixtures.validResidentialItem("cache-evict-1")));

    assertNull(portals.get("test-key"));
    assertNull(cities.get("test-key"));
  }
}
