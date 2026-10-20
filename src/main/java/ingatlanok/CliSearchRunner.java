package ingatlanok;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import ingatlanok.core.SearchExecutionEngine;
import ingatlanok.model.SearchCriteria;
import ingatlanok.persistence.ConfigStorage;

@Component
@ConditionalOnProperty(name = "cli", havingValue = "true")
public class CliSearchRunner implements ApplicationRunner {

  private static final Logger LOGGER = LoggerFactory.getLogger(CliSearchRunner.class);

  private final SearchExecutionEngine searchEngine;
  private final ConfigStorage configStorage;

  public CliSearchRunner(SearchExecutionEngine searchEngine, ConfigStorage configStorage) {
    this.searchEngine = searchEngine;
    this.configStorage = configStorage;
  }

  @Override
  public void run(ApplicationArguments args) {
    SearchCriteria criteria = configStorage.getLastCriteria();
    LOGGER.info("CLI keresés indítása, kritériumok: {}", criteria);
    var result = searchEngine.executeSearch(criteria);
    LOGGER.info("CLI keresés kész: {} találat, {} új hirdetés", result.getTotalFound(), result.getNewCount());
  }
}
