import org.jsoup.Connection;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class KinderplaneetScraper extends BaseScraper {

    @Override
    public boolean supports(String url) {
        return url.toLowerCase().contains("dekinderplaneet.be");
    }

    @Override
    public String fetchPrice(String url) {
        try {
            Connection.Response response = getSafeConnection(url)
                    .header("Accept-Language", "nl-BE,nl;q=0.9")
                    .execute();
            
            Document doc = response.parse();
            String html = doc.html();

            // Methode 1: JSON-LD (Zoeken naar "price": "10.95")
            Pattern p = Pattern.compile("\"price\"\\s*:\\s*\"(\\d+[\\.,]?\\d*)\"");
            Matcher m = p.matcher(html);
            
            if (m.find()) {
                String priceFound = m.group(1).replace(".", ",");
                
                // Decimalen fixen
                if (!priceFound.contains(",")) {
                    priceFound += ",00";
                } else {
                    String[] parts = priceFound.split(",");
                    if (parts.length == 1) priceFound += "00";
                    else if (parts[1].length() == 1) priceFound += "0";
                }
                return "€ " + priceFound;
            }

            // Methode 2: Fallback via meta tags
            Element metaPrice = doc.selectFirst("meta[property=og:price:amount], meta[itemprop=price]");
            if (metaPrice != null) {
                String pStr = metaPrice.attr("content").replace(".", ",");
                if (!pStr.contains(",")) pStr += ",00";
                return "€ " + pStr;
            }

            return "N/A";
        } catch (Exception e) {
            ScrapeExceptionHandler.log("KinderplaneetScraper", url, e);
            return "Scan fout";
        }
    }
}