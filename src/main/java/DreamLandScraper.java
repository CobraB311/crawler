import org.jsoup.Connection;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DreamLandScraper extends BaseScraper {

    private final DateTimeFormatter timeLog = DateTimeFormatter.ofPattern("HH:mm:ss");

    @Override
    public boolean supports(String url) {
        return url.toLowerCase().contains("dreamland.be");
    }

    @Override
    public String fetchPrice(String url) {
        try {
            Connection.Response response = getSafeConnection(url)
                    .header("Accept-Language", "nl-BE,nl;q=0.9")
                    .execute();

            Document doc = response.parse();
            
            // Methode 1: Specifieke CSS selectors voor de actuele prijs
            // We zoeken eerst naar de gedisconteerde prijs, daarna naar de gewone prijs
            Element actualPriceElem = doc.selectFirst(".product-pricing__price.-discounted, [data-product-price]");
            
            if (actualPriceElem != null) {
                // We halen de hoofdcijfers en decimalen apart op binnen dit element
                Element main = actualPriceElem.selectFirst(".product-pricing__price, .product-pricing__price-from"); 
                // In jouw snippet zitten de decimalen in een span met class 'decimals'
                String whole = actualPriceElem.text().split(",")[0].replaceAll("[^0-9]", "");
                Element decimalsElem = actualPriceElem.selectFirst(".decimals");
                String decimals = (decimalsElem != null) ? decimalsElem.text().replaceAll("[^0-9]", "") : "00";
                
                if (!whole.isEmpty()) {
                    return "€ " + whole + "," + decimals;
                }
            }

            // Methode 2: Regex Fallback maar met awareness van 'discounted'
            String html = doc.html();
            
            // We zoeken eerst specifiek in het blok dat de actuele prijs bevat
            Pattern discountedPattern = Pattern.compile("product-pricing__price -discounted.*?currency-symbol\">€</span>.*?(\\d+),.*?decimals\">(\\d+)</span>", Pattern.DOTALL);
            Matcher dm = discountedPattern.matcher(html);
            if (dm.find()) {
                return "€ " + dm.group(1) + "," + dm.group(2);
            }

            // Methode 3: Algemene JSON-LD prijs (vaak de actuele prijs voor Google Shopping)
            Pattern jsonLdPattern = Pattern.compile("\"price\"\\s*:\\s*\"?(\\d+[\\.,](\\d+))\"?");
            Matcher jm = jsonLdPattern.matcher(html);
            if (jm.find()) {
                String priceFound = jm.group(1).replace(".", ",");
                if (!priceFound.contains(",")) priceFound += ",00";
                return "€ " + priceFound;
            }

            // Check op voorraad
            if (html.contains("Niet op voorraad") || html.contains("Uitverkocht")) {
                return "Niet leverbaar";
            }

            logError("Geen prijs gevonden op DreamLand: " + url);
            return "N/A";

        } catch (Exception e) {
            logError("Fout tijdens DreamLand scan: " + e.getMessage());
            return "Scan fout";
        }
    }

    private void logError(String message) {
        System.err.println("[" + LocalTime.now().format(timeLog) + "] [DreamLandScraper] " + message);
    }
}