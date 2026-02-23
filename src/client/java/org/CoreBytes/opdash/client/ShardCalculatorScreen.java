package org.CoreBytes.opdash.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

import java.util.List;

public class ShardCalculatorScreen extends Screen {

    private String inputValue = "1";
    private String resultText = "";

    private float scale;
    private int centerX;

    private int closeButtonY;
    private int closeButtonHeight;

    private final List<String> ITEMS = List.of(
            "Diamond Block",
            "Netherite Ingot",
            "Gräbergemisch",
            "Holzbündel"
    );

    private String selectedItem = "Diamond Block";

    public ShardCalculatorScreen() {
        super(Text.literal("OPDash Shard Rechner"));
    }

    @Override
    protected void init() {
        rebuildLayout();
    }

    private void rebuildLayout() {

        clearChildren();

        this.scale = Math.min(width / 800f, height / 600f);
        this.centerX = width / 2;

        int startY = (int) (100 * scale);
        int btnSize = (int) (55 * scale);
        int gap = (int) (15 * scale);
        int startX = centerX - ((btnSize * 3 + gap * 2) / 2);

        // ITEM BUTTON
        addDrawableChild(new ModernButton(
                centerX - (int)(120 * scale),
                startY,
                (int)(240 * scale),
                (int)(40 * scale), // etwas kleiner
                () -> Text.literal(selectedItem),
                () -> {
                    int idx = ITEMS.indexOf(selectedItem);
                    selectedItem = ITEMS.get((idx + 1) % ITEMS.size());
                }
        ));

        int startButtonsY = startY + (int)(70 * scale);

        for (int i = 1; i <= 9; i++) {
            int col = (i - 1) % 3;
            int row = (i - 1) / 3;

            int finalI = i;
            int finalI1 = i;
            addDrawableChild(new ModernButton(
                    startX + col * (btnSize + gap),
                    startButtonsY + row * (btnSize + gap),
                    btnSize,
                    btnSize,
                    () -> Text.literal(String.valueOf(finalI)),
                    () -> appendNumber(finalI1)
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

        int calcY = startButtonsY + 4 * (btnSize + gap);

        addDrawableChild(new ModernButton(
                centerX - (int)(120 * scale),
                calcY,
                (int)(240 * scale),
                (int)(30 * scale),
                () -> Text.literal("BERECHNEN"),
                this::calculate
        ));

        int closeY = calcY + (int)(50 * scale);

        addDrawableChild(new ModernButton(
                centerX - (int)(120 * scale),
                closeY,
                (int)(240 * scale),
                (int)(30 * scale),
                () -> Text.literal("SCHLIESSEN"),
                this::close
        ));

        this.closeButtonY = closeY;
        this.closeButtonHeight = (int)(30 * scale);
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

        } catch (Exception e) {
            resultText = "Ungültige Zahl!";
        }
    }

    @Override
    public void resize(MinecraftClient client, int width, int height) {
        super.resize(client, width, height);
        rebuildLayout();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context);

        super.render(context, mouseX, mouseY, delta);

        drawInputPanel(context);
        drawResultPanel(context);
    }

    private void renderBackground(DrawContext context) {
        // Transparentes graues Overlay
        context.fill(0, 0, width, height, 0xB0222226);

        // Subtiler vertikaler Verlauf
        context.fillGradient(
                0, 0,
                width, height,
                0x60222226,
                0x60202024
        );
    }

    private void drawInputPanel(DrawContext context) {
        int panelWidth = (int)(240 * scale);
        int panelHeight = (int)(25 * scale); // kleiner gemacht
        int x = centerX - panelWidth / 2;
        int y = (int)(30 * scale);

        context.fill(x, y, x + panelWidth, y + panelHeight, 0xFF1C1C26); // 100% sichtbar
        context.drawBorder(x, y, panelWidth, panelHeight, 0xFFAA00FF);

        context.drawCenteredTextWithShadow(
                textRenderer,
                inputValue,
                x + panelWidth / 2,
                y + (panelHeight - 8) / 2,
                0xFFFFFFFF
        );
    }

    private void drawResultPanel(DrawContext context) {
        int panelWidth = (int)(300 * scale);
        int panelHeight = (int)(55 * scale);
        int x = centerX - panelWidth / 2;

        // 💥 Ganz unten unter Close-Button
        int y = closeButtonY + closeButtonHeight + (int)(10 * scale);

        context.fill(x, y, x + panelWidth, y + panelHeight, 0xFF1C1C26);
        context.drawBorder(x, y, panelWidth, panelHeight, 0xFFAA00FF);

        context.drawCenteredTextWithShadow(
                textRenderer,
                resultText.isEmpty() ? "— Ergebnis —" : resultText,
                x + panelWidth / 2,
                y + (panelHeight - 8) / 2,
                0xFFFFFFFF
        );
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}