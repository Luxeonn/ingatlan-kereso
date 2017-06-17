import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * Created by Pusinszky on 2017. 06. O5..
 */
public class Main {

    private static IngatlanCom ic = new IngatlanCom();

    private static DunaHause dh = new DunaHause();

    public static void main(String[] args){
        List<String> collection = new LinkedList<String>();
        IngatlanHelper.startContent();
        Set<String> urlList = ic.getUrls();
        urlList.addAll(dh.getUrls());
        collection.add("======================================");
        collection.add("Elindítva : " + new SimpleDateFormat("yyyy-MMMMM-dd HH:mm:ss").format(new Date()));
        collection.add("Új URL Száma : " + urlList.size());
        if (!urlList.isEmpty()) {
            collection.addAll(IngatlanHelper.openUrl(urlList));
        }
        IngatlanHelper.saveIngatlanToFile(collection);
    }

}
