package ingatlanok.core;

import ingatlanok.model.IngatlanItem;
import ingatlanok.persistence.ListingStorage;

import java.util.*;

/**
 * Duplikátumszűrésért, új hirdetés státusz meghatározásáért és baseline snapshot kezelésért felelős motor.
 */
public class DeduplicationEngine {

    private final ListingStorage listingStorage;

    public DeduplicationEngine(ListingStorage listingStorage) {
        this.listingStorage = listingStorage;
    }

    /**
     * Eltávolítja az azonos URL-ű elemeket a listából a sorrend megőrzésével.
     */
    public List<IngatlanItem> deduplicateByUrl(Collection<IngatlanItem> items) {
        if (items == null || items.isEmpty()) {
            return Collections.emptyList();
        }
        Map<String, IngatlanItem> unique = new LinkedHashMap<>();
        for (IngatlanItem item : items) {
            if (item != null && item.getUrl() != null) {
                unique.putIfAbsent(item.getUrl(), item);
            }
        }
        return new ArrayList<>(unique.values());
    }

    /**
     * Beállítja az elemek új találat (newItem) jelölését.
     * Ha a város először lett lekérdezve (baseline snapshot), minden találat isNew = false értéket kap.
     * Egyébként a ListingStorage isNewUrl vizsgálata alapján kerül beállításra.
     */
    public void applyNewStatus(String city, Collection<IngatlanItem> items, boolean isFirstSearch) {
        if (items == null || items.isEmpty()) {
            return;
        }

        for (IngatlanItem item : items) {
            if (item == null || item.getUrl() == null) continue;

            if (isFirstSearch) {
                item.setNew(false);
            } else if (listingStorage != null) {
                boolean isNew = listingStorage.isNewUrl(city, item.getUrl());
                item.setNew(isNew);
            }
        }
    }
}
