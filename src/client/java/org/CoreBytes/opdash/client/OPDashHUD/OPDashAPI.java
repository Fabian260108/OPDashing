package org.CoreBytes.opdash.client.OPDashHUD;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.CoreBytes.opdash.client.Config.ConfigManager;

import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;

public class OPDashAPI {

    private static final Gson GSON = new Gson();
    private static final HttpClient CLIENT = HttpClient.newHttpClient();

    private static final long CACHE_DURATION = 10_000;
    private static long lastFetchTimeDiamond = 0;
    private static long lastFetchTimeNetherite = 0;
    private static Map<String, List<Map<String, Object>>> cachedDiamond = null;
    private static Map<String, List<Map<String, Object>>> cachedNetherite = null;

    public static Map<String, List<Map<String, Object>>> fetchItemData(String item) {
        try {
            long now = System.currentTimeMillis();

            if (item.equals("diamond_block") && cachedDiamond != null && now - lastFetchTimeDiamond < CACHE_DURATION)
                return cachedDiamond;

            if (item.equals("netherite_ingot") && cachedNetherite != null && now - lastFetchTimeNetherite < CACHE_DURATION)
                return cachedNetherite;

            String url = "https://api.opsucht.net/market/price/" + item.toLowerCase();
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
            HttpResponse<java.io.InputStream> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofInputStream());

            try (InputStreamReader reader = new InputStreamReader(response.body())) {
                Type type = new TypeToken<Map<String, List<Map<String, Object>>>>() {}.getType();
                Map<String, List<Map<String, Object>>> data = GSON.fromJson(reader, type);

                if (item.equals("diamond_block")) {
                    cachedDiamond = data;
                    lastFetchTimeDiamond = now;
                } else if (item.equals("netherite_ingot")) {
                    cachedNetherite = data;
                    lastFetchTimeNetherite = now;
                }

                return data;
            }

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void updateConfigFromAPI(ConfigManager config, String item, int tradedAmount) {
        try {

            String url = "https://api.opsucht.net/market/price/" + item;
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();

            HttpResponse<java.io.InputStream> response =
                    HttpClient.newHttpClient()
                            .send(request, HttpResponse.BodyHandlers.ofInputStream());


            try (InputStreamReader reader = new InputStreamReader(response.body())) {
                Type type = new TypeToken<Map<String, List<Map<String, Object>>>>() {}.getType();
                Map<String, List<Map<String, Object>>> data = GSON.fromJson(reader, type);



                double buyPrice = 0;
                double sellPrice = 0;

                for (List<Map<String, Object>> orders : data.values()) {
                    for (Map<String, Object> order : orders) {
                        String side = (String) order.get("orderSide");
                        double price = ((Number) order.get("price")).doubleValue();


                        if ("BUY".equalsIgnoreCase(side)) {
                            buyPrice = price;
                        }
                        if ("SELL".equalsIgnoreCase(side)) {
                            sellPrice = price;
                        }
                    }
                }



                double buyValue = buyPrice * tradedAmount;
                double sellValue = sellPrice * tradedAmount;



                config.addBuy(buyValue);
                config.addSell(sellValue);




            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}