package org.CoreBytes.opdash.client.OPDashHUD;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.KeyBinding;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.util.InputUtil;
import org.CoreBytes.opdash.client.Config.ConfigManager;
import org.lwjgl.glfw.GLFW;

public class OPDashHUD {

    public static String formatMoney(double value) {
        if (value >= 1_000_000_000) return String.format("%.2fB", value / 1_000_000_000).replace('.', ',');
        if (value >= 1_000_000) return String.format("%.2fM", value / 1_000_000).replace('.', ',');
        if (value >= 1_000) return String.format("%.2fk", value / 1_000).replace('.', ',');
        return String.format("%.2f", value).replace('.', ',');
    }

    private final ConfigManager shardsConfig;
    private final OPDashHUDConfig hudConfig;

    private boolean hudEnabled = true;
    private boolean moveModeActive = false;

    private boolean dragging = false;
    private int dragOffsetX = 0;
    private int dragOffsetY = 0;

    private final KeyBinding moveHUDKey;

    public OPDashHUD(ConfigManager shardsConfig) {
        this.shardsConfig = shardsConfig;
        this.hudConfig = new OPDashHUDConfig();

        moveHUDKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "OPDash moveHUD",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_H,
                "OPDash"
        ));
    }

    public void toggleHUD() {
        hudEnabled = !hudEnabled;
    }

    public boolean isHudEnabled() {
        return hudEnabled;
    }

    public void tick(MinecraftClient client) {
        if (moveHUDKey.wasPressed() && !moveModeActive) {
            moveModeActive = true;
            client.setScreen(new OPDashMoveHudScreen(this));
        }
    }

    public void render(DrawContext context) {
        if (!hudEnabled) return;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        int x = hudConfig.getX();
        int y = hudConfig.getY();

        // Werte aus Config
        String shards = formatMoney(shardsConfig.getTotalShards()).replace('.', ',');
        String goal = formatMoney(shardsConfig.getShardsGoal()).replace('.', ',');
        String buy = formatMoney(shardsConfig.getTotalBuy());
        String sell = formatMoney(shardsConfig.getTotalSell());

        int width = Math.max(Math.max(Math.max(
                                client.textRenderer.getWidth("OPShards: " + shards),
                                client.textRenderer.getWidth("Goal: " + goal)),
                        client.textRenderer.getWidth("Buy: " + buy)),
                client.textRenderer.getWidth("Sell: " + sell)) + 10;
        int height = 60;

        // Hintergrund mit leichtem Rand
        context.fill(x - 2, y - 2, x + width + 2, y + height + 2, 0xFF333333); // Rand
        context.fill(x, y, x + width, y + height, 0xAA222222); // HUD-Hintergrund

        int labelColor = 0xFFAAAAAA;
        int valueColor = 0xFF55DDFF;

        context.drawText(client.textRenderer, "OPShards:", x + 5, y, labelColor, false);
        context.drawText(client.textRenderer, shards, x + 5 + client.textRenderer.getWidth("OPShards: "), y, valueColor, false);

        context.drawText(client.textRenderer, "Ziel:", x + 5, y + 15, labelColor, false);
        context.drawText(client.textRenderer, goal, x + 5 + client.textRenderer.getWidth("Goal: "), y + 15, valueColor, false);

        context.drawText(client.textRenderer, "Buy:", x + 5, y + 30, labelColor, false);
        context.drawText(client.textRenderer, buy, x + 5 + client.textRenderer.getWidth("Buy: "), y + 30, valueColor, false);

        context.drawText(client.textRenderer, "Sell:", x + 5, y + 45, labelColor, false);
        context.drawText(client.textRenderer, sell, x + 5 + client.textRenderer.getWidth("Sell: "), y + 45, valueColor, false);
    }

    public void renderOverlay(DrawContext context, int mouseX, int mouseY) {
        render(context);

        MinecraftClient client = MinecraftClient.getInstance();

        int x = hudConfig.getX();
        int y = hudConfig.getY();
        int width = getHudWidth(client);
        int height = getHudHeight();

        context.fill(x, y, x + width, y + height, 0x66444444);

        context.drawText(client.textRenderer,
                "HUD Move Mode - Linksklick & ziehen, ESC zum Beenden",
                x, y - 12, 0xFFFFFF, false);
    }

    public void startDragging(int mouseX, int mouseY) {
        MinecraftClient client = MinecraftClient.getInstance();

        int x = hudConfig.getX();
        int y = hudConfig.getY();
        int width = getHudWidth(client);
        int height = getHudHeight();

        if (mouseX >= x && mouseX <= x + width &&
                mouseY >= y && mouseY <= y + height) {

            dragging = true;
            dragOffsetX = mouseX - x;
            dragOffsetY = mouseY - y;
        }
    }

    public void dragTo(int mouseX, int mouseY) {
        if (dragging) {
            hudConfig.setX(mouseX - dragOffsetX);
            hudConfig.setY(mouseY - dragOffsetY);
        }
    }

    public void stopDragging() {
        dragging = false;
    }

    public OPDashHUDConfig getHudConfig() {
        return hudConfig;
    }

    public void setMoveModeActive(boolean active) {
        moveModeActive = active;
    }

    public void render(DrawContext drawContext, RenderTickCounter renderTickCounter) {
        render(drawContext);
    }

    private int getHudWidth(MinecraftClient client) {
        String shards = String.format("%.2f", shardsConfig.getTotalShards()).replace('.', ',');
        String goal = String.format("%.2f", shardsConfig.getShardsGoal()).replace('.', ',');
        String buy = formatMoney(shardsConfig.getTotalBuy());
        String sell = formatMoney(shardsConfig.getTotalSell());

        return Math.max(Math.max(Math.max(
                                client.textRenderer.getWidth("OPShards: " + shards),
                                client.textRenderer.getWidth("Goal: " + goal)),
                        client.textRenderer.getWidth("Buy: " + buy)),
                client.textRenderer.getWidth("Sell: " + sell)) + 10;
    }

    private int getHudHeight() {
        return 60;
    }
}