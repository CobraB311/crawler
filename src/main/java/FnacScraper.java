import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FnacScraper extends BaseScraper {

    @Override
    public boolean supports(String url) {
        return url.toLowerCase().contains("fnac.be");
    }

    @Override
    public String fetchPrice(String url) {
        ChromeOptions options = new ChromeOptions();
        //options.addArguments("--headless=new");
        options.addArguments("--disable-blink-features=AutomationControlled");
        options.addArguments("user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/156.0.0.0 Safari/537.36");

        WebDriver driver = new ChromeDriver(options);

        try {
            driver.get(url);

            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));

            // 1. Probeer de OneTrust cookie-banner automatisch weg te klikken
            try {
                WebElement acceptBtn = wait.until(ExpectedConditions.elementToBeClickable(By.id("onetrust-accept-btn-handler")));
                acceptBtn.click();
            } catch (Exception ignored) {
                // Cookie banner was er niet of al geaccepteerd
            }

            // 2. Wacht tot de buy-box container geladen is (werkt zowel voor eigen voorraad als Marketplace partners)
            wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(".js-fnacBuyBox, [data-automation-id='product-offer-fnac'], .f-productOffers-mp")));

            String pageSource = driver.getPageSource();

            // Methode 1: Direct via het 'data-price' attribuut van de geselecteerde/standaard input
            try {
                WebElement priceInput = driver.findElement(By.cssSelector("input[name='fnacoffer']"));
                String dataPrice = priceInput.getAttribute("data-price");
                if (dataPrice != null && !dataPrice.isEmpty()) {
                    return "€ " + dataPrice.replace(".", ",");
                }
            } catch (Exception ignored) {}

            // Methode 2: Via de visuele .userPrice klasse binnen de buy-box
            List<WebElement> userPriceElements = driver.findElements(By.cssSelector(".js-fnacBuyBox .userPrice, .f-productOffers-mp .userPrice"));
            for (WebElement priceElem : userPriceElements) {
                String priceText = priceElem.getText().trim();
                if (!priceText.isEmpty()) {
                    String rawPrice = priceText.replaceAll("[^0-9,.]", "").replace(".", ",");
                    if (!rawPrice.contains(",")) rawPrice += ",00";
                    return "€ " + rawPrice;
                }
            }

            // Methode 3: JSON-LD fallback
            Pattern p = Pattern.compile("\"price\"\\s*:\\s*\"?(\\d+[\\.,]?\\d*)\"?");
            Matcher m = p.matcher(pageSource);
            if (m.find()) {
                String priceFound = m.group(1).replace(".", ",");
                if (!priceFound.contains(",")) priceFound += ",00";
                if (priceFound.split(",")[1].length() == 1) priceFound += "0";
                return "€ " + priceFound;
            }

            ScrapeExceptionHandler.logCustom("FnacScraper", url, "Pagina geladen maar geen prijs gevonden (is het product nog leverbaar?)");
            return "N/A";

        } catch (Exception e) {
            ScrapeExceptionHandler.log("FnacScraper", url, e);
            return "Scan fout";
        } finally {
            driver.quit();
        }
    }
}