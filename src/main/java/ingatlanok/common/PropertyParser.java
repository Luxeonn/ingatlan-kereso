package ingatlanok.common;

import ingatlanok.model.PriceInfo;
import ingatlanok.model.PropertyDetails;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Ingatlanhirdetések szöveges adatait (ár, alapterület, szobaszám, azonosítók)
 * kinyerő és normalizáló közös segédosztály strukturált típuskonverzióval.
 */
public final class PropertyParser {

    private static final Pattern PRICE_PATTERN = Pattern.compile("(\\d+[,.]?\\d*\\s*M\\s*Ft|\\d[\\d\\s]*\\s*Ft)", Pattern.CASE_INSENSITIVE);
    private static final Pattern DETAILS_PATTERN = Pattern.compile("(\\d+[,.]?\\d*\\s*m²[^\\n•|]*\\d+\\s*szoba|\\d+\\s*szoba[^\\d•|]*[•|]?\\s*\\d+[,.]?\\d*\\s*m²|\\d+[,.]?\\d*\\s*m²)", Pattern.CASE_INSENSITIVE);

    private static final Pattern MILLION_PRICE_PATTERN = Pattern.compile("(\\d+[,.]?\\d*)\\s*M\\s*Ft", Pattern.CASE_INSENSITIVE);
    private static final Pattern HUF_PRICE_PATTERN = Pattern.compile("(\\d[\\d\\s]*)\\s*Ft", Pattern.CASE_INSENSITIVE);

    private static final Pattern AREA_PATTERN = Pattern.compile("(\\d+[,.]?\\d*)\\s*m²", Pattern.CASE_INSENSITIVE);
    private static final Pattern ROOM_PATTERN = Pattern.compile("(\\d+[,.]?\\d*)\\s*szoba", Pattern.CASE_INSENSITIVE);

    private PropertyParser() {
        // Segédosztály, nem példányosítható
    }

    /**
     * Kinyeri az ingatlan hirdetési árát a megadott kártyaszövegből formázott szövegként.
     */
    public static String extractPrice(String text) {
        if (text == null || text.isBlank()) {
            return "Ár a hirdetésben";
        }
        Matcher matcher = PRICE_PATTERN.matcher(text);
        return matcher.find() ? matcher.group(1).trim() : "Ár a hirdetésben";
    }

    /**
     * Strukturált PriceInfo objektumot készít az ár numerikus értékeivel.
     */
    public static PriceInfo parsePrice(String text) {
        String raw = extractPrice(text);
        if ("Ár a hirdetésben".equals(raw)) {
            return new PriceInfo(raw, null, null);
        }

        Matcher mMillion = MILLION_PRICE_PATTERN.matcher(raw);
        if (mMillion.find()) {
            try {
                double million = Double.parseDouble(mMillion.group(1).replace(",", "."));
                long huf = Math.round(million * 1_000_000.0);
                return new PriceInfo(raw, huf, million);
            } catch (NumberFormatException ignored) {
            }
        }

        Matcher mHuf = HUF_PRICE_PATTERN.matcher(raw);
        if (mHuf.find()) {
            try {
                String cleanDigits = mHuf.group(1).replaceAll("\\s+", "");
                long huf = Long.parseLong(cleanDigits);
                double million = Math.round((huf / 1_000_000.0) * 100.0) / 100.0;
                return new PriceInfo(raw, huf, million);
            } catch (NumberFormatException ignored) {
            }
        }

        return new PriceInfo(raw, null, null);
    }

    /**
     * Kinyeri az alapterület és szobaszám információkat a kártyaszövegből.
     */
    public static String extractDetails(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        Matcher matcher = DETAILS_PATTERN.matcher(text);
        return matcher.find() ? matcher.group(1).trim() : "";
    }

    /**
     * Strukturált PropertyDetails objektumot készít az alapterület, szobaszám és m² ár kiszámításával.
     */
    public static PropertyDetails parseDetails(String text, PriceInfo priceInfo) {
        String raw = extractDetails(text);
        Double area = null;
        Double rooms = null;

        if (text != null) {
            Matcher mArea = AREA_PATTERN.matcher(text);
            if (mArea.find()) {
                try {
                    area = Double.parseDouble(mArea.group(1).replace(",", "."));
                } catch (NumberFormatException ignored) {
                }
            }

            Matcher mRooms = ROOM_PATTERN.matcher(text);
            if (mRooms.find()) {
                try {
                    rooms = Double.parseDouble(mRooms.group(1).replace(",", "."));
                } catch (NumberFormatException ignored) {
                }
            }
        }

        Long pricePerM2 = null;
        if (area != null && area > 0 && priceInfo != null && priceInfo.getAmountHuf() != null) {
            pricePerM2 = Math.round((double) priceInfo.getAmountHuf() / area);
        }

        return new PropertyDetails(raw, area, rooms, pricePerM2);
    }

    /**
     * Kinyeri a megadott regex minta első egyező csoportját a szövegből.
     */
    public static String extractFirstGroup(String text, Pattern pattern, String fallback) {
        if (text == null || pattern == null) {
            return fallback;
        }
        Matcher matcher = pattern.matcher(text);
        return matcher.find() ? matcher.group(1).trim() : fallback;
    }
}
