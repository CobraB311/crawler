import org.jsoup.Connection;
import org.jsoup.HttpStatusException;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SupraBazarScraper extends BaseScraper {

    // Lock zorgt ervoor dat meerdere threads nooit tegelijk SupraBazar aanvallen
    private static final ReentrantLock RATE_LIMIT_LOCK = new ReentrantLock(true);
    private static final Map<String, String> SESSION_COOKIES = new HashMap<>();
    private static long lastRequestTime = 0;
    private static long cooldownUntil = 0;
    private static boolean sessionInitialized = false;

    @Override
    public boolean supports(String url) {
        return url.toLowerCase().contains("suprabazar.be");
    }

    @Override
    public String fetchPrice(String url) {
        // Zet alle parallelle threads voor SupraBazar netjes achter elkaar in de wachtrij
        RATE_LIMIT_LOCK.lock();
        try {
            int maxRetries = 3;
            int attempt = 0;

            while (attempt < maxRetries) {
                attempt++;
                try {
                    waitForCooldownAndRateLimit();

                    // Warm de sessie eenmalig op via de homepage voor geldige cookies
                    if (!sessionInitialized) {
                        initSession();
                    }

                    Connection connection = getSafeConnection(url)
                            .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8")
                            .header("Accept-Language", "nl-BE,nl;q=0.9,en;q=0.8")
                            .header("Referer", "https://www.suprabazar.be/")
                            .cookies(SESSION_COOKIES)
                            .ignoreHttpErrors(false);

                    Connection.Response response = connection.execute();
                    lastRequestTime = System.currentTimeMillis();
                    SESSION_COOKIES.putAll(response.cookies());

                    Document doc = response.parse();
                    String html = doc.html();

                    // 1. Zoek prijs en korting in de JSON string
                    Pattern pricePattern = Pattern.compile("\"price\"\\s*:\\s*(\\d+(?:[\\.,]\\d+)?)");
                    Pattern discountPattern = Pattern.compile("\"discount\"\\s*:\\s*(-?\\d+(?:[\\.,]\\d+)?)");

                    Matcher mPrice = pricePattern.matcher(html);
                    Matcher mDiscount = discountPattern.matcher(html);

                    double basePrice = 0;
                    double discount = 0;

                    while (mPrice.find()) {
                        basePrice = Double.parseDouble(mPrice.group(1).replace(",", "."));
                    }

                    if (mDiscount.find()) {
                        discount = Double.parseDouble(mDiscount.group(1).replace(",", "."));
                    }

                    double finalPrice = basePrice + discount;

                    if (finalPrice > 0) {
                        BigDecimal bd = new BigDecimal(finalPrice).setScale(2, RoundingMode.HALF_UP);
                        return "€ " + bd.toString().replace(".", ",");
                    }

                    // 2. Fallback via meta tags
                    Element metaPrice = doc.selectFirst("meta[itemprop=price]");
                    if (metaPrice != null) {
                        String rawMeta = metaPrice.attr("content").replace(",", ".").replaceAll("[^0-9.]", "");
                        if (!rawMeta.isEmpty()) {
                            BigDecimal bd = new BigDecimal(rawMeta).setScale(2, RoundingMode.HALF_UP);
                            return "€ " + bd.toString().replace(".", ",");
                        }
                    }

                    return "N/A";

                } catch (HttpStatusException e) {
                    lastRequestTime = System.currentTimeMillis();
                    if (e.getStatusCode() == 429) {
                        // Reset cookies en stel een globale pauze in (60s, 120s, 180s)
                        SESSION_COOKIES.clear();
                        sessionInitialized = false;
                        long penaltyMillis = 60_000L * attempt;
                        cooldownUntil = System.currentTimeMillis() + penaltyMillis;

                        ScrapeExceptionHandler.logCustom("SupraBazarScraper", url,
                                "HTTP 429 ontvangen. Globale pauze van " + (penaltyMillis / 1000) + "s voor poging " + (attempt + 1) + "...");
                    } else {
                        ScrapeExceptionHandler.log("SupraBazarScraper", url, e);
                        return "Scan fout";
                    }
                } catch (Exception e) {
                    ScrapeExceptionHandler.log("SupraBazarScraper", url, e);
                    return "Scan fout";
                }
            }

            ScrapeExceptionHandler.logCustom("SupraBazarScraper", url, "Definitief geblokkeerd door HTTP 429 na " + maxRetries + " pogingen.");
            return "Rate limit (429)";

        } finally {
            RATE_LIMIT_LOCK.unlock();
        }
    }

    private void initSession() {
        try {
            Connection.Response homeRes = getSafeConnection("https://www.suprabazar.be/")
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                    .header("Accept-Language", "nl-BE,nl;q=0.9")
                    .execute();
            SESSION_COOKIES.putAll(homeRes.cookies());
            sessionInitialized = true;
            lastRequestTime = System.currentTimeMillis();
            Thread.sleep(ThreadLocalRandom.current().nextLong(2500, 4000));
        } catch (Exception ignored) {
            // Ga door zonder voorafgaande homepage-cookies als dit faalt
        }
    }

    private void waitForCooldownAndRateLimit() throws InterruptedException {
        long now = System.currentTimeMillis();

        // 1. Wacht eerst een eventuele globale 429-afkoelperiode uit
        if (cooldownUntil > now) {
            Thread.sleep(cooldownUntil - now);
            now = System.currentTimeMillis();
        }

        // 2. Zorg voor minimaal 5 tot 8 seconden tussen opeenvolgende SupraBazar-requests
        if (lastRequestTime > 0) {
            long minDelay = ThreadLocalRandom.current().nextLong(5000, 8000);
            long elapsed = now - lastRequestTime;
            if (elapsed < minDelay) {
                Thread.sleep(minDelay - elapsed);
            }
        }
    }
}