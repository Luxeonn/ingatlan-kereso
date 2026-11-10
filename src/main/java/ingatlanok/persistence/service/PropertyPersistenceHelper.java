package ingatlanok.persistence.service;

import java.util.List;

import ingatlanok.model.IngatlanItem;
import ingatlanok.model.ListingObservation;
import ingatlanok.model.ListingStatus;
import ingatlanok.model.PriceInfo;
import ingatlanok.persistence.entity.ListingObservationEntity;
import ingatlanok.persistence.entity.PropertyEntity;
import ingatlanok.persistence.mapper.JpaEntityMapper;
import ingatlanok.persistence.mapper.ListingObservationContext;
import ingatlanok.persistence.mapper.PriceHistoryContext;
import ingatlanok.persistence.mapper.PropertyUpdateContext;
import ingatlanok.persistence.repository.ListingObservationRepository;
import ingatlanok.persistence.repository.PriceHistoryRepository;
import ingatlanok.persistence.repository.PropertyRepository;
import ingatlanok.persistence.repository.StatusLogRepository;
import ingatlanok.persistence.service.PriceHistoryTracker.ExistingPriceState;
import ingatlanok.persistence.service.PriceHistoryTracker.PriceDeltaCalculation;
import ingatlanok.persistence.service.StatusLogTracker.StatusDecision;

/**
 * Coordinates entity persistence, price history snapshots, status logs, and periodic listing
 * observations for {@link ingatlanok.persistence.JpaIngatlanStorage}.
 */
public class PropertyPersistenceHelper {

  private final PropertyRepository propertyRepo;
  private final PriceHistoryRepository priceHistoryRepo;
  private final StatusLogRepository statusLogRepo;
  private final ListingObservationRepository listingObservationRepo;
  private final JpaEntityMapper mapper;
  private final PriceHistoryTracker priceTracker;
  private final StatusLogTracker statusTracker;

  public PropertyPersistenceHelper(
      PropertyRepository propertyRepo,
      PriceHistoryRepository priceHistoryRepo,
      StatusLogRepository statusLogRepo,
      ListingObservationRepository listingObservationRepo,
      JpaEntityMapper mapper,
      PriceHistoryTracker priceTracker,
      StatusLogTracker statusTracker) {
    this.propertyRepo = propertyRepo;
    this.priceHistoryRepo = priceHistoryRepo;
    this.statusLogRepo = statusLogRepo;
    this.listingObservationRepo = listingObservationRepo;
    this.mapper = mapper;
    this.priceTracker = priceTracker;
    this.statusTracker = statusTracker;
  }

  public void saveItem(IngatlanItem item, String now) {
    mapper.normalizeItem(item);
    String url = item.getUrl().trim();
    PropertyEntity existing = propertyRepo.findByUrl(url).orElse(null);
    String propId =
        existing != null
            ? existing.getPropertyId()
            : mapper.generatePropertyId(item.getPortal(), item.getId(), url);
    PriceDeltaCalculation calc = calculatePriceDelta(item, existing, now);

    recordPriceHistory(propId, url, now, item, calc);
    recordStatusLog(propId, url, now, existing, item, calc);
    recordListingObservation(propId, url, now, existing, item, calc);
    persistProperty(item, existing, now, calc);
  }

  private PriceDeltaCalculation calculatePriceDelta(
      IngatlanItem item, PropertyEntity existing, String now) {
    PriceInfo pi = item.getPriceInfo();
    Long priceAmount = pi != null ? pi.getAmountHuf() : null;
    Long prevAmount = existing != null ? existing.getPriceAmount() : null;
    ExistingPriceState state =
        new ExistingPriceState(
            existing == null,
            prevAmount,
            existing != null ? existing.getPriceDelta() : null,
            existing != null ? existing.getPriceChangePercent() : null,
            existing != null ? existing.getLastPriceChangeAt() : null,
            item.getStatus());
    return priceTracker.calculate(state, priceAmount, item.getTitle(), now);
  }

  private void recordPriceHistory(
      String propId, String url, String now, IngatlanItem item, PriceDeltaCalculation calc) {
    PriceInfo pi = item.getPriceInfo();
    Long priceAmount = pi != null ? pi.getAmountHuf() : null;
    if (priceAmount == null || priceAmount <= 0) {
      return;
    }
    Double m = pi.getAmountMillion();
    String raw =
        pi.getRawText() != null
            ? pi.getRawText()
            : (item.getPrice() != null ? item.getPrice() : "");
    Long perSqm =
        item.getDetailsInfo() != null ? item.getDetailsInfo().getPricePerSquareMeter() : null;

    if (calc.isNewPriceRecord()) {
      priceHistoryRepo.save(
          mapper.createPriceHistoryEntity(
              new PriceHistoryContext(
                  propId,
                  url,
                  item.getDiscoveredAt() != null ? item.getDiscoveredAt() : now,
                  raw,
                  priceAmount,
                  m,
                  perSqm,
                  null,
                  0L,
                  0.0)));
    } else if (calc.priceChanged()) {
      priceHistoryRepo.save(
          mapper.createPriceHistoryEntity(
              new PriceHistoryContext(
                  propId,
                  url,
                  now,
                  raw,
                  priceAmount,
                  m,
                  perSqm,
                  calc.previousPriceAmount(),
                  calc.delta(),
                  calc.changePercent())));
    }
  }

  private void recordStatusLog(
      String propId,
      String url,
      String now,
      PropertyEntity existing,
      IngatlanItem item,
      PriceDeltaCalculation calc) {
    StatusDecision sd =
        statusTracker.evaluate(
            existing == null,
            existing != null ? existing.getStatus() : null,
            calc.resolvedStatus(),
            existing == null && item.getDiscoveredAt() != null ? item.getDiscoveredAt() : now);
    if (sd.shouldRecord()) {
      statusLogRepo.save(
          mapper.createStatusLogEntity(propId, url, sd.status(), sd.changedAt(), sd.note()));
    }
  }

  private void recordListingObservation(
      String propId,
      String url,
      String now,
      PropertyEntity existing,
      IngatlanItem item,
      PriceDeltaCalculation calc) {
    if (listingObservationRepo == null) {
      return;
    }
    String statusEvent = determineStatusEvent(existing, calc);
    String obsTime =
        existing == null && item.getDiscoveredAt() != null ? item.getDiscoveredAt() : now;
    Long priceAmount = item.getPriceInfo() != null ? item.getPriceInfo().getAmountHuf() : null;
    Long perSqm =
        item.getDetailsInfo() != null ? item.getDetailsInfo().getPricePerSquareMeter() : null;

    ListingObservationEntity obs =
        mapper.createListingObservationEntity(
            new ListingObservationContext(
                propId,
                url,
                existing != null ? existing.getCanonicalId() : null,
                obsTime,
                priceAmount,
                perSqm,
                statusEvent,
                calc.delta(),
                calc.changePercent()));
    listingObservationRepo.save(obs);
  }

  private String determineStatusEvent(PropertyEntity existing, PriceDeltaCalculation calc) {
    if (existing == null) {
      return ListingObservation.StatusEvent.NEW.name();
    }
    if (calc.priceChanged()) {
      return ListingObservation.StatusEvent.PRICE_CHANGED.name();
    }
    boolean wasInactive =
        existing.getStatus() == ListingStatus.INACTIVE
            || existing.getStatus() == ListingStatus.ARCHIVED;
    if (wasInactive && calc.resolvedStatus() == ListingStatus.ACTIVE) {
      return ListingObservation.StatusEvent.RELISTED.name();
    }
    return ListingObservation.StatusEvent.UNCHANGED.name();
  }

  private void persistProperty(
      IngatlanItem item, PropertyEntity existing, String now, PriceDeltaCalculation calc) {
    PropertyUpdateContext ctx =
        new PropertyUpdateContext(
            now,
            calc.delta(),
            calc.changePercent(),
            calc.lastPriceChangeAt(),
            calc.resolvedStatus());
    propertyRepo.save(mapper.toEntity(item, existing, ctx));
  }

  public void deleteCityRelatedHistory(List<String> urls) {
    if (urls == null || urls.isEmpty()) {
      return;
    }
    priceHistoryRepo.deleteByUrlIn(urls);
    statusLogRepo.deleteByUrlIn(urls);
    if (listingObservationRepo != null) {
      listingObservationRepo.deleteByUrlIn(urls);
    }
  }

  public void deleteAllHistory() {
    propertyRepo.deleteAll();
    priceHistoryRepo.deleteAll();
    statusLogRepo.deleteAll();
    if (listingObservationRepo != null) {
      listingObservationRepo.deleteAll();
    }
  }
}
