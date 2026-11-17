package ingatlanok.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import ingatlanok.core.SearchExecutionEngine;
import ingatlanok.model.SearchCriteria;
import ingatlanok.model.SearchResult;

@Service
@RequiredArgsConstructor
public class IngatlanSearchService {

  private final SearchExecutionEngine searchEngine;

  public SearchResult executeSearch(SearchCriteria criteria) {
    return searchEngine.executeSearch(criteria);
  }
}
