package org.CoreBytes.opdash.client.BlockOverlay;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.CoreBytes.opdash.client.Config.ConfigManager;

import java.util.ArrayList;
import java.util.List;

public class OverlaySettingsScreen extends Screen {

    private static final int PALETTE_WIDTH = 200;
    private static final int PALETTE_HEIGHT = 96;

    private final Screen parent;
    private final ConfigManager config;

    private float hue;
    private float saturation;
    private float value;
    private float red;
    private float green;
    private float blue;

    private HueSlider hueSlider;
    private AlphaSlider alphaSlider;
    private RadiusSlider radiusSlider;
    private TextFieldWidget blockIdField;

    private int paletteX;
    private int paletteY;
    private boolean draggingPalette;
    private List<String> overlayBlocks;
    private boolean blocksLoaded;

    public OverlaySettingsScreen(Screen parent, ConfigManager config) {
        super(Text.literal("Dark Oak Overlay Settings"));
        this.parent = parent;
        this.config = config;
    }

    @Override
    protected void init() {
        this.clearChildren();

        this.paletteX = (this.width - PALETTE_WIDTH) / 2;
        this.paletteY = 34;
        if (!blocksLoaded || overlayBlocks == null) {
            this.overlayBlocks = new ArrayList<>(config.getOverlayBlocks());
            blocksLoaded = true;
        }

        float[] hsv = rgbToHsv(config.getOverlayRed(), config.getOverlayGreen(), config.getOverlayBlue());
        hue = hsv[0];
        saturation = hsv[1];
        value = hsv[2];
        updateRgbFromHsv();

        hueSlider = this.addDrawableChild(new HueSlider(paletteX, paletteY + PALETTE_HEIGHT + 8, PALETTE_WIDTH, 18, hue));
        alphaSlider = this.addDrawableChild(new AlphaSlider(paletteX, paletteY + PALETTE_HEIGHT + 30, PALETTE_WIDTH, 18, config.getOverlayAlpha()));
        radiusSlider = this.addDrawableChild(new RadiusSlider(paletteX, paletteY + PALETTE_HEIGHT + 52, PALETTE_WIDTH, 18, config.getOverlayRadius()));

        int centerX = this.width / 2;
        int inputY = paletteY + PALETTE_HEIGHT + 76;
        blockIdField = new TextFieldWidget(this.textRenderer, centerX - 102, inputY, 150, 18, Text.literal("minecraft:block_id"));
        blockIdField.setMaxLength(80);
        blockIdField.setText("minecraft:diamond_block");
        this.addDrawableChild(blockIdField);
        this.setFocused(blockIdField);

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Add"), b -> addBlock()).dimensions(centerX + 52, inputY, 50, 18).build());

        int rowY = inputY + 22;
        int maxRows = 5;
        for (int i = 0; i < overlayBlocks.size() && i < maxRows; i++) {
            String id = overlayBlocks.get(i);
            int index = i;

            String shortId = id.length() > 26 ? id.substring(0, 26) + "..." : id;
            ButtonWidget idLabel = this.addDrawableChild(ButtonWidget.builder(Text.literal(shortId), b -> {}).dimensions(centerX - 102, rowY, 174, 16).build());
            idLabel.active = false;
            this.addDrawableChild(ButtonWidget.builder(Text.literal("X"), b -> {
                overlayBlocks.remove(index);
                init();
            }).dimensions(centerX + 76, rowY, 26, 16).build());
            rowY += 18;
        }

        int buttonsY = rowY + 8;
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Save"), b -> {
            config.setOverlayColor(red, green, blue, alphaSlider.getValueF());
            config.setOverlayRadius(radiusSlider.getRadiusValue());
            config.setOverlayBlocks(overlayBlocks);
            DarkOakHighlighter.loadFromConfig(config);
            this.close();
        }).dimensions(centerX - 122, buttonsY, 78, 20).build());

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Reset"), b -> {
            float[] defaults = rgbToHsv(
                    DarkOakHighlighter.DEFAULT_RED,
                    DarkOakHighlighter.DEFAULT_GREEN,
                    DarkOakHighlighter.DEFAULT_BLUE
            );
            hue = defaults[0];
            saturation = defaults[1];
            value = defaults[2];
            updateRgbFromHsv();
            hueSlider.setValueF(hue);
            alphaSlider.setValueF(DarkOakHighlighter.DEFAULT_ALPHA);
            radiusSlider.setRadiusValue(DarkOakHighlighter.DEFAULT_RADIUS);
        }).dimensions(centerX - 39, buttonsY, 78, 20).build());

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Cancel"), b -> this.close())
                .dimensions(centerX + 44, buttonsY, 78, 20).build());
    }

    private void addBlock() {
        String idText = blockIdField.getText().trim().toLowerCase();
        Identifier id = normalizeBlockIdentifier(idText);
        if (id == null || !Registries.BLOCK.containsId(id)) {
            return;
        }
        String normalized = id.toString();
        if (!overlayBlocks.contains(normalized)) {
            overlayBlocks.add(normalized);
        }
        config.setOverlayBlocks(overlayBlocks);
        DarkOakHighlighter.loadFromConfig(config);
        init();
    }

    private Identifier normalizeBlockIdentifier(String input) {
        Identifier direct = Identifier.tryParse(input);
        if (direct != null) {
            return direct;
        }
        if (!input.contains(":")) {
            return Identifier.tryParse("minecraft:" + input);
        }
        return null;
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (blockIdField != null && blockIdField.isFocused() && blockIdField.charTyped(chr, modifiers)) {
            return true;
        }
        return super.charTyped(chr, modifiers);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (blockIdField != null && blockIdField.isFocused() && blockIdField.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void close() {
        if (this.client != null) {
            this.client.setScreen(parent);
        }
    }

    @Override
    public void render(DrawContext drawContext, int mouseX, int mouseY, float delta) {
        drawContext.fillGradient(0, 0, this.width, this.height, 0xAA000000, 0xAA000000);
        super.render(drawContext, mouseX, mouseY, delta);

        int centerX = this.width / 2;
        drawContext.drawCenteredTextWithShadow(this.textRenderer, this.title, centerX, 14, 0xFFFFFF);
        drawContext.drawCenteredTextWithShadow(this.textRenderer, Text.literal("Palette"), centerX, 24, 0xD0D0D0);
        renderPalette(drawContext);
        renderPaletteCursor(drawContext);

        int previewYTitle = paletteY + PALETTE_HEIGHT + 212;
        drawContext.drawCenteredTextWithShadow(this.textRenderer, Text.literal("Preview"), centerX, previewYTitle, 0xD0D0D0);
        int previewX1 = centerX - 90;
        int previewY1 = previewYTitle + 12;
        int previewX2 = centerX + 90;
        int previewY2 = previewY1 + 30;
        drawContext.drawBorder(previewX1 - 1, previewY1 - 1, (previewX2 - previewX1) + 2, (previewY2 - previewY1) + 2, 0xFF888888);
        drawContext.fill(previewX1, previewY1, previewX2, previewY2, currentColor());

        String info = String.format("R:%d G:%d B:%d A:%d%% Radius:%d",
                (int) (red * 255.0f),
                (int) (green * 255.0f),
                (int) (blue * 255.0f),
                (int) (alphaSlider.getValueF() * 100.0f),
                radiusSlider.getRadiusValue());
        drawContext.drawCenteredTextWithShadow(this.textRenderer, Text.literal(info), centerX, previewY2 + 6, 0xD0D0D0);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && inPalette(mouseX, mouseY)) {
            draggingPalette = true;
            setPaletteSelection(mouseX, mouseY);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (draggingPalette && button == 0) {
            setPaletteSelection(mouseX, mouseY);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            draggingPalette = false;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    private void renderPalette(DrawContext drawContext) {
        for (int dx = 0; dx < PALETTE_WIDTH; dx++) {
            float sat = dx / (float) (PALETTE_WIDTH - 1);
            for (int dy = 0; dy < PALETTE_HEIGHT; dy++) {
                float val = 1.0f - (dy / (float) (PALETTE_HEIGHT - 1));
                int rgb = hsvToRgb(hue, sat, val);
                drawContext.fill(paletteX + dx, paletteY + dy, paletteX + dx + 1, paletteY + dy + 1, 0xFF000000 | rgb);
            }
        }
        drawContext.drawBorder(paletteX - 1, paletteY - 1, PALETTE_WIDTH + 2, PALETTE_HEIGHT + 2, 0xFF888888);
    }

    private void renderPaletteCursor(DrawContext drawContext) {
        int cursorX = paletteX + Math.round(saturation * (PALETTE_WIDTH - 1));
        int cursorY = paletteY + Math.round((1.0f - value) * (PALETTE_HEIGHT - 1));
        drawContext.drawBorder(cursorX - 2, cursorY - 2, 5, 5, 0xFFFFFFFF);
    }

    private boolean inPalette(double mouseX, double mouseY) {
        return mouseX >= paletteX && mouseX < paletteX + PALETTE_WIDTH
                && mouseY >= paletteY && mouseY < paletteY + PALETTE_HEIGHT;
    }

    private void setPaletteSelection(double mouseX, double mouseY) {
        float normalizedX = (float) ((mouseX - paletteX) / PALETTE_WIDTH);
        float normalizedY = (float) ((mouseY - paletteY) / PALETTE_HEIGHT);
        saturation = MathHelper.clamp(normalizedX, 0.0f, 1.0f);
        value = 1.0f - MathHelper.clamp(normalizedY, 0.0f, 1.0f);
        updateRgbFromHsv();
    }

    private int currentColor() {
        int r = (int) (red * 255.0f);
        int g = (int) (green * 255.0f);
        int b = (int) (blue * 255.0f);
        int a = (int) (alphaSlider.getValueF() * 255.0f);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private void updateRgbFromHsv() {
        int rgb = hsvToRgb(hue, saturation, value);
        red = ((rgb >> 16) & 0xFF) / 255.0f;
        green = ((rgb >> 8) & 0xFF) / 255.0f;
        blue = (rgb & 0xFF) / 255.0f;
    }

    private static int hsvToRgb(float hue, float saturation, float value) {
        float h = (hue - (float) Math.floor(hue)) * 6.0f;
        int sector = (int) Math.floor(h);
        float f = h - sector;
        float p = value * (1.0f - saturation);
        float q = value * (1.0f - f * saturation);
        float t = value * (1.0f - (1.0f - f) * saturation);

        float r;
        float g;
        float b;
        switch (sector) {
            case 0 -> {
                r = value;
                g = t;
                b = p;
            }
            case 1 -> {
                r = q;
                g = value;
                b = p;
            }
            case 2 -> {
                r = p;
                g = value;
                b = t;
            }
            case 3 -> {
                r = p;
                g = q;
                b = value;
            }
            case 4 -> {
                r = t;
                g = p;
                b = value;
            }
            default -> {
                r = value;
                g = p;
                b = q;
            }
        }

        int ri = (int) (MathHelper.clamp(r, 0.0f, 1.0f) * 255.0f);
        int gi = (int) (MathHelper.clamp(g, 0.0f, 1.0f) * 255.0f);
        int bi = (int) (MathHelper.clamp(b, 0.0f, 1.0f) * 255.0f);
        return (ri << 16) | (gi << 8) | bi;
    }

    private static float[] rgbToHsv(float red, float green, float blue) {
        float max = Math.max(red, Math.max(green, blue));
        float min = Math.min(red, Math.min(green, blue));
        float delta = max - min;

        float hue;
        if (delta == 0.0f) {
            hue = 0.0f;
        } else if (max == red) {
            hue = ((green - blue) / delta) % 6.0f;
        } else if (max == green) {
            hue = ((blue - red) / delta) + 2.0f;
        } else {
            hue = ((red - green) / delta) + 4.0f;
        }
        hue /= 6.0f;
        if (hue < 0.0f) {
            hue += 1.0f;
        }

        float saturation = max == 0.0f ? 0.0f : delta / max;
        float value = max;
        return new float[]{hue, saturation, value};
    }

    private class HueSlider extends SliderWidget {
        private HueSlider(int x, int y, int width, int height, float value) {
            super(x, y, width, height, Text.empty(), MathHelper.clamp(value, 0.0f, 1.0f));
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            setMessage(Text.literal("Hue: " + (int) (this.value * 360.0) + " deg"));
        }

        @Override
        protected void applyValue() {
            hue = (float) this.value;
            updateRgbFromHsv();
        }

        private void setValueF(float value) {
            this.value = MathHelper.clamp(value, 0.0f, 1.0f);
            applyValue();
            updateMessage();
        }
    }

    private static class AlphaSlider extends SliderWidget {
        private AlphaSlider(int x, int y, int width, int height, float value) {
            super(x, y, width, height, Text.empty(), MathHelper.clamp(value, 0.0f, 1.0f));
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            setMessage(Text.literal("Alpha: " + (int) (this.value * 100.0) + "%"));
        }

        @Override
        protected void applyValue() {
        }

        private float getValueF() {
            return (float) this.value;
        }

        private void setValueF(float value) {
            this.value = MathHelper.clamp(value, 0.0f, 1.0f);
            updateMessage();
        }
    }

    private static class RadiusSlider extends SliderWidget {
        private RadiusSlider(int x, int y, int width, int height, int radius) {
            super(x, y, width, height, Text.empty(),
                    (MathHelper.clamp(radius, 1, DarkOakHighlighter.MAX_RADIUS) - 1.0) / (DarkOakHighlighter.MAX_RADIUS - 1.0));
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            setMessage(Text.literal("Radius: " + getRadiusValue() + " blocks"));
        }

        @Override
        protected void applyValue() {
            updateMessage();
        }

        private int getRadiusValue() {
            return 1 + (int) Math.round(this.value * (DarkOakHighlighter.MAX_RADIUS - 1));
        }

        private void setRadiusValue(int radius) {
            int clamped = MathHelper.clamp(radius, 1, DarkOakHighlighter.MAX_RADIUS);
            this.value = (clamped - 1.0) / (DarkOakHighlighter.MAX_RADIUS - 1.0);
            updateMessage();
        }
    }
}
