import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class DreamLandScraper extends BaseScraper {

    private final DateTimeFormatter timeLog = DateTimeFormatter.ofPattern("HH:mm:ss");

    @Override
    public boolean supports(String url) {
        return url.toLowerCase().contains("dreamland.be");
    }

    @Override
    public String fetchPrice(String url) {
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless=new");
        options.addArguments("--disable-blink-features=AutomationControlled");
        options.addArguments("user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36");

        WebDriver driver = new ChromeDriver(options);

        try {
            driver.get(url);

            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

            // 1. Cookie banner automatisch sluiten indien aanwezig
            try {
                WebElement acceptBtn = new WebDriverWait(driver, Duration.ofSeconds(4))
                        .until(ExpectedConditions.elementToBeClickable(By.cssSelector(".cookie-consent__buttons button[value='yes']")));
                acceptBtn.click();
            } catch (Exception ignored) {
                // Cookie banner was al afgehandeld of niet aanwezig
            }

            // 2. Wachten tot de prijs-container geladen is
            wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("[data-product-price-block]")));

            // 3. Eerst zoeken naar gedisconteerde prijs, anders reguliere prijs
            List<WebElement> priceElements = driver.findElements(By.cssSelector(".product-pricing__price.-discounted, [data-product-price]"));

            for (WebElement priceElem : priceElements) {
                String priceText = priceElem.getText().trim();

                if (!priceText.isEmpty()) {
                    try {
                        WebElement decimalsElem = priceElem.findElement(By.cssSelector(".decimals"));
                        String decimals = decimalsElem.getText().replaceAll("[^0-9]", "");
                        String whole = priceText.split(",")[0].replaceAll("[^0-9]", "");

                        if (!whole.isEmpty()) {
                            return "€ " + whole + "," + (decimals.isEmpty() ? "00" : decimals);
                        }
                    } catch (Exception e) {
                        // Fallback als .decimals niet direct los te selecteren is
                        String cleanedPrice = priceText.replaceAll("[^0-9,]", "").replace(".", ",");
                        if (!cleanedPrice.isEmpty()) {
                            return "€ " + cleanedPrice;
                        }
                    }
                }
            }

            // 4. Controleer op voorraad indien geen prijs kan worden uitgelezen
            String pageSource = driver.getPageSource();
            if (pageSource.contains("Niet op voorraad") || pageSource.contains("Uitverkocht")) {
                return "Niet leverbaar";
            }

            logError("Geen prijs gevonden op DreamLand: " + url);
            return "N/A";

        } catch (Exception e) {
            logError("Fout tijdens DreamLand Selenium scan: " + e.getMessage());
            return "Scan fout";
        } finally {
            // Zorg dat de Chromium instance altijd netjes afgesloten wordt
            driver.quit();
        }
    }

    private void logError(String message) {
        System.err.println("[" + LocalTime.now().format(timeLog) + "] [DreamLandScraper] " + message);
    }
}