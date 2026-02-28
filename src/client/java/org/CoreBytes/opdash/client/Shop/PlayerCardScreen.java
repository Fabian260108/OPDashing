package org.CoreBytes.opdash.client.Shop;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.CoreBytes.opdash.client.Config.ConfigManager;
import org.CoreBytes.opdash.client.OpdashClient;

public class PlayerCardScreen extends Screen {

    private static final int CARD_WIDTH = 300;
    private static final int CARD_HEIGHT = 230;
    private static final int UI_SHIFT_Y = -24;

    private final Screen parent;

    public PlayerCardScreen(Screen parent) {
        super(Text.literal("Player Card"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        Layout layout = getLayout();
        int buttonWidth = Math.min(180, this.width - 20);
        int buttonX = (this.width - buttonWidth) / 2;
        int buttonY = Math.min(this.height - 28, layout.cardY2 + 10);
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Back"), button -> this.close())
                .dimensions(buttonX, buttonY, buttonWidth, 20)
                .build());
    }

    @Override
    public void render(DrawContext drawContext, int mouseX, int mouseY, float delta) {
        Layout layout = getLayout();
        int centerX = this.width / 2;
        int cardX1 = layout.cardX1;
        int cardY1 = layout.cardY1;
        int cardX2 = layout.cardX2;
        int cardY2 = layout.cardY2;

        drawContext.fill(0, 0, this.width, this.height, 0xFF000000);
        super.render(drawContext, mouseX, mouseY, delta);

        RenderSystem.disableDepthTest();
        drawContext.getMatrices().push();
        drawContext.getMatrices().translate(0.0f, 0.0f, 200.0f);
        drawContext.fill(cardX1, cardY1, cardX2, cardY2, 0xFF111111);
        drawContext.drawBorder(cardX1, cardY1, cardX2 - cardX1, cardY2 - cardY1, 0xFFFFFFFF);
        drawContext.drawBorder(cardX1 + 1, cardY1 + 1, (cardX2 - cardX1) - 2, (cardY2 - cardY1) - 2, 0xFFAAAAAA);

        int titleX1 = cardX1 + 10;
        int titleY1 = cardY1 + 10;
        int titleX2 = cardX2 - 10;
        int titleY2 = titleY1 + 24;
        drawContext.fill(titleX1, titleY1, titleX2, titleY2, 0xFF1A1A1A);
        drawContext.drawBorder(titleX1, titleY1, titleX2 - titleX1, titleY2 - titleY1, 0xFFFFFFFF);
        drawCenteredText(drawContext, Text.literal("PLAYER CARD"), centerX, titleY1 + 8, 0xFFFFFF00);

        int headTop = titleY2 + 8;

        ConfigManager config = OpdashClient.shardsConfig;
        String playerName = this.client != null && this.client.player != null
                ? this.client.player.getName().getString()
                : "Unknown";

        int nameY = headTop + 76;
        drawCenteredText(drawContext, Text.literal(playerName), centerX, nameY, 0xFFFFFF00);

        int statsX = cardX1 + 18;
        int statsY = nameY + 14;
        int statLineSpacing = 14;

        int lootboxXp = config != null ? config.getLootboxXpTotal() : 0;
        double shards = config != null ? config.getTotalShards() : 0.0;
        double buy = config != null ? config.getTotalBuy() : 0.0;
        double sell = config != null ? config.getTotalSell() : 0.0;
        int totalPlaySeconds = config != null ? config.getModPlayTimeSeconds() : 0;
        int sessionPlaySeconds = OpdashClient.getSessionPlayTimeSeconds();

        drawContext.drawText(this.textRenderer, Text.literal("Lootbox XP: " + lootboxXp), statsX, statsY, 0xFFFFFF00, false);
        drawContext.drawText(this.textRenderer, Text.literal(String.format("OPShards: %.2f", shards)), statsX, statsY + statLineSpacing, 0xFFFFFFFF, false);
        drawContext.drawText(this.textRenderer, Text.literal(String.format("Total Buy: %.2f", buy)), statsX, statsY + (statLineSpacing * 2), 0xFFFFFFFF, false);
        drawContext.drawText(this.textRenderer, Text.literal(String.format("Total Sell: %.2f", sell)), statsX, statsY + (statLineSpacing * 3), 0xFFFFFFFF, false);
        drawContext.drawText(this.textRenderer, Text.literal("Mod Time: " + formatDuration(totalPlaySeconds)), statsX, statsY + (statLineSpacing * 4), 0xFFFFFFFF, false);
        drawContext.drawText(this.textRenderer, Text.literal("Session Streak: " + formatDuration(sessionPlaySeconds)), statsX, statsY + (statLineSpacing * 5), 0xFFFFFFFF, false);

        // Draw head in the same foreground matrix stack.
        renderPlayerHead(drawContext, centerX, headTop, 6.0f);

        drawContext.getMatrices().pop();
        RenderSystem.enableDepthTest();
    }

    @Override
    public void close() {
        if (this.client != null) {
            this.client.setScreen(parent);
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    private void renderPlayerHead(DrawContext drawContext, int centerX, int topY, float scale) {
        if (this.client == null || !(this.client.player instanceof AbstractClientPlayerEntity player)) {
            drawFallbackHead(drawContext, centerX, topY);
            return;
        }

        Identifier skin = player.getSkinTextures().texture();
        int outerSize = 88;
        int innerSize = 64;

        drawContext.fill(centerX - (outerSize / 2), topY - 6, centerX + (outerSize / 2), topY + outerSize - 6, 0xFFFFFFFF);
        drawContext.drawBorder(centerX - (outerSize / 2), topY - 6, outerSize, outerSize, 0xFF000000);
        drawContext.fill(centerX - (innerSize / 2), topY + 6, centerX + (innerSize / 2), topY + 6 + innerSize, 0xFFFFFFFF);
        drawContext.drawBorder(centerX - (innerSize / 2), topY + 6, innerSize, innerSize, 0xFF000000);

        drawContext.getMatrices().push();
        drawContext.getMatrices().translate(centerX - 24.0f, topY + 14.0f, 0.0f);
        drawContext.getMatrices().scale(scale, scale, 1.0f);

        // Ensure skin is rendered at full opacity.
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        drawContext.drawTexture(RenderLayer::getGuiTextured, skin, 0, 0, 8, 8, 8, 8, 64, 64);

        drawContext.getMatrices().pop();
    }

    private void drawFallbackHead(DrawContext drawContext, int centerX, int topY) {
        int outerSize = 88;
        int innerSize = 64;
        drawContext.fill(centerX - (outerSize / 2), topY - 6, centerX + (outerSize / 2), topY + outerSize - 6, 0xFFEEEEEE);
        drawContext.drawBorder(centerX - (outerSize / 2), topY - 6, outerSize, outerSize, 0xFF000000);
        drawContext.fill(centerX - (innerSize / 2), topY + 6, centerX + (innerSize / 2), topY + 6 + innerSize, 0xFFD6A36A);
        drawContext.drawBorder(centerX - (innerSize / 2), topY + 6, innerSize, innerSize, 0xFF000000);
        drawContext.fill(centerX - 16, topY + 28, centerX - 8, topY + 36, 0xFF222222);
        drawContext.fill(centerX + 8, topY + 28, centerX + 16, topY + 36, 0xFF222222);
        drawContext.fill(centerX - 10, topY + 46, centerX + 10, topY + 50, 0xFF222222);
    }

    private String formatDuration(int totalSeconds) {
        int safe = Math.max(0, totalSeconds);
        int hours = safe / 3600;
        int minutes = (safe % 3600) / 60;
        int seconds = safe % 60;
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }

    private Layout getLayout() {
        int cardWidth = Math.min(CARD_WIDTH, this.width - 20);
        int cardHeight = Math.min(CARD_HEIGHT, this.height - 60);
        int cardX1 = (this.width - cardWidth) / 2;
        int cardY1 = Math.max(10, ((this.height - cardHeight - 34) / 2) + UI_SHIFT_Y);
        int cardX2 = cardX1 + cardWidth;
        int cardY2 = cardY1 + cardHeight;
        return new Layout(cardX1, cardY1, cardX2, cardY2);
    }

    private void drawCenteredText(DrawContext drawContext, Text text, int centerX, int y, int color) {
        int x = centerX - (this.textRenderer.getWidth(text) / 2);
        drawContext.drawText(this.textRenderer, text, x, y, color, false);
    }

    private record Layout(int cardX1, int cardY1, int cardX2, int cardY2) {
    }
}
