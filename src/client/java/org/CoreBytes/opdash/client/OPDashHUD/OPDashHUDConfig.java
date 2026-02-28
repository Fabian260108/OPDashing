package org.CoreBytes.opdash.client.OPDashHUD;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class OPDashHUDConfig {

    private static final String FILE_NAME = "opdash_hud.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final Path configPath;
    private Map<String, Object> configData;

    public OPDashHUDConfig() {
        Path configDir = FabricLoader.getInstance().getConfigDir();
        configPath = configDir.resolve(FILE_NAME);

        try {
            if (!Files.exists(configDir)) Files.createDirectories(configDir);
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (Files.exists(configPath)) load();
        else {
            configData = new HashMap<>();
            configData.put("x", 10);
            configData.put("y", 10);
            save();
        }
    }

    public int getX() { return ((Number) configData.getOrDefault("x", 10)).intValue(); }
    public int getY() { return ((Number) configData.getOrDefault("y", 10)).intValue(); }

    public void setX(int x) { configData.put("x", x); save(); }
    public void setY(int y) { configData.put("y", y); save(); }

    private void save() {
        try (Writer writer = Files.newBufferedWriter(configPath, StandardCharsets.UTF_8)) {
            GSON.toJson(configData, writer);
        } catch (IOException e) { e.printStackTrace(); }
    }

    private void load() {
        try (Reader reader = Files.newBufferedReader(configPath, StandardCharsets.UTF_8)) {
            Map<String,Object> loaded = GSON.fromJson(reader, Map.class);
            configData = (loaded != null) ? loaded : new HashMap<>();
        } catch (IOException e) { e.printStackTrace(); configData = new HashMap<>(); }
    }
}