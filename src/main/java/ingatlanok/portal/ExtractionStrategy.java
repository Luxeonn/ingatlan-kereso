package ingatlanok.portal;

import ingatlanok.model.SearchCriteria;
import ingatlanok.model.raw.ExtractionMethod;
import ingatlanok.model.raw.RawExtractionResult;

public interface ExtractionStrategy {

  ExtractionMethod method();

  RawExtractionResult extract(SearchCriteria criteria);
}
