package ingatlanok;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import ingatlanok.core.SearchExecutionEngine;
import ingatlanok.model.SearchCriteria;
import ingatlanok.persistence.ConfigStorage;

@Slf4j
@Component
@ConditionalOnProperty(name = "cli")
@RequiredArgsConstructor
public class CliSearchRunner implements ApplicationRunner {

  private final SearchExecutionEngine searchEngine;
  private final ConfigStorage configStorage;

  @Override
  public void run(ApplicationArguments args) {
    SearchCriteria criteria = configStorage.getLastCriteria();
    log.info("CLI keresés indítása, kritériumok: {}", criteria);
    var result = searchEngine.executeSearch(criteria);
    log.info("CLI keresés kész: {} találat, {} új hirdetés", result.getTotalFound(), result.getNewCount());
  }
}
