import org.jsoup.Connection;
import org.jsoup.Jsoup;

public abstract class BaseScraper implements PriceScraper {
    protected Connection getSafeConnection(String url) {
        return Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .header("Accept-Language", "nl-BE,nl;q=0.9,en-US;q=0.8,en;q=0.7")
                .timeout(15000);
    }
}