import org.jsoup.Connection;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CoolblueScraper extends BaseScraper {

    @Override
    public boolean supports(String url) {
        return url.toLowerCase().contains("coolblue.be") || url.toLowerCase().contains("coolblue.nl");
    }

    @Override
    public String fetchPrice(String url) {
        try {
            Connection.Response response = getSafeConnection(url)
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8")
                    .execute();
            
            Document doc = response.parse();
            String html = doc.html();

            // Methode 1: JSON-LD
            Pattern p = Pattern.compile("\"price\"\\s*:\\s*(\\d+[\\.,]?\\d*)");
            Matcher m = p.matcher(html);
            
            if (m.find()) {
                String priceFound = m.group(1).replace(".", ",");
                
                // Verbeterde decimalen-logica
                if (!priceFound.contains(",")) {
                    priceFound += ",00";
                } else {
                    String[] parts = priceFound.split(",");
                    if (parts.length == 1) { // Geval zoals "60,"
                        priceFound += "00";
                    } else if (parts[1].length() == 1) { // Geval zoals "60,5"
                        priceFound += "0";
                    }
                }
                return "€ " + priceFound;
            }

            // Methode 2: Fallback
            Element priceElem = doc.selectFirst(".sales-price__current, .sales-price__main");
            if (priceElem != null) {
                String rawPrice = priceElem.text().replaceAll("[^0-9,.]", "").replace(".", ",");
                if (!rawPrice.contains(",")) rawPrice += ",00";
                else if (rawPrice.endsWith(",")) rawPrice += "00";
                return "€ " + rawPrice;
            }

            return "N/A";
        } catch (Exception e) {
            ScrapeExceptionHandler.log("CoolblueScraper", url, e);
            return "Scan fout";
        }
    }
}