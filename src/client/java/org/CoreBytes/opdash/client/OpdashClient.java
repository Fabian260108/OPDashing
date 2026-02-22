package org.CoreBytes.opdash.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.text.Text;
import org.CoreBytes.opdash.client.CommandButtonScreen;
import org.lwjgl.glfw.GLFW;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OpdashClient implements ClientModInitializer {

    public static ConfigManager shardsConfig;
    public static OPDashHUD hud;
    private static KeyBinding openShardCalculatorScreen;

    // Keybinding-Feld global deklarieren
    private static KeyBinding openButtonScreen;

    // ROBUSTES REGEX (ohne OPSUCHT Prefix!)
    private static final Pattern EXCHANGE_PATTERN = Pattern.compile(
            "Du hast (\\d+)x ([^ ]+) in ([\\d,.]+) OPShards umgetauscht"
    );

    @Override
    public void onInitializeClient() {

        openShardCalculatorScreen = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "Öffne Shard Calculator",
                GLFW.GLFW_KEY_C, // Du kannst die Taste ändern
                "OPDash"
        ));

        // Prüfen, ob Shard Calculator Hotkey gedrückt wurde
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openShardCalculatorScreen.wasPressed()) {
                client.setScreen(new ShardCalculatorScreen());
            }
        });


        shardsConfig = new ConfigManager();
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

        String msg = rawMsg.replaceAll("§.", "");
        Matcher matcher = EXCHANGE_PATTERN.matcher(msg);
        if (!matcher.find()) return;

        int amount = Integer.parseInt(matcher.group(1));
        String item = matcher.group(2).toLowerCase();
        double shards = Double.parseDouble(matcher.group(3).replace(",", "."));

        shardsConfig.addShards(shards);
        shardsConfig.addTrade(item, amount, shards);

        if (item.contains("diamant")) {
            OPDashAPI.updateConfigFromAPI(shardsConfig, "diamond_block", amount);
        } else if (item.contains("netherite")) {
            OPDashAPI.updateConfigFromAPI(shardsConfig, "netherite_ingot", amount);
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            client.player.sendMessage(Text.literal("§aTrade erkannt: " + amount + "x " + item), false);
        }
    }
}