import org.jsoup.Connection;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class AmazonScraper extends BaseScraper {

    private final DateTimeFormatter timeLog = DateTimeFormatter.ofPattern("HH:mm:ss");

    @Override
    public boolean supports(String url) {
        String urlLower = url.toLowerCase();
        return urlLower.contains("amazon.nl") || 
               urlLower.contains("amazon.com.be") || 
               urlLower.contains("amzn.eu");
    }

    @Override
    public String fetchPrice(String url) {
        try {
            Connection.Response response = getSafeConnection(url)
                    .header("Accept-Language", "nl-BE,nl;q=0.9,en-US;q=0.8")
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8")
                    .followRedirects(true) 
                    .execute();

            if (response.statusCode() == 503) {
                logError("Amazon 503: Captcha gedetecteerd.");
                return "Captcha";
            }

            Document doc = response.parse();
            String htmlContent = doc.text();

            // 1. Check op blokkades/geen aanbod
            if (htmlContent.contains("Geen aanbevolen aanbod beschikbaar") || 
                htmlContent.contains("Prijs hoger dan gebruikelijk")) {
                return "Geen aanbod";
            }

            if (htmlContent.contains("Momenteel niet verkrijgbaar") || 
                htmlContent.contains("Niet op voorraad")) {
                return "Niet leverbaar";
            }

            // 2. Methode A: De specifieke "PriceToPay" selector (vaak bij boeken en nieuwe lay-outs)
            // Zoals in jouw snippet: .priceToPay of .apex-pricetopay-value
            Element priceToPay = doc.selectFirst(".priceToPay, .apex-pricetopay-value");
            if (priceToPay != null) {
                Element whole = priceToPay.selectFirst(".a-price-whole");
                Element fraction = priceToPay.selectFirst(".a-price-fraction");
                
                if (whole != null) {
                    // Verwijder alles behalve cijfers (negeert de komma/punt binnen de whole span)
                    String wholeText = whole.ownText().replaceAll("[^0-9]", "");
                    if (wholeText.isEmpty()) {
                        // Als ownText leeg is, probeer de volledige tekst minus de decimal span
                        wholeText = whole.text().split(",")[0].replaceAll("[^0-9]", "");
                    }
                    
                    String fractText = (fraction != null) ? fraction.text().replaceAll("[^0-9]", "") : "00";
                    if (fractText.isEmpty()) fractText = "00";
                    
                    return "€ " + wholeText + "," + fractText;
                }
            }

            // 3. Methode B: De standaard Buy Box containers
            Element priceContainer = doc.selectFirst("#corePrice_feature_div, #corePriceDisplay_desktop_feature_div");
            if (priceContainer != null) {
                Element w = priceContainer.selectFirst(".a-price-whole");
                Element f = priceContainer.selectFirst(".a-price-fraction");
                if (w != null) {
                    String wt = w.text().replaceAll("[^0-9]", "");
                    String ft = (f != null) ? f.text().replaceAll("[^0-9]", "") : "00";
                    return "€ " + wt + "," + ft;
                }
            }

            // 4. Methode C: Fallback voor de "a-offscreen" prijs (zoals zichtbaar in jouw snippet bovenaan)
            Element offscreen = doc.selectFirst(".aok-offscreen");
            if (offscreen != null && offscreen.text().contains("€")) {
                String clean = offscreen.text().replaceAll("[^0-9,.]", "").replace(".", ",");
                if (!clean.contains(",")) clean += ",00";
                return "€ " + clean.replace(",,", ",");
            }

            logError("Geen prijs gevonden op Amazon: " + response.url());
            return "N/A";

        } catch (Exception e) {
            logError("Fout tijdens Amazon scan: " + e.getMessage());
            return "Scan fout";
        }
    }

    private void logError(String message) {
        System.err.println("[" + LocalTime.now().format(timeLog) + "] [AmazonScraper] " + message);
    }
}