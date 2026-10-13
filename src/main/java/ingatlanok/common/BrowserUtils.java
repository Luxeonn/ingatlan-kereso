package ingatlanok.common;

import java.awt.Desktop;
import java.net.URI;
import java.util.*;

public final class BrowserUtils {

    private BrowserUtils() {

    }

    public static boolean openUrl(String url) {
        if (url == null || url.isBlank()) {
            return false;
        }
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(new URI(url));
                return true;
            } else {
                new ProcessBuilder("rundll32", "url.dll,FileProtocolHandler", url).start();
                return true;
            }
        } catch (Exception e) {
            System.err.println("Nem sikerült megnyitni a böngészőben: " + url + " (" + e.getMessage() + ")");
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
            System.out.println("Megjegyzés: Az első " + maxToOpen + " új link került megnyitásra a böngészőben, a többi elérhető a felületen.");
        }

        return opened;
    }

    public static List<String> openUrls(Collection<String> urlList) {
        int max = Integer.getInteger("max.open.browser", 5);
        return openUrls(urlList, max);
    }
}
