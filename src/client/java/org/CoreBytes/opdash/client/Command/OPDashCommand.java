package org.CoreBytes.opdash.client.Command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.CoreBytes.opdash.client.Config.ConfigManager;
import org.CoreBytes.opdash.client.OPDashHUD.OPDashHUD;
import org.CoreBytes.opdash.client.Shard.ShardCalculator;

import java.util.Map;

public class OPDashCommand {

    public static String prefix = "§b§lOPDASH §7>> ";

    // Nur diese Items für Autocomplete und Shard-Rechner
    private static final Map<String, String> ITEM_ALIASES = Map.of(
            "diamond_block", "diamond_block",
            "netherite_ingot", "netherite_ingot",
            "graebergemisch", "gräbergemisch",
            "gräbergemisch", "gräbergemisch",
            "holzbundel", "holzbündel",
            "holzbündel", "holzbündel"
    );

    public static void registerCommands(ConfigManager config, OPDashHUD hud) {

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {

            dispatcher.register(
                    ClientCommandManager.literal("opdash")

                            // HUD Toggle
                            .then(ClientCommandManager.literal("hud")
                                    .then(ClientCommandManager.literal("toggle")
                                            .executes(ctx -> {
                                                hud.toggleHUD();
                                                String status = hud.isHudEnabled() ? "eingeschaltet" : "ausgeschaltet";
                                                MinecraftClient.getInstance().player.sendMessage(
                                                        Text.literal(prefix + "§f§lOPDash HUD " + status)
                                                                .formatted(Formatting.GREEN),
                                                        false
                                                );
                                                return Command.SINGLE_SUCCESS;
                                            })
                                    )
                            )

                            // Reset
                            .then(ClientCommandManager.literal("reset")
                                    .executes(ctx -> {
                                        config.resetShards();
                                        config.addBuy(-config.getTotalBuy());
                                        config.addSell(-config.getTotalSell());

                                        MinecraftClient.getInstance().player.sendMessage(
                                                Text.literal(prefix + "§f§lOPShards, §c§lBuy §7§lund §a§lSell §7§lzurückgesetzt")
                                                        .formatted(Formatting.RED),
                                                false
                                        );
                                        return Command.SINGLE_SUCCESS;
                                    })
                            )

                            // Set
                            .then(ClientCommandManager.literal("set")
                                    .then(ClientCommandManager.argument("value", DoubleArgumentType.doubleArg())
                                            .executes(ctx -> {
                                                double value = DoubleArgumentType.getDouble(ctx, "value");
                                                config.setShards(value);

                                                MinecraftClient.getInstance().player.sendMessage(
                                                        Text.literal(prefix + "§f§lOPShards auf §c§l" + value + " §f§lgesetzt")
                                                                .formatted(Formatting.RED),
                                                        false
                                                );
                                                return Command.SINGLE_SUCCESS;
                                            })
                                    )
                            )

                            // Ziel
                            .then(ClientCommandManager.literal("ziel")
                                    .then(ClientCommandManager.argument("value", DoubleArgumentType.doubleArg(0))
                                            .executes(ctx -> {
                                                double goal = DoubleArgumentType.getDouble(ctx, "value");
                                                config.setShardsGoal(goal);

                                                MinecraftClient.getInstance().player.sendMessage(
                                                        Text.literal(prefix + "§7Shards-Ziel gesetzt: " + goal)
                                                                .formatted(Formatting.AQUA),
                                                        false
                                                );
                                                return Command.SINGLE_SUCCESS;
                                            })
                                    )
                            )

                            // Fortschritt
                            .then(ClientCommandManager.literal("fortschritt")
                                    .executes(ctx -> {
                                        double total = config.getDouble("totalShards", 0.0);
                                        double goal = config.getDouble("shardsGoal", 0.0);
                                        double remaining = goal - total;
                                        if (remaining < 0) remaining = 0;

                                        String message = prefix + String.format(
                                                "§f§lOPShards: %.2f §7| §a§lZiel: %.2f §7| §c§lVerbleibend: %.2f",
                                                total, goal, remaining
                                        ).replace('.', ',');

                                        MinecraftClient.getInstance().player.sendMessage(
                                                Text.literal(message).formatted(Formatting.YELLOW),
                                                false
                                        );
                                        return Command.SINGLE_SUCCESS;
                                    })
                            )

// 🔹 Shard Calculator mit Alias & optionaler Menge + Autocomplete
                            .then(ClientCommandManager.literal("shardcalculator")
                                    .then(ClientCommandManager.argument("item", StringArgumentType.string())
                                            .suggests((ctx, builder) -> {
                                                // Autocomplete nur für erlaubte Items (ASCII-Varianten)
                                                for (String s : new String[]{"diamond_block", "netherite_ingot", "graebergemisch", "holzbundel"}) {
                                                    if (s.toLowerCase().startsWith(builder.getRemaining().toLowerCase()))
                                                        builder.suggest(s);
                                                }
                                                return builder.buildFuture();
                                            })
                                            .then(ClientCommandManager.argument("amount", DoubleArgumentType.doubleArg(1))
                                                    .executes(ctx -> {
                                                        String inputItem = StringArgumentType.getString(ctx, "item").trim().toLowerCase();
                                                        double amount = DoubleArgumentType.getDouble(ctx, "amount");

                                                        // --- Alias-Map für Paper-Items / Umlaute ---
                                                        Map<String, String> ITEM_ALIASES = Map.of(
                                                                "diamond_block", "diamond_block",
                                                                "netherite_ingot", "netherite_ingot",
                                                                "graebergemisch", "gräbergemisch",
                                                                "gräbergemisch", "gräbergemisch",
                                                                "holzbundel", "holzbündel",
                                                                "holzbündel", "holzbündel"
                                                        );

                                                        String apiKey = ITEM_ALIASES.get(inputItem);
                                                        if (apiKey == null) {
                                                            MinecraftClient.getInstance().player.sendMessage(
                                                                    Text.literal("§b§lOPDASH §7>> §cFehler: Item '" + inputItem + "' nicht gefunden oder keine Shard-Rate verfügbar.")
                                                                            .formatted(Formatting.RED),
                                                                    false
                                                            );
                                                            return Command.SINGLE_SUCCESS;
                                                        }

                                                        double totalShards = ShardCalculator.calculateShards(apiKey, amount);
                                                        if (totalShards <= 0) {
                                                            MinecraftClient.getInstance().player.sendMessage(
                                                                    Text.literal("§b§lOPDASH §7>> §cFehler: Keine Shard-Rate verfügbar für '" + apiKey + "'.")
                                                                            .formatted(Formatting.RED),
                                                                    false
                                                            );
                                                            return Command.SINGLE_SUCCESS;
                                                        }

                                                        MinecraftClient.getInstance().player.sendMessage(
                                                                Text.literal(String.format("§b§lOPDASH §7>> §f%.0f x §b%s §f= §a§l%.2f §fOPShards", amount, inputItem, totalShards))
                                                                        .formatted(Formatting.GREEN),
                                                                false
                                                        );

                                                        return Command.SINGLE_SUCCESS;
                                                    })
                                            )
                                            // --- Wenn keine Menge angegeben wird, default = 1 ---
                                            .executes(ctx -> {
                                                String inputItem = StringArgumentType.getString(ctx, "item").trim().toLowerCase();

                                                Map<String, String> ITEM_ALIASES = Map.of(
                                                        "diamond_block", "diamond_block",
                                                        "netherite_ingot", "netherite_ingot",
                                                        "graebergemisch", "gräbergemisch",
                                                        "gräbergemisch", "gräbergemisch",
                                                        "holzbundel", "holzbündel",
                                                        "holzbündel", "holzbündel"
                                                );

                                                String apiKey = ITEM_ALIASES.get(inputItem);
                                                if (apiKey == null) {
                                                    MinecraftClient.getInstance().player.sendMessage(
                                                            Text.literal("§b§lOPDASH §7>> §cFehler: Item '" + inputItem + "' nicht gefunden oder keine Shard-Rate verfügbar.")
                                                                    .formatted(Formatting.RED),
                                                            false
                                                    );
                                                    return Command.SINGLE_SUCCESS;
                                                }

                                                double totalShards = ShardCalculator.calculateShards(apiKey, 1);
                                                if (totalShards <= 0) {
                                                    MinecraftClient.getInstance().player.sendMessage(
                                                            Text.literal("§b§lOPDASH §7>> §cFehler: Keine Shard-Rate verfügbar für '" + apiKey + "'.")
                                                                    .formatted(Formatting.RED),
                                                            false
                                                    );
                                                    return Command.SINGLE_SUCCESS;
                                                }

                                                MinecraftClient.getInstance().player.sendMessage(
                                                        Text.literal(String.format("§b§lOPDASH §7>> §f1x §b%s §f= §a§l%.2f §fOPShards", inputItem, totalShards))
                                                                .formatted(Formatting.GREEN),
                                                        false
                                                );

                                                return Command.SINGLE_SUCCESS;
                                            })
                                    )
                            )
            );
        });
    }
}