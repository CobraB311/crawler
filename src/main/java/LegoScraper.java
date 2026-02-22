import org.jsoup.Connection;
import org.jsoup.Jsoup;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LegoScraper extends BaseScraper {

    private final DateTimeFormatter timeLog = DateTimeFormatter.ofPattern("HH:mm:ss");

    @Override
    public boolean supports(String url) {
        return url.toLowerCase().contains("lego.com");
    }

    @Override
    public String fetchPrice(String url) {
        try {
            Connection conn = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.0 Mobile/15E148 Safari/604.1")
                    .header("Accept-Language", "nl-be")
                    .timeout(20000)
                    .ignoreHttpErrors(true)
                    .followRedirects(true);

            Connection.Response response = conn.execute();

            if (response.statusCode() == 403) {
                return "403: Bot Detectie";
            }

            String html = response.body();

            if (html.contains("Tijdelijk niet op voorraad") || html.contains("Niet meer leverbaar")) {
                return "Niet leverbaar";
            }

            // Regex zoektocht naar prijs
            Pattern p = Pattern.compile("\"price\"\\s*:\\s*\"?(\\d+[\\.,]\\d+)\"?");
            Matcher m = p.matcher(html);

            if (m.find()) {
                String priceFound = m.group(1).replace(".", ",");
                if (!priceFound.contains(",")) priceFound += ",00";
                return "€ " + priceFound;
            }

            return "N/A";
        } catch (Exception e) {
            return "Scan fout";
        }
    }
}