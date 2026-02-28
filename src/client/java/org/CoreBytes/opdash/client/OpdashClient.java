package org.CoreBytes.opdash.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.CoreBytes.opdash.client.BlockOverlay.DarkOakHighlighter;
import org.CoreBytes.opdash.client.BlockOverlay.OverlaySettingsScreen;
import org.CoreBytes.opdash.client.Command.OPDashCommand;
import org.CoreBytes.opdash.client.CommandsOverlay.CommandButtonScreen;
import org.CoreBytes.opdash.client.Config.ConfigManager;
import org.CoreBytes.opdash.client.Lootbox.PrestigeBoxScreen;
import org.CoreBytes.opdash.client.OPDashHUD.OPDashAPI;
import org.CoreBytes.opdash.client.OPDashHUD.OPDashHUD;
import org.CoreBytes.opdash.client.Shard.ShardCalculatorScreen;
import org.lwjgl.glfw.GLFW;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OpdashClient implements ClientModInitializer {

    public static ConfigManager shardsConfig;
    public static OPDashHUD hud;
    private static KeyBinding openShardCalculatorScreen;
    private static KeyBinding openPrestigeBoxScreen;

    // Keybinding-Feld global deklarieren
    private static KeyBinding openButtonScreen;

    public static KeyBinding toggleOverlayKey;
    public static KeyBinding openOverlaySettingsKey;
    public static boolean overlayEnabled = false;
    private static int sessionPlayTimeSeconds = 0;
    private static int playtimeTickCounter = 0;

    private static final Pattern EXCHANGE_PATTERN = Pattern.compile(
            ".*Du hast\\s+([\\d.,]+)\\s*[xX]\\s+(.+?)\\s+in\\s+([\\d.,]+)\\s+OPShards umgetauscht.*",
            Pattern.CASE_INSENSITIVE
    );
    @Override
    public void onInitializeClient() {

        toggleOverlayKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "Dark Oak Log Overlay Toggle", // Beschreibung
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_K,          // Standardtaste K
                "OPDash"      // Kategorie
        ));
        openOverlaySettingsKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "Dark Oak Overlay Settings",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_O,
                "OPDash"
        ));

        WorldRenderEvents.AFTER_TRANSLUCENT.register(context -> {
            if (overlayEnabled) {
                DarkOakHighlighter.renderOverlay(context.matrixStack(), 0.0f);
            }
        });

        net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (toggleOverlayKey.wasPressed()) {
                overlayEnabled = !overlayEnabled;
            }
        });



        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (toggleOverlayKey.wasPressed()) {
                overlayEnabled = !overlayEnabled;
            }
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openOverlaySettingsKey.wasPressed()) {
                if (shardsConfig != null) {
                    client.setScreen(new OverlaySettingsScreen(client.currentScreen, shardsConfig));
                }
            }
        });

        // Shard Calculator Hotkey



        openShardCalculatorScreen = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "Öffne Shard Calculator",
                GLFW.GLFW_KEY_C, // Du kannst die Taste ändern
                "OPDash"
        ));

        openPrestigeBoxScreen = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "Open Prestige Box Overlay",
                GLFW.GLFW_KEY_P,
                "OPDash"
        ));
        // Prüfen, ob Shard Calculator Hotkey gedrückt wurde
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openShardCalculatorScreen.wasPressed()) {
                client.setScreen(new ShardCalculatorScreen());
            }
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openPrestigeBoxScreen.wasPressed()) {
                client.setScreen(new PrestigeBoxScreen());
            }
        });

        shardsConfig = new ConfigManager();
        DarkOakHighlighter.loadFromConfig(shardsConfig);
        hud = new OPDashHUD(shardsConfig);

        // --- Keybinding registrieren ---
        openButtonScreen = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "Öffne das Button Fenster",
                GLFW.GLFW_KEY_B,
                "OPDash"
        ));

        OPDashCommand.registerCommands(shardsConfig, hud);

        HudRenderCallback.EVENT.register((context, tickDelta) -> hud.render(context));
        ClientTickEvents.END_CLIENT_TICK.register(client -> hud.tick(client));
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null || shardsConfig == null) {
                return;
            }

            playtimeTickCounter++;
            if (playtimeTickCounter >= 20) {
                playtimeTickCounter = 0;
                sessionPlayTimeSeconds++;
                shardsConfig.addModPlaySeconds(1);
            }
        });

        // Keybinding abfragen
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openButtonScreen.wasPressed()) {
                // Overlay öffnen
                client.setScreen(new CommandButtonScreen(shardsConfig));
            }
        });

        ClientReceiveMessageEvents.GAME.register((message, overlay) -> handleMessage(message.getString()));
    }

    private void handleMessage(String rawMsg) {
        if (rawMsg == null) return;

        String msg = rawMsg.replaceAll("§.", "").trim(); // Farbe entfernen

        Matcher matcher = EXCHANGE_PATTERN.matcher(msg);
        if (!matcher.find()) return;

        // Menge (Amount) korrekt parsen (Tausenderpunkte entfernen, Komma als Dezimal)
        int amount = (int) parseNumber(matcher.group(1));
        String item = matcher.group(2).toLowerCase();
        double shards = parseNumber(matcher.group(3));

        shardsConfig.addShards(shards);
        shardsConfig.addTrade(item, amount, shards);

        // Automatische API-Updates
        if (item.contains("diamant")) {
            OPDashAPI.updateConfigFromAPI(shardsConfig, "diamond_block", amount);
        } else if (item.contains("netherite")) {
            OPDashAPI.updateConfigFromAPI(shardsConfig, "netherite_ingot", amount);
        }

        MinecraftClient client = MinecraftClient.getInstance();

    }

    private static double parseNumber(String input) {
        if (input == null || input.isEmpty()) return 0.0;
        input = input.replace(".", ""); // Tausender entfernen
        input = input.replace(",", "."); // Komma zu Dezimal
        try {
            return Double.parseDouble(input);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    public static int getSessionPlayTimeSeconds() {
        return sessionPlayTimeSeconds;
    }
}

