package org.CoreBytes.opdash.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class CommandButton {
    private final String title;
    private final String command;

    public CommandButton(String title, String command) {
        this.title = title;
        this.command = command;
    }

    public String getTitle() { return title; }
    public String getCommand() { return command; }

    // Führt den Command oder Chat aus
    public void execute() {
        MinecraftClient client = MinecraftClient.getInstance();

        if (client.player != null && client.getNetworkHandler() != null) {

            if (command.startsWith("/")) {
                // Command senden
                client.player.networkHandler.sendChatCommand(command.substring(1));

                // GUI schließen
                client.setScreen(null);

            } else {
                // Normale Chat Nachricht senden
                client.player.networkHandler.sendChatMessage(command);
            }
        }
    }

    // Rot = Command, Grün = Chat
    public Text getColoredTitle() {
        if (command.startsWith("/")) return Text.literal(title).formatted(Formatting.RED).formatted(Formatting.BOLD);
        return Text.literal(title).formatted(Formatting.GREEN).formatted(Formatting.BOLD);
    }
}