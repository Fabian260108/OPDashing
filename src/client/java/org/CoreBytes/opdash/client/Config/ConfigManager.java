package org.CoreBytes.opdash.client.Config;

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
            configData.put("overlayRed", 0.60);
            configData.put("overlayGreen", 0.00);
            configData.put("overlayBlue", 0.60);
            configData.put("overlayAlpha", 0.08);
            configData.put("overlayRadius", 32.0);
            configData.put("overlayBlocks", createDefaultOverlayBlocks());
            configData.put("lootboxXpTotal", 0.0);
            configData.put("shopNameSymbolOwned", false);
            configData.put("shopNameSymbolEquipped", false);
            configData.put("shopPlayerCardOwned", false);
            configData.put("modPlayTimeSeconds", 0.0);
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

    // --- Lootbox XP ---
    public int getLootboxXpTotal() {
        return (int) Math.round(getDouble("lootboxXpTotal", 0.0));
    }

    public void addLootboxXp(int amount) {
        if (amount <= 0) return;
        configData.put("lootboxXpTotal", getLootboxXpTotal() + amount);
        save();
    }

    public boolean spendLootboxXp(int amount) {
        if (amount <= 0) return true;
        int current = getLootboxXpTotal();
        if (current < amount) return false;
        configData.put("lootboxXpTotal", current - amount);
        save();
        return true;
    }

    public boolean isShopNameSymbolOwned() {
        Object value = configData.get("shopNameSymbolOwned");
        return value instanceof Boolean b && b;
    }

    public void setShopNameSymbolOwned(boolean owned) {
        configData.put("shopNameSymbolOwned", owned);
        save();
    }

    public boolean isShopNameSymbolEquipped() {
        Object value = configData.get("shopNameSymbolEquipped");
        return value instanceof Boolean b && b;
    }

    public void setShopNameSymbolEquipped(boolean equipped) {
        configData.put("shopNameSymbolEquipped", equipped);
        save();
    }

    public boolean isShopPlayerCardOwned() {
        Object value = configData.get("shopPlayerCardOwned");
        return value instanceof Boolean b && b;
    }

    public void setShopPlayerCardOwned(boolean owned) {
        configData.put("shopPlayerCardOwned", owned);
        save();
    }

    // --- Mod Playtime ---
    public int getModPlayTimeSeconds() {
        return (int) Math.round(getDouble("modPlayTimeSeconds", 0.0));
    }

    public void addModPlaySeconds(int seconds) {
        if (seconds <= 0) return;
        configData.put("modPlayTimeSeconds", getModPlayTimeSeconds() + seconds);
        save();
    }

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
        configData.putIfAbsent("overlayRed", 0.60);
        configData.putIfAbsent("overlayGreen", 0.00);
        configData.putIfAbsent("overlayBlue", 0.60);
        configData.putIfAbsent("overlayAlpha", 0.08);
        configData.putIfAbsent("overlayRadius", 32.0);
        configData.putIfAbsent("overlayBlocks", createDefaultOverlayBlocks());
        configData.putIfAbsent("lootboxXpTotal", 0.0);
        configData.putIfAbsent("shopNameSymbolOwned", false);
        configData.putIfAbsent("shopNameSymbolEquipped", false);
        configData.putIfAbsent("shopPlayerCardOwned", false);
        configData.putIfAbsent("modPlayTimeSeconds", 0.0);
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

    @SuppressWarnings("unchecked")
    public List<Map<String, String>> getCommandButtons() {
        Object val = configData.get("commandButtons");
        if (val instanceof List<?> list) {
            return (List<Map<String,String>>) list;
        }
        List<Map<String,String>> newList = new ArrayList<>();
        configData.put("commandButtons", newList);
        save();
        return newList;
    }

    public void setCommandButtons(List<Map<String, String>> buttons) {
        configData.put("commandButtons", buttons);
        save();
    }

    public float getOverlayRed() {
        return (float) getDouble("overlayRed", 0.60);
    }

    public float getOverlayGreen() {
        return (float) getDouble("overlayGreen", 0.00);
    }

    public float getOverlayBlue() {
        return (float) getDouble("overlayBlue", 0.60);
    }

    public float getOverlayAlpha() {
        return (float) getDouble("overlayAlpha", 0.08);
    }

    public void setOverlayColor(float red, float green, float blue, float alpha) {
        configData.put("overlayRed", (double) red);
        configData.put("overlayGreen", (double) green);
        configData.put("overlayBlue", (double) blue);
        configData.put("overlayAlpha", (double) alpha);
        save();
    }

    public int getOverlayRadius() {
        int radius = (int) Math.round(getDouble("overlayRadius", 32.0));
        return Math.max(1, Math.min(60, radius));
    }

    public void setOverlayRadius(int radius) {
        configData.put("overlayRadius", (double) Math.max(1, Math.min(60, radius)));
        save();
    }

    @SuppressWarnings("unchecked")
    public List<String> getOverlayBlocks() {
        Object val = configData.get("overlayBlocks");
        if (val instanceof List<?> list) {
            List<String> result = new ArrayList<>();
            for (Object entry : list) {
                String id = String.valueOf(entry);
                if (id != null && !id.isBlank()) {
                    result.add(id.trim().toLowerCase());
                }
            }
            return result;
        }
        List<String> defaults = createDefaultOverlayBlocks();
        configData.put("overlayBlocks", defaults);
        save();
        return defaults;
    }

    public void setOverlayBlocks(List<String> blocks) {
        configData.put("overlayBlocks", blocks);
        save();
    }

    private static List<String> createDefaultOverlayBlocks() {
        List<String> list = new ArrayList<>();
        list.add("minecraft:dark_oak_log");
        return list;
    }

}
