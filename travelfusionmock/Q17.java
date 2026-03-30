import org.jsoup.Jsoup;
// import com.google.gson.GsonBuilder;
import org.jsoup.nodes.*;

import java.util.*;


class Q16 {
    // public static void print(Object o) {
    //     System.out.println(new GsonBuilder().setPrettyPrinting().create().toJson(o));
    // }

    public static String cheapestDestination(String html) {
        Document doc = Jsoup.parse(html);
        Element table = doc.getElementsByTag("table").first();

        String result = "";
        int best = Integer.MAX_VALUE;
        
        for (var row:table.getElementsByTag("tr")) {
            var data = row.getElementsByTag("td");
            if (data.hasText()) {
                var dest = data.first().text();
                var price = Integer.parseInt(data.last().text());

                if (price < best) {
                    result = dest;
                    best = price;
                }
            }
        }

        return result;
    }

    public static void main(String[] args) {
        var in = new Scanner(System.in);
        String html = "";
        while (in.hasNextLine()) {
            html += in.nextLine() + "\n";
        }

        String bestDest = cheapestDestination(html);
        System.out.println(bestDest);

    }
}