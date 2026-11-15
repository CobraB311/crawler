import org.jsoup.Connection;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import java.math.BigDecimal;
import java.math.RoundingMode;
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
            Connection.Response response = getSafeConnection(url)
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8")
                    .header("Accept-Language", "nl-BE,nl;q=0.9")
                    .execute();

            Document doc = response.parse();

            // Methode 1: Specifieke MediaMarkt price-container (negeert doorgestreepte adviesprijs)
            Element brandedPriceContainer = doc.selectFirst("[data-test*='mms-branded-price']");
            if (brandedPriceContainer != null) {
                Element wholeElem = brandedPriceContainer.selectFirst("[data-test='branded-price-whole-value']");
                Element decimalElem = brandedPriceContainer.selectFirst("[data-test='branded-price-decimal-value']");

                if (wholeElem != null) {
                    String whole = wholeElem.text().replaceAll("[^0-9]", "");
                    String decimals = (decimalElem != null) ? decimalElem.text().replaceAll("[^0-9]", "") : "00";
                    if (decimals.isEmpty()) {
                        decimals = "00"; // Vangt "–" of "-" op bij ronde bedragen zoals € 44,-
                    } else if (decimals.length() == 1) {
                        decimals += "0";
                    }
                    if (!whole.isEmpty()) {
                        return "€ " + whole + "," + decimals;
                    }
                }
            }

            // Methode 2: JSON-LD fallback met veilige nummer-parsing
            String html = doc.html();
            Pattern p = Pattern.compile("\"price\"\\s*:\\s*\"?(\\d+(?:[\\.,]\\d+)?)\"?");
            Matcher m = p.matcher(html);

            if (m.find()) {
                return formatPrice(m.group(1));
            }

            // Methode 3: Meta tags fallback
            Element metaPrice = doc.selectFirst("meta[property=product:price:amount], meta[itemprop=price]");
            if (metaPrice != null) {
                return formatPrice(metaPrice.attr("content"));
            }

            return "N/A";
        } catch (Exception e) {
            ScrapeExceptionHandler.log("MediaMarktScraper", url, e);
            return "Scan fout";
        }
    }

    private String formatPrice(String rawPrice) {
        String normalized = rawPrice.replace(",", ".").replaceAll("[^0-9.]", "");
        if (normalized.endsWith(".")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        if (normalized.isEmpty()) {
            return "N/A";
        }
        BigDecimal bd = new BigDecimal(normalized).setScale(2, RoundingMode.HALF_UP);
        return "€ " + bd.toString().replace(".", ",");
    }
}