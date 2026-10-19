package ingatlanok.persistence;

import ingatlanok.model.SearchCriteria;

/** Keresési konfiguráció és paraméterek tárolásáért felelős interfész. */
public interface ConfigStorage {

  /** Betölti a legutóbb elmentett keresési feltételeket, vagy alapértelmezettet ad vissza. */
  SearchCriteria getLastCriteria();

  /** Elmenti a megadott keresési feltételeket. */
  void saveLastCriteria(SearchCriteria criteria);
}
