import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

public class BolScraper extends BaseScraper {
    @Override
    public boolean supports(String url) {
        return url.toLowerCase().contains("bol.com");
    }

    @Override
    public String fetchPrice(String url) {
        try {
            Document doc = getSafeConnection(url).get();
            Element buyBlock = doc.getElementById("buyBlockSlot");
            String fullPageText = doc.text();
            
            if (fullPageText.contains("Niet leverbaar") || (buyBlock != null && buyBlock.text().contains("Niet leverbaar"))) {
                return "Niet leverbaar";
            }

            Element visualPrice = (buyBlock != null) ? buyBlock.selectFirst(".text-promo-text-high") : doc.selectFirst(".text-promo-text-high");
            
            if (visualPrice != null) {
                String rawText = visualPrice.text().trim();
                String cleanPrice = rawText.replaceAll("[^0-9,\\-]", "");
                if (!cleanPrice.isEmpty()) {
                    if (cleanPrice.contains("-")) cleanPrice = cleanPrice.replace("-", "").replace(",", "") + ",00";
                    if (!cleanPrice.contains(",")) cleanPrice += ",00";
                    return "€ " + cleanPrice.replace(",,", ",");
                }
            }
            return "N/A";
        } catch (Exception e) { return "Scan fout"; }
    }
}