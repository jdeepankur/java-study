import org.jsoup.Jsoup;
import org.jsoup.select.*;
import org.jsoup.nodes.*;

import java.util.*;


class Q16 {
    public static List<String> extractLinks(String html) {
        Document doc = Jsoup.parse(html);
        Elements es = doc.getElementsByTag("a");

        List<String> result = Arrays.stream(es.toArray(new Element[]{})).map(e -> e.attr("href")).toList();

        return result;
    }

    public static void main(String[] args) {
        var in = new Scanner(System.in);
        String html = "";
        while (in.hasNextLine()) {
            html += in.nextLine() + "\n";
        }

        String[] links = extractLinks(html).toArray(new String[]{});
        for (String link:links) {
            System.out.println(link);
        } 

    }
}