package ingatlanok.persistence.mapper;

import ingatlanok.common.CityUtils;
import ingatlanok.common.PropertyParser;
import ingatlanok.model.*;
import ingatlanok.persistence.service.PriceHistoryTracker.PriceDeltaCalculation;
import org.dizitart.no2.collection.Document;

/**
 * Leképező komponens Nitrite Document és domain modellek / rekordok között.
 */
public final class NitriteDocumentMapper {

    private NitriteDocumentMapper() {}

    public static void normalizeItem(IngatlanItem item) {
        if (item == null) return;
        if (item.getPriceInfo() == null && item.getPrice() != null && !item.getPrice().isBlank()) {
            item.setPriceInfo(PropertyParser.parsePrice(item.getPrice()));
        }
        if (item.getDetailsInfo() == null && item.getDetails() != null && !item.getDetails().isBlank()) {
            item.setDetailsInfo(PropertyParser.parseDetails(item.getDetails(), item.getPriceInfo()));
        }
    }

    public static IngatlanItem toItem(Document doc) {
        if (doc == null) return null;
        IngatlanItem item = new IngatlanItem();
        item.setId(doc.get("id", String.class));
        item.setUrl(doc.get("url", String.class));
        item.setPortal(doc.get("portal", String.class));
        item.setTitle(doc.get("title", String.class));
        item.setPrice(doc.get("price", String.class));
        item.setLocation(doc.get("location", String.class));
        item.setCity(doc.get("city", String.class));
        item.setDetails(doc.get("details", String.class));
        item.setDiscoveredAt(doc.get("discoveredAt", String.class));
        Boolean isNew = doc.get("isNew", Boolean.class);
        item.setNew(isNew != null && isNew);
        Boolean viewed = doc.get("viewed", Boolean.class);
        item.setViewed(viewed != null && viewed);
        item.setViewedAt(doc.get("viewedAt", String.class));
        item.setImageUrl(doc.get("imageUrl", String.class));
        item.setStatus(doc.get("status", String.class));

        Number delta = doc.get("priceDelta", Number.class);
        if (delta != null) item.setPriceDelta(delta.longValue());
        Number changePct = doc.get("priceChangePercent", Number.class);
        if (changePct != null) item.setPriceChangePercent(changePct.doubleValue());
        item.setLastPriceChangeAt(doc.get("lastPriceChangeAt", String.class));

        Number priceAmount = doc.get("priceAmount", Number.class);
        Number priceMillions = doc.get("priceMillions", Number.class);
        if (priceAmount != null || priceMillions != null) {
            item.setPriceInfo(new PriceInfo(item.getPrice(), priceAmount != null ? priceAmount.longValue() : 0L, priceMillions != null ? priceMillions.doubleValue() : 0.0));
        }

        Number areaSqm = doc.get("areaSqm", Number.class);
        Number rooms = doc.get("rooms", Number.class);
        Number pricePerSqm = doc.get("pricePerSqm", Number.class);
        if (areaSqm != null || rooms != null || pricePerSqm != null) {
            item.setDetailsInfo(new PropertyDetails(item.getDetails(), areaSqm != null ? areaSqm.doubleValue() : 0.0, rooms != null ? rooms.doubleValue() : 0.0, pricePerSqm != null ? pricePerSqm.longValue() : 0L));
        }
        return item;
    }

    public static Document toPropertyDocument(IngatlanItem item, String propId, Document existing, PriceDeltaCalculation pc, String now) {
        String city = item.getCity() != null && !item.getCity().isBlank() ? item.getCity().trim() : "Ismeretlen";
        PriceInfo pi = item.getPriceInfo();
        PropertyDetails di = item.getDetailsInfo();
        boolean prevViewed = existing != null && Boolean.TRUE.equals(existing.get("viewed", Boolean.class));
        return toPropertyDocument(item, propId, item.getPortal() != null ? item.getPortal().trim() : "Egyéb", city,
                CityUtils.normalizeCityKey(city), item.getTitle() != null ? item.getTitle() : "",
                pi != null && pi.getRawText() != null ? pi.getRawText() : (item.getPrice() != null ? item.getPrice() : ""),
                pi != null ? pi.getAmountHuf() : null, pi != null ? pi.getAmountMillion() : null,
                di != null ? di.getPricePerSquareMeter() : null, item.getDetails() != null ? item.getDetails() : "",
                di != null ? di.getAreaSquareMeters() : null, di != null ? di.getRoomCount() : null,
                item.getDiscoveredAt() != null ? item.getDiscoveredAt() : now, prevViewed || item.isViewed(),
                prevViewed ? existing.get("viewedAt", String.class) : (item.isViewed() ? (item.getViewedAt() != null ? item.getViewedAt() : now) : null),
                pc.resolvedStatus(), pc.delta(), pc.changePercent(), pc.lastPriceChangeAt(), now);
    }

    public static Document toPropertyDocument(
            IngatlanItem item, String propertyId, String portal, String cityName, String cityKey,
            String title, String priceRaw, Long priceAmount, Double priceMillions, Long pricePerSqm,
            String details, Double areaSqm, Double rooms, String discoveredAt, boolean finalViewed,
            String finalViewedAt, String targetStatus, Long priceDelta, Double priceChangePercent,
            String lastPriceChangeAt, String now
    ) {
        return Document.createDocument("url", item.getUrl().trim())
                .put("id", propertyId).put("portal", portal).put("city", cityName).put("normalizedCity", cityKey)
                .put("location", cityName).put("title", title).put("price", priceRaw).put("priceAmount", priceAmount)
                .put("priceMillions", priceMillions).put("pricePerSqm", pricePerSqm).put("details", details)
                .put("areaSqm", areaSqm).put("rooms", rooms).put("imageUrl", item.getImageUrl())
                .put("discoveredAt", discoveredAt).put("isNew", item.isNew()).put("viewed", finalViewed)
                .put("viewedAt", finalViewedAt).put("status", targetStatus).put("priceDelta", priceDelta)
                .put("priceChangePercent", priceChangePercent).put("lastPriceChangeAt", lastPriceChangeAt).put("updatedAt", now);
    }

    public static PriceHistoryEntry toPriceHistoryEntry(Document doc, String fallbackKey) {
        Number id = doc.get("id", Number.class);
        Number priceAmount = doc.get("priceAmount", Number.class);
        Number priceMillions = doc.get("priceMillions", Number.class);
        Number pricePerSqm = doc.get("pricePerSqm", Number.class);
        Number prevAmount = doc.get("previousPriceAmount", Number.class);
        Number delta = doc.get("priceDelta", Number.class);
        Number changePct = doc.get("changePercent", Number.class);

        return new PriceHistoryEntry(
                id != null ? id.intValue() : 0,
                doc.get("propertyId", String.class) != null ? doc.get("propertyId", String.class) : fallbackKey,
                doc.get("recordedAt", String.class), doc.get("priceRaw", String.class),
                priceAmount != null ? priceAmount.longValue() : 0L,
                priceMillions != null ? priceMillions.doubleValue() : 0.0,
                pricePerSqm != null ? pricePerSqm.longValue() : null,
                prevAmount != null ? prevAmount.longValue() : null,
                delta != null ? delta.longValue() : null,
                changePct != null ? changePct.doubleValue() : null);
    }

    public static Document toPriceHistoryDocument(
            int id, String propertyId, String url, String recordedAt, String priceRaw,
            Long priceAmount, Double priceMillions, Long pricePerSqm,
            Long previousPriceAmount, Long priceDelta, Double changePercent
    ) {
        return Document.createDocument("id", id).put("propertyId", propertyId).put("url", url)
                .put("recordedAt", recordedAt).put("priceRaw", priceRaw).put("priceAmount", priceAmount)
                .put("priceMillions", priceMillions).put("pricePerSqm", pricePerSqm)
                .put("previousPriceAmount", previousPriceAmount).put("priceDelta", priceDelta).put("changePercent", changePercent);
    }

    public static StatusLogEntry toStatusLogEntry(Document doc, String fallbackKey) {
        Number id = doc.get("id", Number.class);
        return new StatusLogEntry(
                id != null ? id.intValue() : 0,
                doc.get("propertyId", String.class) != null ? doc.get("propertyId", String.class) : fallbackKey,
                doc.get("status", String.class), doc.get("changedAt", String.class), doc.get("note", String.class));
    }

    public static Document toStatusLogDocument(int id, String propertyId, String url, String status, String changedAt, String note) {
        return Document.createDocument("id", id).put("propertyId", propertyId).put("url", url)
                .put("status", status).put("changedAt", changedAt).put("note", note);
    }
}
