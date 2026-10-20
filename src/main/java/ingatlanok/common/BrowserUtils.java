package ingatlanok.common;

import java.awt.Desktop;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class BrowserUtils {

  private static final Logger LOGGER = LoggerFactory.getLogger(BrowserUtils.class);

  private static final Set<String> ALLOWED_PORTAL_HOSTS =
      Set.of(
          "dh.hu",
          "www.dh.hu",
          "oc.hu",
          "www.oc.hu",
          "jofogas.hu",
          "www.jofogas.hu",
          "ingatlan.jofogas.hu",
          "koltozzbe.hu",
          "www.koltozzbe.hu",
          "ingatlanbazar.hu",
          "www.ingatlanbazar.hu",
          "ingatlan.com",
          "www.ingatlan.com",
          "localhost",
          "127.0.0.1");

  private BrowserUtils() {}

  public static boolean openUrl(String url) {
    if (url == null || url.isBlank()) {
      return false;
    }
    String trimmed = url.trim();
    if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) {
      LOGGER.warn(
          "Biztonsági figyelmeztetés: Csak http:// vagy https:// hivatkozás nyitható meg: {}",
          trimmed);
      return false;
    }

    URI uri;
    try {
      uri = URI.create(trimmed);
    } catch (IllegalArgumentException e) {
      LOGGER.warn("Érvénytelen URL: {}", trimmed);
      return false;
    }

    String scheme = uri.getScheme();
    if (scheme == null
        || (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme))) {
      LOGGER.warn("Nem engedélyezett URL séma: {}", trimmed);
      return false;
    }

    String host = uri.getHost();
    if (host == null || !ALLOWED_PORTAL_HOSTS.contains(host.toLowerCase(Locale.ROOT))) {
      LOGGER.warn("Nem engedélyezett domain a böngésző megnyitásához: {}", trimmed);
      return false;
    }

    try {
      if (Desktop.isDesktopSupported()
          && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
        Desktop.getDesktop().browse(uri);
        return true;
      }

      LOGGER.warn("A böngésző megnyitása nem támogatott ezen a környezeten: {}", trimmed);
      return false;
    } catch (Exception e) {
      LOGGER.error("Nem sikerült megnyitni a böngészőben: {} ({})", trimmed, e.getMessage(), e);
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
      LOGGER.info(
          "Megjegyzés: Az első {} új link került megnyitásra a böngészőben, a többi elérhető a"
              + " felületen.",
          maxToOpen);
    }

    return opened;
  }

  public static List<String> openUrls(Collection<String> urlList) {
    int max = Integer.getInteger("max.open.browser", 5);
    return openUrls(urlList, max);
  }
}
