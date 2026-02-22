package org.CoreBytes.opdash.client;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OPShardAPI {

    private static final Gson GSON = new Gson();
    private static final HttpClient CLIENT = HttpClient.newHttpClient();

    private static final long CACHE_DURATION = 10_000; // 10 Sekunden Cache
    private static long lastFetchTime = 0;
    private static Map<String, Double> cachedRates = new HashMap<>();

    /**
     * Liefert den aktuellen Shard-Wert für ein Item.
     * @param itemKey z.B. "diamond_block", "netherite_ingot", "gräbergemisch", "holzbündel"
     * @return Shard-Wert, 0 wenn nicht gefunden
     */
    public static double getRate(String itemKey) {
        long now = System.currentTimeMillis();

        if (now - lastFetchTime > CACHE_DURATION || cachedRates.isEmpty()) {
            try {
                String url = "https://api.opsucht.net/merchant/rates";
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .GET()
                        .build();

                HttpResponse<java.io.InputStream> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofInputStream());
                try (InputStreamReader reader = new InputStreamReader(response.body())) {
                    Type listType = new TypeToken<List<Map<String, Object>>>() {}.getType();
                    List<Map<String, Object>> data = GSON.fromJson(reader, listType);

                    cachedRates.clear();
                    for (Map<String, Object> entry : data) {
                        String source = ((String) entry.get("source"));

                        // Normalisierung für Paper-Items
                        if (source.contains("minecraft:paper")) {
                            if (source.contains("Gräbergemisch")) source = "gräbergemisch";
                            else if (source.contains("Holzbündel")) source = "holzbündel";
                        }

                        double rate = ((Number) entry.get("exchangeRate")).doubleValue();
                        cachedRates.put(source.toLowerCase(), rate);
                    }
                    lastFetchTime = now;
                }
            } catch (Exception e) {
                e.printStackTrace();
                return 0.0;
            }
        }

        return cachedRates.getOrDefault(itemKey.toLowerCase(), 0.0);
    }
}