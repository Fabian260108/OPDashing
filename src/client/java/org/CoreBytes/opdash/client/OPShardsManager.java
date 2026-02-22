package org.CoreBytes.opdash.client;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.fabricmc.loader.api.FabricLoader;


import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OPShardsManager {

    private static final String FILE_NAME = "opdash_shards.json";
    private static final Pattern EXCHANGE_PATTERN = Pattern.compile(
            "OPSUCHT » Du hast \\d+x [^ ]+ in ([\\d,.]+) OPShards umgetauscht\\."
    );

    private double totalShards = 0;
    private final File saveFile;

    public OPShardsManager() {
        // Config-Ordner
        File configDir = FabricLoader.getInstance().getConfigDir().toFile();

        // Ordner erstellen, falls er nicht existiert
        if (!configDir.exists()) {
            configDir.mkdirs();
        }

        // Speicherdatei
        saveFile = new File(configDir, FILE_NAME);

        // Datei erstellen, falls sie nicht existiert
        if (!saveFile.exists()) {
            totalShards = 0;
            saveShards();
        } else {
            loadShards();
        }
    }

    public void handleMessage(String msg) {
        Matcher matcher = EXCHANGE_PATTERN.matcher(msg);
        if (matcher.find()) {
            String opShardsStr = matcher.group(1).replace(",", ".");
            try {
                double shards = Double.parseDouble(opShardsStr);
                totalShards += shards;
                saveShards(); // direkt speichern
            } catch (NumberFormatException ignored) { }
        }
    }

    public double getTotalShards() {
        return totalShards;
    }

    public void resetShards() {
        totalShards = 0;
        saveShards();
    }

    private void saveShards() {
        try (FileWriter writer = new FileWriter(saveFile)) {
            Gson gson = new Gson();
            JsonObject obj = new JsonObject();
            obj.addProperty("totalShards", totalShards);
            gson.toJson(obj, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadShards() {
        if (!saveFile.exists()) return;
        try (FileReader reader = new FileReader(saveFile)) {
            Gson gson = new Gson();
            JsonObject obj = gson.fromJson(reader, JsonObject.class);
            if (obj.has("totalShards")) {
                totalShards = obj.get("totalShards").getAsDouble();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}