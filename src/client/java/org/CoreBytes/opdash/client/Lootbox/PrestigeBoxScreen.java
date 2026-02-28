package org.CoreBytes.opdash.client.Lootbox;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.random.Random;
import org.CoreBytes.opdash.client.OpdashClient;
import org.CoreBytes.opdash.client.Shop.ItemShopScreen;

import java.util.ArrayList;
import java.util.List;

public class PrestigeBoxScreen extends Screen {

    private static final Random RANDOM = Random.create();
    private static final Identifier XP_ORB_TEXTURE = Identifier.of("minecraft", "textures/entity/experience_orb.png");
    private static final int REEL_CARD_WIDTH = 86;
    private static final int REEL_CARD_HEIGHT = 44;
    private static final int REEL_CARD_GAP = 8;
    private static final int REEL_STEP = REEL_CARD_WIDTH + REEL_CARD_GAP;
    private static final int REEL_LENGTH = 44;
    private static final int REEL_STOP_INDEX = 34;

    private PrestigeBoxReward lastReward;
    private PrestigeBoxReward pendingReward;
    private final List<PrestigeBoxReward> reelItems = new ArrayList<>();
    private int animationTicks;
    private int rewardFlashTicks;
    private int openImpactTicks;
    private int xpPopupTicks;
    private int xpPopupAmount;
    private double reelOffset;
    private double reelTargetOffset;
    private double reelSpeed;
    private boolean reelSpinning;
    private boolean hasOpenedOnce;
    private ButtonWidget closeButton;

    public PrestigeBoxScreen() {
        super(Text.literal(PrestigeBoxManager.BOX_DISPLAY_NAME));
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int panelY = this.height / 2 - 96;
        int buttonsY = panelY + 170;

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Open Box"), button -> {
            if (this.reelSpinning) {
                return;
            }
            startReelSpin();
        }).dimensions(centerX - 75, buttonsY, 150, 20).build());

        closeButton = this.addDrawableChild(ButtonWidget.builder(Text.literal("Close"), button -> this.close())
                .dimensions(centerX - 75, buttonsY + 24, 150, 20).build());

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Item Shop"), button -> {
                    if (!this.reelSpinning) {
                        this.client.setScreen(new ItemShopScreen(this));
                    }
                }).dimensions(10, this.height - 28, 90, 20)
                .build());
    }

    @Override
    public void render(DrawContext drawContext, int mouseX, int mouseY, float delta) {
        drawContext.fillGradient(0, 0, this.width, this.height, 0xD2000000, 0xDE000000);

        int centerX = this.width / 2;
        int panelX1 = centerX - 120;
        int panelY1 = this.height / 2 - 96;
        int panelX2 = centerX + 120;
        int panelY2 = panelY1 + 214;

        drawContext.fillGradient(panelX1, panelY1, panelX2, panelY2, 0xF3282838, 0xF11B1B28);
        drawContext.drawBorder(panelX1, panelY1, panelX2 - panelX1, panelY2 - panelY1, 0xFF8A70C6);

        drawLightStreaks(drawContext, panelX1, panelY1, panelX2, panelY2, delta);

        int titlePulse = (int) ((Math.sin((animationTicks + delta) * 0.15f) + 1.0f) * 24.0f);
        int titleColor = brighten(0xFFEDE1FF, titlePulse);
        drawContext.drawCenteredTextWithShadow(this.textRenderer, Text.literal(PrestigeBoxManager.BOX_DISPLAY_NAME), centerX, panelY1 + 10, titleColor);
        drawContext.drawCenteredTextWithShadow(this.textRenderer, Text.literal("Premium Loot Overlay"), centerX, panelY1 + 24, 0xFFBEB8D0);

        int reelY = panelY1 - 20;

        drawContext.drawCenteredTextWithShadow(this.textRenderer, Text.literal("Current Reward"), centerX, panelY1 + 128, 0xFFD6D2E8);

        PrestigeBoxReward displayedReward = getDisplayedReward();
        if (reelSpinning) {
            drawContext.drawCenteredTextWithShadow(this.textRenderer, Text.literal("Spinning..."), centerX, panelY1 + 142, 0xFFE3D6FF);
        } else if (displayedReward == null) {
            drawContext.drawCenteredTextWithShadow(this.textRenderer, Text.literal("Press Open Box"), centerX, panelY1 + 142, 0xFF8F8A9F);
        } else {
            int rewardColor = displayedReward.color();
            if (rewardFlashTicks > 0) {
                rewardColor = brighten(rewardColor, 40);
            }
            drawContext.drawCenteredTextWithShadow(this.textRenderer, Text.literal(displayedReward.name()), centerX, panelY1 + 142, rewardColor);
            drawContext.drawCenteredTextWithShadow(this.textRenderer, Text.literal(displayedReward.rarity()), centerX, panelY1 + 154, rewardColor);
        }

        if (xpPopupTicks > 0) {
            int alpha = Math.min(255, 90 + xpPopupTicks * 8);
            int xpY = panelY1 + 52 - (24 - xpPopupTicks);
            drawContext.drawCenteredTextWithShadow(
                    this.textRenderer,
                    Text.literal("+" + xpPopupAmount + " XP"),
                    centerX,
                    xpY,
                    (alpha << 24) | 0xFFE3FF9C
            );
        }

        super.render(drawContext, mouseX, mouseY, delta);

        // Draw reel only after first open, above normal widgets so cards stay in foreground.
        if (hasOpenedOnce) {
            drawContext.getMatrices().push();
            drawContext.getMatrices().translate(0.0f, 0.0f, 520.0f);
            drawRewardReel(drawContext, centerX, reelY);
            drawContext.getMatrices().pop();
        }

        // Draw the box last at a very high z-layer so it is guaranteed to be in front.
        int shakeX = 0;
        int shakeY = 0;
        if (openImpactTicks > 0) {
            shakeX = (int) (Math.sin((openImpactTicks + delta) * 4.5f) * 2.0f);
            shakeY = (int) (Math.cos((openImpactTicks + delta) * 3.5f) * 1.5f);
        }
        drawContext.getMatrices().push();
        drawContext.getMatrices().translate(0.0f, 0.0f, 420.0f);
        renderLootBox(drawContext, centerX + shakeX, panelY1 + 38 + shakeY, delta);
        drawContext.getMatrices().pop();

        if (openImpactTicks > 0) {
            int flashAlpha = openImpactTicks * 6;
            drawContext.fill(panelX1 + 4, panelY1 + 4, panelX2 - 4, panelY2 - 4, (flashAlpha << 24) | 0xFFF5C9);
        }
    }

    @Override
    public void tick() {
        animationTicks++;
        if (reelSpinning) {
            double distance = reelTargetOffset - reelOffset;
            if (distance > 420.0) {
                reelSpeed = 30.0;
            } else {
                reelSpeed = Math.max(1.05, distance / 14.0);
            }
            reelOffset += reelSpeed;

            if (reelOffset >= reelTargetOffset - 0.9 && pendingReward != null) {
                reelOffset = reelTargetOffset;
                reelSpinning = false;
                lastReward = pendingReward;
                pendingReward = null;
                rewardFlashTicks = 28;
                openImpactTicks = 14;
                giveXpForReward(lastReward);
            }
        }
        if (closeButton != null) {
            closeButton.active = !reelSpinning;
        }
        if (rewardFlashTicks > 0) {
            rewardFlashTicks--;
        }
        if (openImpactTicks > 0) {
            openImpactTicks--;
        }
        if (xpPopupTicks > 0) {
            xpPopupTicks--;
        }
        super.tick();
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return !reelSpinning;
    }

    private void renderLootBox(DrawContext drawContext, int centerX, int baseY, float delta) {
        VisualStyle style = styleForCurrentReward();
        float pulse = (float) ((Math.sin((animationTicks + delta) * 0.18f) + 1.0f) * 0.5f);
        int glowAlpha = 35 + (int) (pulse * 95.0f);
        int sideGlow = (glowAlpha << 24) | (style.glowColor & 0x00FFFFFF);

        int boxX1 = centerX - 52;
        int boxY1 = baseY + 16;
        int boxX2 = centerX + 52;
        int boxY2 = baseY + 72;

        drawContext.fill(boxX1 - 20, boxY1 - 10, boxX1 - 8, boxY2 + 10, sideGlow);
        drawContext.fill(boxX2 + 8, boxY1 - 10, boxX2 + 20, boxY2 + 10, sideGlow);
        drawContext.fill(centerX - 46, boxY2 + 5, centerX + 46, boxY2 + 13, (65 << 24) | (style.glowColor & 0x00FFFFFF));

        drawContext.fillGradient(boxX1 + 2, boxY1 + 2, boxX2 + 10, boxY2 + 8, style.sideTop, style.sideBottom);
        drawContext.fillGradient(boxX1, boxY1, boxX2, boxY2, style.frontTop, style.frontBottom);
        drawContext.drawBorder(boxX1, boxY1, boxX2 - boxX1, boxY2 - boxY1, style.border);
        drawContext.drawBorder(boxX1 + 1, boxY1 + 1, (boxX2 - boxX1) - 2, (boxY2 - boxY1) - 2, withAlpha(style.highlight, 110));

        int lidY1 = boxY1 - 14;
        int lidLift = 0;
        if (rewardFlashTicks > 0) {
            lidLift = 3 + (rewardFlashTicks % 2);
        }
        drawContext.fillGradient(boxX1, lidY1 - lidLift, boxX2 + 12, boxY1 - 2 - lidLift, darken(style.sideTop, 0.18f), darken(style.sideBottom, 0.25f));
        drawContext.fillGradient(boxX1 - 2, lidY1 - lidLift, boxX2 + 2, boxY1 - 2 - lidLift, style.topTop, style.topBottom);
        drawContext.drawBorder(boxX1 - 2, lidY1 - lidLift, (boxX2 - boxX1) + 4, 12, style.border);

        drawContext.fill(centerX - 5, boxY1 + 16, centerX + 5, boxY1 + 30, 0xFFF2C871);
        drawContext.drawBorder(centerX - 5, boxY1 + 16, 10, 14, 0xFFFFF2C2);
        drawContext.fill(boxX1 + 14, boxY1, boxX1 + 17, boxY2, withAlpha(style.highlight, 190));
        drawContext.fill(boxX2 - 17, boxY1, boxX2 - 14, boxY2, withAlpha(style.highlight, 130));

        drawRivet(drawContext, boxX1 + 8, boxY1 + 8, style.rivet);
        drawRivet(drawContext, boxX2 - 8, boxY1 + 8, style.rivet);
        drawRivet(drawContext, boxX1 + 8, boxY2 - 8, style.rivet);
        drawRivet(drawContext, boxX2 - 8, boxY2 - 8, style.rivet);

        drawContext.fill(boxX1 + 20, boxY1 + 18, boxX2 - 20, boxY1 + 20, withAlpha(style.highlight, 170));
        drawContext.fill(boxX1 + 16, boxY1 + 36, boxX2 - 16, boxY1 + 37, withAlpha(style.shadow, 160));

        drawEnergySpark(drawContext, boxX1 - 14, boxY1 + 10 + (int) (pulse * 6), withAlpha(style.highlight, 220));
        drawEnergySpark(drawContext, boxX2 + 14, boxY1 + 16 - (int) (pulse * 5), withAlpha(style.highlight, 220));
        drawEnergySpark(drawContext, centerX, lidY1 - 6 - lidLift, withAlpha(style.core, 240));

        int bob = (int) (Math.sin((animationTicks + delta) * 0.16f) * 2.5f);
        int coreY = boxY1 - 4 - lidLift + bob;

        drawContext.fill(centerX - 30, coreY - 12, centerX + 30, coreY + 12, withAlpha(style.glowColor, 90));
        drawContext.fill(centerX - 20, coreY - 18, centerX + 20, coreY + 18, withAlpha(style.glowColor, 70));
        drawDiamond(drawContext, centerX, coreY, 11, style.core);
        drawDiamond(drawContext, centerX, coreY, 7, brighten(style.core, 28));
        drawDiamond(drawContext, centerX, coreY, 3, 0xFFFFFFFF);

        int ringShift = (int) ((animationTicks + delta) % 10.0f);
        drawContext.drawBorder(centerX - 24 - ringShift, coreY - 15, 48, 30, withAlpha(style.glowColor, 110));
        drawContext.drawBorder(centerX - 22 + ringShift, coreY - 13, 44, 26, withAlpha(style.glowColor, 85));
    }

    private void drawEnergySpark(DrawContext drawContext, int x, int y, int color) {
        drawContext.fill(x - 1, y - 3, x + 1, y + 3, color);
        drawContext.fill(x - 3, y - 1, x + 3, y + 1, color);
    }

    private void drawDiamond(DrawContext drawContext, int cx, int cy, int radius, int color) {
        for (int dy = -radius; dy <= radius; dy++) {
            int width = radius - Math.abs(dy);
            drawContext.fill(cx - width, cy + dy, cx + width + 1, cy + dy + 1, color);
        }
    }

    private void drawRivet(DrawContext drawContext, int x, int y, int color) {
        drawContext.fill(x - 1, y - 1, x + 2, y + 2, color);
        drawContext.fill(x, y, x + 1, y + 1, brighten(color, 25));
    }

    private void drawLightStreaks(DrawContext drawContext, int x1, int y1, int x2, int y2, float delta) {
        int width = x2 - x1;
        int t = animationTicks + (int) delta;
        int sweep1 = x1 + Math.floorMod(t * 3, width + 50) - 25;
        int sweep2 = x1 + Math.floorMod(t * 2 + 70, width + 60) - 30;

        drawContext.fill(sweep1, y1 + 30, sweep1 + 14, y2 - 34, 0x22C8A4FF);
        drawContext.fill(sweep2, y1 + 42, sweep2 + 10, y2 - 22, 0x1EE1CBFF);
    }

    private void drawRewardReel(DrawContext drawContext, int centerX, int y) {
        int reelWidth = 208;
        int reelX1 = centerX - reelWidth / 2;
        int reelX2 = centerX + reelWidth / 2;
        int reelY1 = y;
        int reelY2 = y + REEL_CARD_HEIGHT + 8;

        drawContext.fillGradient(reelX1, reelY1, reelX2, reelY2, 0xEE161622, 0xEE0F0F18);
        drawContext.drawBorder(reelX1, reelY1, reelX2 - reelX1, reelY2 - reelY1, 0xFF7C6AB0);

        if (reelItems.isEmpty()) {
            return;
        }

        double baseX = centerX - (REEL_CARD_WIDTH / 2.0) - reelOffset;
        for (int i = 0; i < reelItems.size(); i++) {
            int cardX = (int) Math.round(baseX + (i * REEL_STEP));
            if (cardX + REEL_CARD_WIDTH < reelX1 + 2 || cardX > reelX2 - 2) {
                continue;
            }
            drawRewardCard(drawContext, reelItems.get(i), cardX, reelY1 + 4);
        }

        drawContext.fill(reelX1, reelY1, reelX1 + 26, reelY2, 0xCC12121C);
        drawContext.fill(reelX2 - 26, reelY1, reelX2, reelY2, 0xCC12121C);
        drawContext.fill(centerX - 1, reelY1 + 2, centerX + 1, reelY2 - 2, 0xFFFFE08F);
        drawContext.fill(centerX - 8, reelY1, centerX + 8, reelY1 + 2, 0xFFFFE08F);
        drawContext.fill(centerX - 8, reelY2 - 2, centerX + 8, reelY2, 0xFFFFE08F);
    }

    private void drawRewardCard(DrawContext drawContext, PrestigeBoxReward reward, int x, int y) {
        int cardBorder = brighten(reward.color(), 18);
        drawContext.fillGradient(x, y, x + REEL_CARD_WIDTH, y + REEL_CARD_HEIGHT, darken(reward.color(), 0.45f), darken(reward.color(), 0.68f));
        drawContext.drawBorder(x, y, REEL_CARD_WIDTH, REEL_CARD_HEIGHT, cardBorder);

        int xpValue = extractXpValue(reward);
        drawContext.drawCenteredTextWithShadow(this.textRenderer, Text.literal(reward.rarity()), x + REEL_CARD_WIDTH / 2, y + 5, brighten(reward.color(), 26));
        drawContext.drawCenteredTextWithShadow(this.textRenderer, Text.literal(xpValue + " XP"), x + REEL_CARD_WIDTH / 2, y + 17, 0xFFFFFFFF);

        int orbCount = xpForRarity(reward.rarity());
        int spacing = 10;
        int totalWidth = (orbCount * spacing) - 2;
        int startX = x + (REEL_CARD_WIDTH - totalWidth) / 2;
        int iconY = y + 27;
        for (int i = 0; i < orbCount; i++) {
            drawXpOrb(drawContext, startX + (i * spacing) + 4, iconY + 5);
        }
    }

    private void drawXpOrb(DrawContext drawContext, int centerX, int centerY) {
        int frame = (animationTicks / 2) % 16;
        int u = (frame % 4) * 16;
        int v = (frame / 4) * 16;
        drawContext.getMatrices().push();
        drawContext.getMatrices().translate(centerX - 4.0f, centerY - 4.0f, 0.0f);
        drawContext.getMatrices().scale(0.5f, 0.5f, 1.0f);
        drawContext.drawTexture(RenderLayer::getGuiTextured, XP_ORB_TEXTURE, 0, 0, u, v, 16, 16, 64, 64);
        drawContext.getMatrices().pop();
    }

    private VisualStyle styleForCurrentReward() {
        PrestigeBoxReward displayedReward = getDisplayedReward();
        String rarity = displayedReward == null ? "Epic" : displayedReward.rarity();
        return switch (rarity.toLowerCase()) {
            case "common" -> new VisualStyle(
                    0xFF4A4E66, 0xFF2F3342, 0xFF565C75, 0xFF3A4053,
                    0xFFD1D7FF, 0xFFA1A9D9, 0xFF2A2E3F, 0xFF8C96D4, 0xFFD8E0FF, 0xFFC6CEE6, 0xFF9CA6D6, 0xFF8DA2FF
            );
            case "rare" -> new VisualStyle(
                    0xFF2F5679, 0xFF1E344D, 0xFF3D6C96, 0xFF2A4B69,
                    0xFFBEE8FF, 0xFF8CBAD4, 0xFF1A2A38, 0xFF62C0FF, 0xFFC7EEFF, 0xFFBCD2E1, 0xFF95BCD6, 0xFF5ECFFF
            );
            case "legendary" -> new VisualStyle(
                    0xFF7B4A1B, 0xFF4E2F13, 0xFF986126, 0xFF6C461E,
                    0xFFFFE2AD, 0xFFD5B37F, 0xFF3E260F, 0xFFFFA33A, 0xFFFFE8BE, 0xFFEAD6B8, 0xFFC09359, 0xFFFFA03A
            );
            case "mythic" -> new VisualStyle(
                    0xFF7A233C, 0xFF4E1526, 0xFFA12E50, 0xFF6A1F38,
                    0xFFFFC0D4, 0xFFD99AAE, 0xFF3C101E, 0xFFFF5F7A, 0xFFFFCAD8, 0xFFE8B7C3, 0xFFC58A9C, 0xFFFF5F7A
            );
            default -> new VisualStyle(
                    0xFF5A2D7A, 0xFF331B47, 0xFF70409A, 0xFF492A66,
                    0xFFE3C6FF, 0xFFBE96DF, 0xFF261434, 0xFFB06BFF, 0xFFF0D8FF, 0xFFE6D2F2, 0xFFAF8CCD, 0xFF9B6BFF
            );
        };
    }

    private PrestigeBoxReward getDisplayedReward() {
        if (reelSpinning && !reelItems.isEmpty()) {
            int idx = (int) Math.round(reelOffset / REEL_STEP);
            idx = Math.max(0, Math.min(reelItems.size() - 1, idx));
            return reelItems.get(idx);
        }
        return lastReward;
    }

    private void giveXpForReward(PrestigeBoxReward reward) {
        if (this.client == null || this.client.player == null || reward == null) {
            return;
        }
        int xpAmount = xpForRarity(reward.rarity());
        if (xpAmount <= 0) {
            return;
        }
        this.client.player.addExperience(xpAmount);
        if (OpdashClient.shardsConfig != null) {
            int rewardXpValue = extractXpValue(reward);
            if (rewardXpValue <= 0) {
                rewardXpValue = xpAmount;
            }
            OpdashClient.shardsConfig.addLootboxXp(rewardXpValue);
        }
        this.xpPopupAmount = xpAmount;
        this.xpPopupTicks = 24;
    }

    private int xpForRarity(String rarity) {
        return switch (rarity.toLowerCase()) {
            case "common" -> 1;
            case "rare" -> 2;
            case "epic" -> 3;
            case "legendary" -> 4;
            case "mythic" -> 5;
            default -> 1;
        };
    }

    private void startReelSpin() {
        hasOpenedOnce = true;
        pendingReward = PrestigeBoxManager.rollReward(RANDOM);
        reelItems.clear();

        for (int i = 0; i < REEL_LENGTH; i++) {
            if (i == REEL_STOP_INDEX) {
                reelItems.add(pendingReward);
            } else {
                reelItems.add(PrestigeBoxManager.rollReward(RANDOM));
            }
        }

        reelOffset = 0.0;
        reelSpeed = 30.0;
        reelTargetOffset = REEL_STOP_INDEX * REEL_STEP;
        reelSpinning = true;
        rewardFlashTicks = 0;
        openImpactTicks = 0;
    }

    private int extractXpValue(PrestigeBoxReward reward) {
        String name = reward.name();
        int value = 0;
        int i = 0;
        while (i < name.length() && Character.isDigit(name.charAt(i))) {
            value = (value * 10) + (name.charAt(i) - '0');
            i++;
        }
        return value;
    }

    private int withAlpha(int argbColor, int alpha) {
        int safeAlpha = Math.max(0, Math.min(255, alpha));
        return (safeAlpha << 24) | (argbColor & 0x00FFFFFF);
    }

    private int darken(int argbColor, float amount) {
        int a = (argbColor >>> 24) & 0xFF;
        int r = (argbColor >>> 16) & 0xFF;
        int g = (argbColor >>> 8) & 0xFF;
        int b = argbColor & 0xFF;
        float factor = Math.max(0.0f, 1.0f - amount);
        return (a << 24) | ((int) (r * factor) << 16) | ((int) (g * factor) << 8) | (int) (b * factor);
    }

    private int brighten(int argb, int amount) {
        int a = (argb >>> 24) & 0xFF;
        int r = Math.min(255, ((argb >>> 16) & 0xFF) + amount);
        int g = Math.min(255, ((argb >>> 8) & 0xFF) + amount);
        int b = Math.min(255, (argb & 0xFF) + amount);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private record VisualStyle(
            int frontTop,
            int frontBottom,
            int sideTop,
            int sideBottom,
            int border,
            int rivet,
            int shadow,
            int core,
            int highlight,
            int topTop,
            int topBottom,
            int glowColor
    ) {
    }
}
