import org.jsoup.Connection;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.math.BigDecimal;
import java.math.RoundingMode;

public class SupraBazarScraper extends BaseScraper {

    @Override
    public boolean supports(String url) {
        return url.toLowerCase().contains("suprabazar.be");
    }

    @Override
    public String fetchPrice(String url) {
        try {
            Connection.Response response = getSafeConnection(url).execute();
            Document doc = response.parse();
            String html = doc.html();

            // We zoeken naar de prijs en de korting in de JSON string
            Pattern pricePattern = Pattern.compile("\"price\"\\s*:\\s*(\\d+[\\.,]?\\d*)");
            Pattern discountPattern = Pattern.compile("\"discount\"\\s*:\\s*(-?\\d+[\\.,]?\\d*)");

            Matcher mPrice = pricePattern.matcher(html);
            Matcher mDiscount = discountPattern.matcher(html);

            double basePrice = 0;
            double discount = 0;

            // We nemen de laatste match van de prijs (vaak degene in pdpDataLayer)
            while (mPrice.find()) {
                basePrice = Double.parseDouble(mPrice.group(1));
            }

            // We zoeken de korting (bijv. -8.99)
            if (mDiscount.find()) {
                discount = Double.parseDouble(mDiscount.group(1));
            }

            // Bereken de eindprijs (Korting is vaak al negatief in de JSON, dus we tellen het op)
            double finalPrice = basePrice + discount;

            if (finalPrice > 0) {
                // Gebruik BigDecimal voor nette afronding naar 2 decimalen
                BigDecimal bd = new BigDecimal(finalPrice).setScale(2, RoundingMode.HALF_UP);
                String formatted = bd.toString().replace(".", ",");
                if (!formatted.contains(",")) formatted += ",00";
                if (formatted.split(",")[1].length() == 1) formatted += "0";
                return "€ " + formatted;
            }

            // Fallback via meta tags als de JSON faalt
            Element metaPrice = doc.selectFirst("meta[itemprop=price]");
            if (metaPrice != null) {
                String pStr = metaPrice.attr("content").replace(".", ",");
                if (!pStr.contains(",")) pStr += ",00";
                return "€ " + pStr;
            }

            return "N/A";
        } catch (Exception e) {
            ScrapeExceptionHandler.log("SupraBazarScraper", url, e);
            return "Scan fout";
        }
    }
}