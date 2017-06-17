import org.apache.commons.io.FileUtils;

import java.io.*;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * Created by Pusinszky on 2017. 06. 17..
 */
public class IngatlanHelper {

    private static String fileName = "2017_06_06.txt";

    public static void startContent(){
        BufferedReader br = openFile();
        try {
            String line = br.readLine();
            System.out.println("Adatbázis feltöltése");
            while (line != null) {
                IngatlanDataBaseMock.addUrl(line);
                line = br.readLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                br.close();
            } catch (IOException e) {
            }
        }
    }

    public static void saveIngatlanToFile(List<String> urlList ){
        try {
            FileUtils.writeLines(new File(fileName),urlList,true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static List<String> openUrl(Set<String> urlList){
        List<String> result = new LinkedList<String>();
        Runtime rt = Runtime.getRuntime();
        for(String url : urlList){
            System.out.println("URL : " + url);
            try {
                rt.exec("rundll32 url.dll,FileProtocolHandler " + url);
            } catch (IOException e) {
                e.printStackTrace();
            }
            result.add(url);
        }
        return result;
    }


    private static BufferedReader openFile(){
        try {
            return new BufferedReader(new FileReader(fileName));
        } catch (FileNotFoundException e) {
            createNewFile();
        }
        return openFile();
    }

    private static void createNewFile(){
        FileOutputStream oFile = null;
        try {
            oFile = new FileOutputStream(fileName, false);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } finally {
            try {
                oFile.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
