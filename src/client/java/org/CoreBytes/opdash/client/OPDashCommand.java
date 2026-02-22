package org.CoreBytes.opdash.client;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class OPDashCommand {
    public static String prefix = "§b§lOPDASH §7>> ";

    public static void registerCommands(ConfigManager config, OPDashHUD hud) {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(
                    ClientCommandManager.literal("opdash")
                            .then(ClientCommandManager.literal("hud")
                                    .then(ClientCommandManager.literal("toggle")
                                            .executes(ctx -> {
                                                hud.toggleHUD();
                                                String status = hud.isHudEnabled() ? "eingeschaltet" : "ausgeschaltet";
                                                MinecraftClient.getInstance().player.sendMessage(
                                                        Text.literal(prefix + "§f§lOPDash HUD " + status).formatted(Formatting.GREEN), false
                                                );
                                                return Command.SINGLE_SUCCESS;
                                            })
                                    )
                            )
                            .then(ClientCommandManager.literal("reset")
                                    .executes(ctx -> {
                                        config.resetShards();
                                        // Reset Buy und Sell
                                        config.addBuy(-config.getTotalBuy());
                                        config.addSell(-config.getTotalSell());

                                        MinecraftClient.getInstance().player.sendMessage(
                                                Text.literal(prefix + "§f§lOPShards, §c§lBuy §7§lund §a§lSell §7§lzurückgesetzt").formatted(Formatting.RED), false
                                        );
                                        return Command.SINGLE_SUCCESS;
                                    })
                            ).then(ClientCommandManager.literal("set")
                                    .then(ClientCommandManager.argument("value", DoubleArgumentType.doubleArg())
                                            .executes(ctx -> {
                                                double value = DoubleArgumentType.getDouble(ctx, "value");
                                                config.setShards(value);

                                                MinecraftClient.getInstance().player.sendMessage(
                                                        Text.literal(prefix + "§f§lOPShards auf§c§l " + value +  " §f§lgesetzt").formatted(Formatting.RED), false
                                                );
                                                return Command.SINGLE_SUCCESS;
                                            })
                                    )
                            )
                            .then(ClientCommandManager.literal("ziel")
                                    .then(ClientCommandManager.argument("value", DoubleArgumentType.doubleArg(0))
                                            .executes(ctx -> {
                                                double goal = DoubleArgumentType.getDouble(ctx, "value");
                                                config.setShardsGoal(goal);
                                                MinecraftClient.getInstance().player.sendMessage(
                                                        Text.literal(prefix + "§7Shards-Ziel gesetzt: " + goal).formatted(Formatting.AQUA), false
                                                );
                                                return Command.SINGLE_SUCCESS;
                                            })
                                    )
                            )
                            .then(ClientCommandManager.literal("fortschritt")
                                    .executes(ctx -> {
                                        double total = config.getDouble("totalShards", 0.0);
                                        double goal = config.getDouble("shardsGoal", 0.0);
                                        double remaining = goal - total;
                                        if (remaining < 0) remaining = 0;

                                        String message = prefix + String.format("§f§lOPShards: %.2f §7| §a§lZiel: %.2f §7| §c§lVerbleibend: %.2f", total, goal, remaining)
                                                .replace('.', ',');
                                        MinecraftClient.getInstance().player.sendMessage(
                                                Text.literal(message).formatted(Formatting.YELLOW), false
                                        );
                                        return Command.SINGLE_SUCCESS;
                                    })
                            )
            );
        });
    }
}