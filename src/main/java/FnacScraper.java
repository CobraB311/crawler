import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

        // Sla cookies en DataDome-validatie op in een vaste profielmap
        options.addArguments("user-data-dir=C:/temp/fnac-chrome-profile");

        // Laat deze regel EERST uitgeschakeld om de puzzel eenmalig op te lossen,
        // of laat hem uit als Fnac headless blijft blokkeren:
        // options.addArguments("--headless=new");

        options.addArguments("--disable-blink-features=AutomationControlled");
        options.setExperimentalOption("excludeSwitches", Collections.singletonList("enable-automation"));
        options.setExperimentalOption("useAutomationExtension", false);
        options.addArguments("--window-size=1920,1080");
        // GEEN handmatige user-agent instellen zodat deze 100% matcht met Chrome 156

        ChromeDriver driver = new ChromeDriver(options);

        Map<String, Object> params = new HashMap<>();
        params.put("source", "Object.defineProperty(navigator, 'webdriver', {get: () => undefined})");
        driver.executeCdpCommand("Page.addScriptToEvaluateOnNewDocument", params);

        try {
            driver.get(url);

            // Geef extra tijd om eventueel handmatig de puzzel op te lossen tijdens de eerste run
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));

            // 1. OneTrust cookie-banner sluiten indien aanwezig
            try {
                WebElement acceptBtn = new WebDriverWait(driver, Duration.ofSeconds(3))
                        .until(ExpectedConditions.elementToBeClickable(By.id("onetrust-accept-btn-handler")));
                acceptBtn.click();
            } catch (Exception ignored) {
            }

            // 2. Wacht tot de buy-box container geladen is
            wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(".js-fnacBuyBox, [data-automation-id='product-offer-fnac'], .f-productOffers-mp")));

            // Methode 1: Direct via het 'data-price' attribuut
            try {
                WebElement priceInput = driver.findElement(By.cssSelector("input[name='fnacoffer']"));
                String dataPrice = priceInput.getAttribute("data-price");
                if (dataPrice != null && !dataPrice.isEmpty()) {
                    return "€ " + dataPrice.replace(".", ",");
                }
            } catch (Exception ignored) {
            }

            // Methode 2: Via de visuele .userPrice klasse
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
            Matcher m = p.matcher(driver.getPageSource());
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