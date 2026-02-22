package org.CoreBytes.opdash.client;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

import java.util.List;

public class ShardCalculatorScreen extends Screen {

    private String inputValue = "1";
    private String resultText = "";

    private float fade = 0f;
    private float resultFade = 0f;
    private float resultScale = 0.8f;

    private float scale;
    private int centerX;

    private final List<String> ITEMS = List.of(
            "Diamond Block",
            "Netherite Ingot",
            "Gräbergemisch",
            "Holzbündel"
    );

    private String selectedItem = "Diamond Block";
    private ModernButton itemButton;

    public ShardCalculatorScreen() {
        super(Text.literal("OPDash Shard Rechner"));
    }

    @Override
    protected void init() {

        this.scale = Math.min(width / 800f, height / 600f);
        this.centerX = width / 2;

        int startY = (int) (70 * scale);
        int btnSize = (int) (55 * scale);
        int gap = (int) (15 * scale);
        int startX = centerX - ((btnSize * 3 + gap * 2) / 2);

        // Item Button
        itemButton = new ModernButton(
                centerX - (int)(120 * scale),
                startY,
                (int)(240 * scale),
                (int)(45 * scale),
                () -> Text.literal(selectedItem),
                () -> {
                    int idx = ITEMS.indexOf(selectedItem);
                    selectedItem = ITEMS.get((idx + 1) % ITEMS.size());
                });

        addDrawableChild(itemButton);

        // Zahlen 1-9
        int startButtonsY = startY + (int)(80 * scale);

        for (int i = 1; i <= 9; i++) {
            int col = (i - 1) % 3;
            int row = (i - 1) / 3;
            int num = i;

            addDrawableChild(new ModernButton(
                    startX + col * (btnSize + gap),
                    startButtonsY + row * (btnSize + gap),
                    btnSize,
                    btnSize,
                    () -> Text.literal(String.valueOf(num)),
                    () -> appendNumber(num)
            ));
        }

        // 0
        addDrawableChild(new ModernButton(
                startX + btnSize + gap,
                startButtonsY + 3 * (btnSize + gap),
                btnSize,
                btnSize,
                () -> Text.literal("0"),
                () -> appendNumber(0)
        ));

        // Backspace
        addDrawableChild(new ModernButton(
                startX,
                startButtonsY + 3 * (btnSize + gap),
                btnSize,
                btnSize,
                () -> Text.literal("←"),
                this::backspace
        ));

        // Berechnen
        addDrawableChild(new ModernButton(
                centerX - (int)(120 * scale),
                startButtonsY + 4 * (btnSize + gap),
                (int)(240 * scale),
                (int)(50 * scale),
                () -> Text.literal("BERECHNEN"),
                this::calculate
        ));

        // Schließen
        addDrawableChild(new ModernButton(
                centerX - (int)(120 * scale),
                startButtonsY + 4 * (btnSize + gap) + (int)(65 * scale),
                (int)(240 * scale),
                (int)(45 * scale),
                () -> Text.literal("SCHLIESSEN"),
                this::close
        ));
    }

    private void appendNumber(int num) {
        if (inputValue.equals("0")) inputValue = "";
        inputValue += num;
    }

    private void backspace() {
        if (!inputValue.isEmpty())
            inputValue = inputValue.substring(0, inputValue.length() - 1);

        if (inputValue.isEmpty())
            inputValue = "0";
    }

    private void calculate() {
        try {
            double amount = Double.parseDouble(inputValue);

            double total = ShardCalculator.calculateShards(
                    selectedItem.toLowerCase().replace(" ", "_"),
                    amount
            );

            resultText = String.format("%.0f × %s = %.2f OPShards",
                    amount, selectedItem, total);

            resultFade = 0f;
            resultScale = 0.8f;

        } catch (Exception e) {
            resultText = "Ungültige Zahl!";
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {

        // Wichtig: bei jedem Frame neu berechnen (Fix für Fenstergröße ändern)
        this.scale = Math.min(width / 800f, height / 600f);
        this.centerX = width / 2;

        fade = Math.min(fade + delta * 0.05f, 1f);
        resultFade = Math.min(resultFade + delta * 0.08f, 1f);
        resultScale = Math.min(resultScale + delta * 0.12f, 1f);

        renderBackground(context);

        // INPUT PANEL
        drawFancyPanel(
                context,
                centerX - (int)(120 * scale),
                (int)(25 * scale),
                (int)(240 * scale),
                (int)(55 * scale),
                inputValue,
                false
        );

        // RESULT PANEL (dynamisch unter Buttons)
        int rWidth = (int)(320 * resultScale * scale);
        int rHeight = (int)(60 * resultScale * scale);
        int rY = height - (int)(90 * scale);

        drawFancyPanel(
                context,
                centerX - rWidth / 2,
                rY,
                rWidth,
                rHeight,
                resultText,
                true
        );

        super.render(context, mouseX, mouseY, delta);
    }

    public void renderBackground(DrawContext context) {

        // Sanfter Fade
        int alpha = (int)(180 * fade);
        int dark = (alpha << 24) | 0x0B0B12;

        // Basis Dark Overlay
        context.fill(0, 0, width, height, dark);

        // Subtiler lila Verlauf von oben nach unten
        context.fillGradient(
                0, 0,
                width, height,
                (int)(120 * fade) << 24 | 0x2A003F,
                (int)(120 * fade) << 24 | 0x120018
        );

        // Center Glow (leicht)
        int glowSize = 300;
        int glowAlpha = (int)(80 * fade);
        int glowColor = (glowAlpha << 24) | 0xAA00FF;

        context.fill(
                width / 2 - glowSize,
                height / 2 - glowSize,
                width / 2 + glowSize,
                height / 2 + glowSize,
                glowColor
        );
    }
    private void drawFancyPanel(DrawContext context,
                                int x, int y,
                                int width, int height,
                                String text,
                                boolean isResult) {

        float glowStrength = isResult ? resultFade : fade;

        // Outer Glow
        int glowAlpha = (int)(100 * glowStrength) << 24;
        context.fill(
                x - 5,
                y - 5,
                x + width + 5,
                y + height + 5,
                glowAlpha | 0xAA00FF
        );

        // Background (leichtes Gradient)
        context.fillGradient(
                x,
                y,
                x + width,
                y + height,
                0xFF101014,
                0xFF1B1B24
        );

        // Subtiler Inner Highlight
        context.fillGradient(
                x + 2,
                y + 2,
                x + width - 2,
                y + height / 2,
                0x22FFFFFF,
                0x00000000
        );

        // Border
        context.drawBorder(x, y, width, height, 0xFFAA00FF);

        // Text
        context.drawCenteredTextWithShadow(
                textRenderer,
                text,
                x + width / 2,
                y + (height - 8) / 2,
                isResult ? 0xFFDD88FF : 0xFFFFFFFF
        );
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}