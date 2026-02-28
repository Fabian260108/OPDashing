package org.CoreBytes.opdash.client.Shop;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import org.CoreBytes.opdash.client.Config.ConfigManager;
import org.CoreBytes.opdash.client.OpdashClient;

public class ItemShopScreen extends Screen {

    private static final int PLAYER_CARD_COST = 40;
    private static final int PANEL_WIDTH = 320;
    private static final int PANEL_HEIGHT = 180;
    private static final int UI_SHIFT_Y = -20;

    private final Screen parent;
    private ButtonWidget actionButton;
    private ButtonWidget playerCardButton;

    public ItemShopScreen(Screen parent) {
        super(Text.literal("Item Shop"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        Layout layout = getLayout();
        int mainButtonWidth = Math.min(190, this.width - 20);
        int mainButtonX = (this.width - mainButtonWidth) / 2;

        actionButton = this.addDrawableChild(ButtonWidget.builder(Text.literal("Loading..."), b -> onActionPressed())
                .dimensions(mainButtonX, layout.buyButtonY, mainButtonWidth, 20)
                .build());

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Back"), b -> this.close())
                .dimensions(mainButtonX, layout.backButtonY, mainButtonWidth, 20)
                .build());

        playerCardButton = this.addDrawableChild(ButtonWidget.builder(Text.literal("Player Card"), b -> {
                    if (OpdashClient.shardsConfig != null && OpdashClient.shardsConfig.isShopPlayerCardOwned()) {
                        this.client.setScreen(new PlayerCardScreen(this));
                    }
                }).dimensions(10, layout.playerCardButtonY, 110, 20)
                .build());

        refreshActionButton();
    }

    @Override
    public void tick() {
        refreshActionButton();
        super.tick();
    }

    @Override
    public void render(DrawContext drawContext, int mouseX, int mouseY, float delta) {
        Layout layout = getLayout();
        drawContext.fill(0, 0, this.width, this.height, 0xFF000000);
        super.render(drawContext, mouseX, mouseY, delta);

        RenderSystem.disableDepthTest();
        drawContext.getMatrices().push();
        drawContext.getMatrices().translate(0.0f, 0.0f, 200.0f);
        drawContext.fill(layout.panelX1, layout.panelY1, layout.panelX2, layout.panelY2, 0xFF111111);
        drawContext.drawBorder(layout.panelX1, layout.panelY1, layout.panelX2 - layout.panelX1, layout.panelY2 - layout.panelY1, 0xFFFFFFFF);
        drawContext.drawBorder(layout.panelX1 + 1, layout.panelY1 + 1, layout.panelX2 - layout.panelX1 - 2, layout.panelY2 - layout.panelY1 - 2, 0xFFAAAAAA);

        int titleX1 = layout.panelX1 + 10;
        int titleX2 = layout.panelX2 - 10;
        int titleY1 = layout.panelY1 + 10;
        int titleY2 = titleY1 + 22;
        drawContext.fill(titleX1, titleY1, titleX2, titleY2, 0xFF1A1A1A);
        drawContext.drawBorder(titleX1, titleY1, titleX2 - titleX1, titleY2 - titleY1, 0xFFFFFFFF);
        drawCenteredText(drawContext, Text.literal("ITEM SHOP"), this.width / 2, titleY1 + 7, 0xFFFFFF00);

        int infoX1 = layout.panelX1 + 10;
        int infoY1 = titleY2 + 8;
        int infoX2 = layout.panelX2 - 10;
        int infoY2 = infoY1 + 72;
        drawContext.fill(infoX1, infoY1, infoX2, infoY2, 0xFF000000);
        drawContext.drawBorder(infoX1, infoY1, infoX2 - infoX1, infoY2 - infoY1, 0xFFFFFFFF);

        ConfigManager config = OpdashClient.shardsConfig;
        int balance = config != null ? config.getLootboxXpTotal() : 0;
        drawContext.drawText(this.textRenderer, Text.literal("Lootbox XP: " + balance), infoX1 + 8, infoY1 + 22, 0xFFFFFF00, false);
        drawContext.drawText(this.textRenderer, Text.literal("Produkt: Player Card Access"), infoX1 + 8, infoY1 + 36, 0xFFFFFFFF, false);
        drawContext.drawText(this.textRenderer, Text.literal("Effekt: Profilkarte freischalten"), infoX1 + 8, infoY1 + 50, 0xFFFFFFFF, false);
        drawContext.drawText(this.textRenderer, Text.literal("Kosten: " + PLAYER_CARD_COST + " Lootbox XP"), infoX1 + 8, infoY1 + 64, 0xFFFFFF00, false);

        boolean owned = config != null && config.isShopPlayerCardOwned();
        String state = owned ? "Status: Owned" : "Status: Locked";
        drawContext.drawText(this.textRenderer, Text.literal(state), layout.panelX1 + 12, infoY2 + 8, 0xFFFFFFFF, false);
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

    private void onActionPressed() {
        ConfigManager config = OpdashClient.shardsConfig;
        if (config == null) {
            return;
        }

        if (!config.isShopPlayerCardOwned()) {
            if (!config.spendLootboxXp(PLAYER_CARD_COST)) {
                return;
            }
            config.setShopPlayerCardOwned(true);
        }

        refreshActionButton();
    }

    private void refreshActionButton() {
        if (actionButton == null) {
            return;
        }
        ConfigManager config = OpdashClient.shardsConfig;
        if (config == null) {
            actionButton.setMessage(Text.literal("Config Missing"));
            actionButton.active = false;
            if (playerCardButton != null) {
                playerCardButton.active = false;
            }
            return;
        }

        boolean owned = config.isShopPlayerCardOwned();

        if (!owned) {
            actionButton.setMessage(Text.literal("Buy Player Card (" + PLAYER_CARD_COST + " XP)"));
            actionButton.active = config.getLootboxXpTotal() >= PLAYER_CARD_COST;
        } else {
            actionButton.setMessage(Text.literal("Owned"));
            actionButton.active = false;
        }

        if (playerCardButton != null) {
            playerCardButton.active = owned;
        }
    }

    private Layout getLayout() {
        int panelWidth = Math.min(PANEL_WIDTH, this.width - 20);
        int panelHeight = Math.min(PANEL_HEIGHT, this.height - 70);
        int panelX1 = (this.width - panelWidth) / 2;
        int panelY1 = Math.max(10, ((this.height - panelHeight - 48) / 2) + UI_SHIFT_Y);
        int panelX2 = panelX1 + panelWidth;
        int panelY2 = panelY1 + panelHeight;

        int buyButtonY = Math.min(this.height - 52, panelY2 + 8);
        int backButtonY = buyButtonY + 24;
        int playerCardButtonY = this.height - 28;
        return new Layout(panelX1, panelY1, panelX2, panelY2, buyButtonY, playerCardButtonY, backButtonY);
    }

    private void drawCenteredText(DrawContext drawContext, Text text, int centerX, int y, int color) {
        int x = centerX - (this.textRenderer.getWidth(text) / 2);
        drawContext.drawText(this.textRenderer, text, x, y, color, false);
    }

    private record Layout(
            int panelX1,
            int panelY1,
            int panelX2,
            int panelY2,
            int buyButtonY,
            int playerCardButtonY,
            int backButtonY
    ) {
    }
}
