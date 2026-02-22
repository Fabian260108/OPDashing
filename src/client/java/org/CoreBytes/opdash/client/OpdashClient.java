package org.CoreBytes.opdash.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OpdashClient implements ClientModInitializer {

    public static ConfigManager shardsConfig;
    public static OPDashHUD hud;

    // ROBUSTES REGEX (ohne OPSUCHT Prefix!)
    private static final Pattern EXCHANGE_PATTERN = Pattern.compile(
            "Du hast (\\d+)x ([^ ]+) in ([\\d,.]+) OPShards umgetauscht"
    );

    @Override
    public void onInitializeClient() {
        shardsConfig = new ConfigManager();
        hud = new OPDashHUD(shardsConfig);

        OPDashCommand.registerCommands(shardsConfig, hud);

        HudRenderCallback.EVENT.register((context, tickDelta) -> OpdashClient.hud.render(context));
        ClientTickEvents.END_CLIENT_TICK.register(client -> OpdashClient.hud.tick(client));

        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            handleMessage(message.getString());
        });
    }

    private void handleMessage(String rawMsg) {


        if (rawMsg == null) {
            return;
        }

        String msg = rawMsg.replaceAll("§.", "");

        Matcher matcher = EXCHANGE_PATTERN.matcher(msg);

        if (!matcher.find()) {
            return;
        }

        int amount = Integer.parseInt(matcher.group(1));
        String item = matcher.group(2).toLowerCase();
        double shards = Double.parseDouble(matcher.group(3).replace(",", "."));


        shardsConfig.addShards(shards);
        shardsConfig.addTrade(item, amount, shards );


        if (item.contains("diamant")) {
            OPDashAPI.updateConfigFromAPI(shardsConfig, "diamond_block", amount);
        }
        else if (item.contains("netherite")) {
            OPDashAPI.updateConfigFromAPI(shardsConfig, "netherite_ingot", amount);
        }
        else {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            client.player.sendMessage(Text.literal("§aTrade erkannt: " + amount + "x " + item), false);
        }
    }
}