import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;
import java.io.*;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

public class WishlistController {
    private final Map<String, JSONArray> loadedFiles = new LinkedHashMap<>();
    private final Map<String, String> urlToLocalPathMap = new HashMap<>();
    private final List<JSONObject> allItemsList = new ArrayList<>();
    private final List<PriceScraper> scrapers = new ArrayList<>();
    private final ExecutorService executor = Executors.newFixedThreadPool(5);
    
    private volatile boolean shouldStop = false;
    private volatile boolean isPaused = false;
    private final Object pauseLock = new Object();

    public WishlistController() {
        scrapers.add(new BolScraper());
        scrapers.add(new LegoScraper());
        scrapers.add(new AmazonScraper());
        scrapers.add(new DreamLandScraper());
    }

    public void loadData(List<String> jsonUrls, Map<String, String> localPaths, Runnable onComplete) {
        new Thread(() -> {
            try {
                urlToLocalPathMap.clear();
                urlToLocalPathMap.putAll(localPaths);
                allItemsList.clear();
                loadedFiles.clear();
                for (String url : jsonUrls) {
                    String trimmed = url.trim();
                    if (trimmed.isEmpty()) continue;
                    String content = readContent(trimmed);
                    JSONArray items = new JSONArray(new JSONTokener(content));
                    loadedFiles.put(trimmed, items);
                    for (int i = 0; i < items.length(); i++) {
                        allItemsList.add(items.getJSONObject(i));
                    }
                }
                onComplete.run();
            } catch (Exception e) { e.printStackTrace(); }
        }).start();
    }

    private String readContent(String urlStr) throws Exception {
        String local = urlToLocalPathMap.get(urlStr);
        if (local != null && new File(local).exists()) {
            return new String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(local)), StandardCharsets.UTF_8);
        }
        // Java 23 fix: Use URI.create().toURL()
        try (InputStream in = URI.create(urlStr).toURL().openStream(); 
             BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            StringBuilder b = new StringBuilder(); String l;
            while ((l = r.readLine()) != null) b.append(l);
            return b.toString();
        }
    }

    public void scanSingleItemParallel(int index, boolean bol, boolean lego, boolean amzn, boolean dream, Consumer<Integer> onFinish) {
        JSONObject item = allItemsList.get(index);
        JSONArray winkels = item.getJSONArray("winkels");
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        for (int j = 0; j < winkels.length(); j++) {
            JSONObject w = winkels.getJSONObject(j);
            String link = w.getString("link");
            if (shouldSkip(link, bol, lego, amzn, dream)) {
                w.put("live_prijs", "Gedeactiveerd");
                continue;
            }
            PriceScraper scraper = scrapers.stream().filter(s -> s.supports(link)).findFirst().orElse(null);
            if (scraper != null) {
                futures.add(CompletableFuture.runAsync(() -> w.put("live_prijs", scraper.fetchPrice(link)), executor));
            }
        }
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenRun(() -> onFinish.accept(index));
    }

    private boolean shouldSkip(String link, boolean bol, boolean lego, boolean amzn, boolean dream) {
        String l = link.toLowerCase();
        if (l.contains("bol.com") && !bol) return true;
        if (l.contains("lego.com") && !lego) return true;
        if ((l.contains("amazon.") || l.contains("amzn.eu")) && !amzn) return true;
        if (l.contains("dreamland.be") && !dream) return true;
        return false;
    }

    public void applyLivePrices(JSONArray array) {
        for (int i = 0; i < array.length(); i++) {
            JSONArray winkels = array.getJSONObject(i).getJSONArray("winkels");
            for (int j = 0; j < winkels.length(); j++) {
                JSONObject w = winkels.getJSONObject(j);
                if (w.has("live_prijs") && w.getString("live_prijs").startsWith("€")) {
                    w.put("prijs", w.getString("live_prijs"));
                }
            }
        }
    }

    public String formatJson(JSONArray array) {
        StringBuilder sb = new StringBuilder("[\n");
        for (int i = 0; i < array.length(); i++) {
            JSONObject obj = array.getJSONObject(i);
            sb.append("  {\n");
            String[] keys = {"id", "naam", "nummer", "beschrijving", "afbeelding_url", "winkels"};
            for (int k = 0; k < keys.length; k++) {
                String key = keys[k];
                if (!obj.has(key)) continue;
                sb.append("    \"").append(key).append("\": ");
                if (key.equals("winkels")) {
                    sb.append("[\n");
                    JSONArray ws = obj.getJSONArray("winkels");
                    for (int j = 0; j < ws.length(); j++) {
                        JSONObject w = ws.getJSONObject(j);
                        sb.append("      { \"naam\": \"").append(w.getString("naam")).append("\", ")
                          .append("\"prijs\": \"").append(w.getString("prijs")).append("\", ")
                          .append("\"link\": \"").append(w.getString("link")).append("\" }")
                          .append(j < ws.length() - 1 ? ",\n" : "\n");
                    }
                    sb.append("    ]\n");
                } else {
                    sb.append("\"").append(obj.get(key).toString().replace("\"", "\\\"")).append("\"")
                      .append(k < keys.length - 1 ? ",\n" : "\n");
                }
            }
            int lastComma = sb.lastIndexOf(",");
            if (lastComma > sb.length() - 5) sb.deleteCharAt(lastComma);
            sb.append("  }").append(i < array.length() - 1 ? ",\n" : "\n");
        }
        sb.append("]");
        return sb.toString().replace("\\/", "/");
    }

    public void save(String url, String content) throws Exception {
        String path = urlToLocalPathMap.get(url);
        if (path != null) {
            try (BufferedWriter w = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(path), StandardCharsets.UTF_8))) {
                w.write(content);
            }
        }
    }

    public List<JSONObject> getAllItems() { return allItemsList; }
    public Map<String, JSONArray> getLoadedFiles() { return loadedFiles; }
    public void setStop(boolean s) { this.shouldStop = s; }
    public boolean isShouldStop() { return shouldStop; }
    public void setPaused(boolean p) { this.isPaused = p; synchronized(pauseLock) { pauseLock.notifyAll(); } }
    public boolean isPaused() { return isPaused; }
    public Object getPauseLock() { return pauseLock; }
}