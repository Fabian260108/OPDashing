package org.CoreBytes.opdash.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;

public class ConfigManager {

    private static final String FILE_NAME = "opdash.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type CONFIG_TYPE = new TypeToken<Map<String, Object>>() {}.getType();

    private final Path configPath;
    private Map<String, Object> configData;

    public ConfigManager() {
        Path configDir = FabricLoader.getInstance().getConfigDir();
        configPath = configDir.resolve(FILE_NAME);

        try {
            if (!Files.exists(configDir)) Files.createDirectories(configDir);
        } catch (IOException e) { e.printStackTrace(); }

        if (Files.exists(configPath)) load();
        else {
            configData = new HashMap<>();
            configData.put("totalShards", 0.0);
            configData.put("shardsGoal", 0.0);
            configData.put("totalBuy", 0.0);
            configData.put("totalSell", 0.0);
            configData.put("tradeHistory", new ArrayList<Map<String,Object>>());
            save();
        }
    }

    // --- Shards & Goal ---
    public double getTotalShards() { return ((Number) configData.getOrDefault("totalShards", 0.0)).doubleValue(); }
    public void addShards(double shards) { configData.put("totalShards", getTotalShards() + shards); save(); }
    public void setShards(double shards) { configData.put("totalShards", shards); save(); }
    public void resetShards() {
        configData.put("totalShards", 0.0);
        configData.put("totalBuy", 0.0);
        configData.put("totalSell", 0.0);
        save();
    }

    public double getShardsGoal() { return ((Number) configData.getOrDefault("shardsGoal", 0.0)).doubleValue(); }
    public void setShardsGoal(double goal) { configData.put("shardsGoal", goal); save(); }

    // --- Buy / Sell ---
    public double getTotalBuy() { return ((Number) configData.getOrDefault("totalBuy", 0.0)).doubleValue(); }
    public double getTotalSell() { return ((Number) configData.getOrDefault("totalSell", 0.0)).doubleValue(); }
    public void addBuy(double amount) { configData.put("totalBuy", getTotalBuy() + amount); save(); }
    public void addSell(double amount) { configData.put("totalSell", getTotalSell() + amount); save(); }

    // --- Trade History ---
    @SuppressWarnings("unchecked")
    public List<Map<String,Object>> getTradeHistory() {
        Object val = configData.get("tradeHistory");
        if (val instanceof List<?> list) return (List<Map<String,Object>>) list;
        List<Map<String,Object>> newList = new ArrayList<>();
        configData.put("tradeHistory", newList);
        return newList;
    }

    public void addTrade(String itemName, int amount, double shards) {
        Map<String,Object> trade = new HashMap<>();
        trade.put("item", itemName);
        trade.put("amount", amount);
        trade.put("shards", shards);
        trade.put("timestamp", Instant.now().toEpochMilli());
        getTradeHistory().add(trade);
        save();
    }

    // --- Load / Save ---
    private void save() {
        try (Writer writer = Files.newBufferedWriter(configPath, StandardCharsets.UTF_8)) {
            GSON.toJson(configData, writer);
        } catch (IOException e) { e.printStackTrace(); }
    }

    private void load() {
        try (Reader reader = Files.newBufferedReader(configPath, StandardCharsets.UTF_8)) {
            Map<String,Object> loaded = GSON.fromJson(reader, CONFIG_TYPE);
            configData = (loaded != null) ? loaded : new HashMap<>();
        } catch (IOException e) { e.printStackTrace(); configData = new HashMap<>(); }

        configData.putIfAbsent("totalShards", 0.0);
        configData.putIfAbsent("shardsGoal", 0.0);
        configData.putIfAbsent("totalBuy", 0.0);
        configData.putIfAbsent("totalSell", 0.0);
        configData.putIfAbsent("tradeHistory", new ArrayList<Map<String,Object>>());
        save();
    }

    public double getDouble(String key, double def) {
        Object val = configData.get(key);
        if (val instanceof Number n) return n.doubleValue();
        try { return Double.parseDouble(String.valueOf(val)); } catch (Exception e) { return def; }
    }

    public void setDouble(String key, double value) {
        configData.put(key, value);
        save();
    }
}