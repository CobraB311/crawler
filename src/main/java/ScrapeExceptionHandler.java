import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class ScrapeExceptionHandler {
    private static final DateTimeFormatter timeLog = DateTimeFormatter.ofPattern("HH:mm:ss");

    public static void log(String scraperName, String url, Exception e) {
        String timestamp = LocalTime.now().format(timeLog);
        System.err.println("\n" + "=".repeat(50));
        System.err.println("[" + timestamp + "] FOUT BIJ: " + scraperName);
        System.err.println("LINK: " + url);
        System.err.println("FOUTMELDING: " + e.getMessage());
        
        if (e instanceof org.jsoup.HttpStatusException hse) {
            System.err.println("HTTP STATUS: " + hse.getStatusCode());
        }
        
        System.err.println("=".repeat(50) + "\n");
    }

    public static void logCustom(String scraperName, String url, String message) {
        String timestamp = LocalTime.now().format(timeLog);
        System.err.println("[" + timestamp + "] [" + scraperName + "] ATTENTIE: " + message + " -> " + url);
    }
}