package ingatlanok.helper;

import ingatlanok.persistence.IngatlanDataBaseMock;
import org.apache.commons.io.FileUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * Segédosztály a korábban látott URL-ek perzisztenciájához és a böngészőkezeléshez.
 */
public class IngatlanHelper {

    private static final String DEFAULT_FILE = "ingatlanok_history.txt";
    private static final String LEGACY_FILE = "2017_06_06.txt";
    private static String fileName = System.getProperty("ingatlan.file", DEFAULT_FILE);

    public static void startContent() {
        // Ha létezik a régi naplófájl, töltsük be az előzményeket
        File legacy = new File(LEGACY_FILE);
        if (legacy.exists()) {
            loadFileToMemory(legacy);
        }

        File current = new File(fileName);
        if (!current.exists()) {
            try {
                current.createNewFile();
            } catch (IOException e) {
                System.err.println("Nem sikerült létrehozni a történet fájlt: " + e.getMessage());
            }
        } else {
            loadFileToMemory(current);
        }

        System.out.println("Adatbázis inicializálva: " + IngatlanDataBaseMock.getUrls().size() + " korábbi link a memóriában.");
    }

    private static void loadFileToMemory(File file) {
        try (BufferedReader br = new BufferedReader(new FileReader(file, StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.startsWith("http://") || line.startsWith("https://")) {
                    IngatlanDataBaseMock.addUrl(line);
                }
            }
        } catch (IOException e) {
            System.err.println("Hiba a fájl olvasásakor [" + file.getName() + "]: " + e.getMessage());
        }
    }

    public static void saveIngatlanToFile(List<String> lines) {
        try {
            FileUtils.writeLines(new File(fileName), StandardCharsets.UTF_8.name(), lines, true);
            System.out.println("Eredmények elmentve a fájlba: " + fileName);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static List<String> openUrl(Set<String> urlList) {
        List<String> result = new LinkedList<>();
        int opened = 0;
        int maxToOpen = Integer.getInteger("max.open.browser", 5);

        for (String url : urlList) {
            System.out.println("Új találat: " + url);
            result.add(url);

            // Csak az első pár linket nyitjuk meg automatikusan a böngészőben a gép túlterhelésének elkerülésére
            if (opened < maxToOpen) {
                try {
                    if (java.awt.Desktop.isDesktopSupported() && java.awt.Desktop.getDesktop().isSupported(java.awt.Desktop.Action.BROWSE)) {
                        java.awt.Desktop.getDesktop().browse(new java.net.URI(url));
                        opened++;
                    } else {
                        new ProcessBuilder("rundll32", "url.dll,FileProtocolHandler", url).start();
                        opened++;
                    }
                } catch (Exception e) {
                    System.err.println("Nem sikerült megnyitni a böngészőben: " + url);
                }
            }
        }

        if (urlList.size() > maxToOpen) {
            System.out.println("Megjegyzés: Az első " + maxToOpen + " új link került megnyitásra a böngészőben, a többi rögzítve a naplófájlban.");
        }

        return result;
    }
}
