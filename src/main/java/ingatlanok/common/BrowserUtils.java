package ingatlanok.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Desktop;
import java.net.URI;
import java.util.*;

public final class BrowserUtils {

    private static final Logger log = LoggerFactory.getLogger(BrowserUtils.class);

    private BrowserUtils() {

    }

    public static boolean openUrl(String url) {
        if (url == null || url.isBlank()) {
            return false;
        }
        String trimmed = url.trim();
        if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) {
            log.warn("Biztonsági figyelmeztetés: Csak http:// vagy https:// hivatkozás nyitható meg: {}", trimmed);
            return false;
        }
        try {
            String os = System.getProperty("os.name", "").toLowerCase();
            if (os.contains("win")) {
                new ProcessBuilder("cmd", "/c", "start", "", trimmed).start();
                return true;
            } else if (os.contains("mac")) {
                new ProcessBuilder("open", trimmed).start();
                return true;
            } else if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(new URI(trimmed));
                return true;
            } else {
                new ProcessBuilder("xdg-open", trimmed).start();
                return true;
            }
        } catch (Exception e) {
            log.error("Nem sikerült megnyitni a böngészőben: {} ({})", trimmed, e.getMessage(), e);
            return false;
        }
    }

    public static List<String> openUrls(Collection<String> urlList, int maxToOpen) {
        List<String> opened = new ArrayList<>();
        if (urlList == null || urlList.isEmpty()) {
            return opened;
        }

        for (String url : urlList) {
            if (opened.size() >= maxToOpen) {
                break;
            }
            if (openUrl(url)) {
                opened.add(url);
            }
        }

        if (urlList.size() > maxToOpen) {
            log.info("Megjegyzés: Az első {} új link került megnyitásra a böngészőben, a többi elérhető a felületen.", maxToOpen);
        }

        return opened;
    }

    public static List<String> openUrls(Collection<String> urlList) {
        int max = Integer.getInteger("max.open.browser", 5);
        return openUrls(urlList, max);
    }
}
