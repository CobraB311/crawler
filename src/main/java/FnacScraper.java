import org.jsoup.Connection;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FnacScraper extends BaseScraper {

    @Override
    public boolean supports(String url) {
        return url.toLowerCase().contains("fnac.be");
    }

    @Override
    public String fetchPrice(String url) {
        try {
            // We configureren de connectie handmatig om redirects beter te beheren
            Connection conn = getSafeConnection(url)
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8")
                    .header("Accept-Language", "nl-BE,nl;q=0.9")
                    .header("Sec-Fetch-Dest", "document")
                    .header("Sec-Fetch-Mode", "navigate")
                    .header("Sec-Fetch-Site", "none")
                    .header("Upgrade-Insecure-Requests", "1")
                    .followRedirects(true) 
                    .maxBodySize(0); // Voorkom afgekapte pagina's

            Connection.Response response = conn.execute();
            String finalUrl = response.url().toString();

            // Check of we in de wachtrij terecht zijn gekomen
            if (finalUrl.contains("queue.fnacdarty.com")) {
                ScrapeExceptionHandler.logCustom("FnacScraper", url, "Wachtrij (Queue-it) actief. Scraper kan niet verder.");
                return "Wachtrij actief";
            }

            Document doc = response.parse();
            String html = doc.html();

            // Methode 1: JSON-LD (Meest betrouwbaar)
            Pattern p = Pattern.compile("\"price\"\\s*:\\s*(\\d+[\\.,]?\\d*)");
            Matcher m = p.matcher(html);
            if (m.find()) {
                String priceFound = m.group(1).replace(".", ",");
                if (!priceFound.contains(",")) priceFound += ",00";
                if (priceFound.split(",")[1].length() == 1) priceFound += "0";
                return "€ " + priceFound;
            }

            // Methode 2: Visuele prijs fallback
            Element priceElem = doc.selectFirst(".f-faPriceBox__price, .userPrice, [data-automation-id=product-price-label]");
            if (priceElem != null) {
                String rawPrice = priceElem.text().replaceAll("[^0-9,.]", "").replace(".", ",");
                if (!rawPrice.contains(",")) rawPrice += ",00";
                return "€ " + rawPrice;
            }

            ScrapeExceptionHandler.logCustom("FnacScraper", url, "Pagina geladen maar geen prijs gevonden (is het product nog leverbaar?)");
            return "N/A";

        } catch (Exception e) {
            // Als we specifiek de redirect error krijgen, loggen we dat duidelijk
            if (e.getMessage().contains("Too many redirects")) {
                ScrapeExceptionHandler.logCustom("FnacScraper", url, "Redirect Loop: Waarschijnlijk geblokkeerd door wachtrij of Bot-beveiliging.");
            } else {
                ScrapeExceptionHandler.log("FnacScraper", url, e);
            }
            return "Scan fout";
        }
    }
}