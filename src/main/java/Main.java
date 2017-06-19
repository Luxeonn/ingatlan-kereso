import ingatlanok.helper.IngatlanHelper;
import ingatlanok.service.impl.DunaHause;
import ingatlanok.service.impl.IngatlanBazar;
import ingatlanok.service.impl.IngatlanCom;
import ingatlanok.service.impl.OtthonCentrum;

import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by Pusinszky on 2017. 06. O5..
 */
public class Main {

    public static void main(String[] args) {
        List<String> collection = new LinkedList<String>();
        Set<String> urlList = new HashSet<String>();
        IngatlanHelper.startContent();

        urlList.addAll(new IngatlanCom().getUrls());
        urlList.addAll(new DunaHause().getUrls());
        urlList.addAll(new IngatlanBazar().getUrls());
        urlList.addAll(new OtthonCentrum().getUrls());

        collection.add("======================================");
        collection.add("Elindítva : " + new SimpleDateFormat("yyyy-MMMMM-dd HH:mm:ss").format(new Date()));
        collection.add("Új URL Száma : " + urlList.size());
        if (!urlList.isEmpty()) {
            collection.addAll(IngatlanHelper.openUrl(urlList));
        }
        IngatlanHelper.saveIngatlanToFile(collection);
    }

}
