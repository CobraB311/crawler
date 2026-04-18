import org.jsoup.Connection;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MediaMarktScraper extends BaseScraper {

    @Override
    public boolean supports(String url) {
        return url.toLowerCase().contains("mediamarkt.be");
    }

    @Override
    public String fetchPrice(String url) {
        try {
            // MediaMarkt vereist vaak specifieke headers om geen 403 te geven
            Connection.Response response = getSafeConnection(url)
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8")
                    .execute();
            
            Document doc = response.parse();
            String html = doc.html();

            // Methode 1: JSON-LD (Zoeken naar "price": 79.99)
            Pattern p = Pattern.compile("\"price\"\\s*:\\s*(\\d+[\\.,]?\\d*)");
            Matcher m = p.matcher(html);
            
            if (m.find()) {
                String priceFound = m.group(1).replace(".", ",");
                if (!priceFound.contains(",")) priceFound += ",00";
                if (priceFound.split(",")[1].length() == 1) priceFound += "0";
                return "€ " + priceFound;
            }

            // Methode 2: Fallback via meta tags
            Element metaPrice = doc.selectFirst("meta[property=product:price:amount], meta[itemprop=price]");
            if (metaPrice != null) {
                String pStr = metaPrice.attr("content").replace(".", ",");
                if (!pStr.contains(",")) pStr += ",00";
                return "€ " + pStr;
            }

            return "N/A";
        } catch (Exception e) {
            ScrapeExceptionHandler.log("MediaMarktScraper", url, e);
            return "Scan fout";
        }
    }
}