package ingatlanok.scraper;

import java.util.Optional;

/**
 * Backward compatibility facade for IngatlanBazarLocationResolver. Delegates to {@link
 * ingatlanok.portal.ingatlanbazar.IngatlanBazarLocationResolver}.
 */
public final class IngatlanBazarLocationResolver {

  private IngatlanBazarLocationResolver() {}

  public static void registerLocation(String key, int id) {
    ingatlanok.portal.ingatlanbazar.IngatlanBazarLocationResolver.registerLocation(key, id);
  }

  public static Optional<Integer> resolveLocationId(String cityName) {
    return ingatlanok.portal.ingatlanbazar.IngatlanBazarLocationResolver.resolveLocationId(
        cityName);
  }
}
