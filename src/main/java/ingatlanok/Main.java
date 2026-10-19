package ingatlanok;

import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;

import ingatlanok.model.SearchCriteria;
import ingatlanok.model.SearchResult;
import ingatlanok.persistence.IngatlanRepository;
import ingatlanok.service.IngatlanSearchService;

public class Main {

  private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

  private Main() {}

  public static void main(String[] args) {
    if (isCliRequested(args)) {
      runCliMode();
    } else {
      SpringApplication.run(IngatlanApplication.class, filterLauncherArgs(args));
    }
  }

  public static boolean isCliRequested(String[] args) {
    if (args != null) {
      for (String arg : args) {
        if ("--cli".equalsIgnoreCase(arg)) {
          return true;
        }
      }
    }
    return Boolean.getBoolean("cli");
  }

  public static String[] filterLauncherArgs(String[] args) {
    if (args == null) {
      return new String[0];
    }
    return Arrays.stream(args)
        .filter(arg -> !"--web".equalsIgnoreCase(arg) && !"--cli".equalsIgnoreCase(arg))
        .toArray(String[]::new);
  }

  public static void runCliMode() {
    LOGGER.info("==================================================");
    LOGGER.info("  Ingatlan Kereső - Parancssori Keresés (CLI Mód)");
    LOGGER.info("==================================================");

    IngatlanRepository.getInstance();
    SearchCriteria criteria = IngatlanRepository.getInstance().getLastCriteria();
    criteria.setAutoOpenBrowser(true);

    SearchResult result = IngatlanSearchService.getInstance().executeSearch(criteria);

    LOGGER.info("==================================================");
    LOGGER.info("Lefutva: {}", result.getExecutedAt());
    LOGGER.info("Összesített találat: {}", result.getTotalFound());
    LOGGER.info("ÚJ hirdetések száma: {}", result.getNewCount());
    LOGGER.info("==================================================");
    LOGGER.info("Folyamat sikeresen befejeződött.");
  }
}
