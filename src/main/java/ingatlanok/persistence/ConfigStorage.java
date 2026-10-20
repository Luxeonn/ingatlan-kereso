package ingatlanok.persistence;

import ingatlanok.model.SearchCriteria;

public interface ConfigStorage {

  SearchCriteria getLastCriteria();

  void saveLastCriteria(SearchCriteria criteria);
}
