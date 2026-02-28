package org.CoreBytes.opdash.client.Shard;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

public class ModernButton extends net.minecraft.client.gui.widget.ClickableWidget {

    private final Runnable onClick;
    private final java.util.function.Supplier<Text> textSupplier;

    private float hoverAnimation = 0f;
    private float clickAnimation = 0f;

    public ModernButton(int x, int y, int width, int height,
                        java.util.function.Supplier<Text> textSupplier,
                        Runnable onClick) {
        super(x, y, width, height, textSupplier.get());
        this.onClick = onClick;
        this.textSupplier = textSupplier;
    }

    @Override
    protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {

        setMessage(textSupplier.get());

        boolean hovered = isHovered();

        hoverAnimation = hovered
                ? Math.min(hoverAnimation + delta * 6f, 1f)
                : Math.max(hoverAnimation - delta * 6f, 0f);

        clickAnimation = Math.max(clickAnimation - delta * 8f, 0f);

        float scale = 1f + 0.08f * hoverAnimation;

        int scaledWidth = (int)(getWidth() * scale);
        int scaledHeight = (int)(getHeight() * scale);

        int drawX = getX() - (scaledWidth - getWidth()) / 2;
        int drawY = getY() - (scaledHeight - getHeight()) / 2;

        // Lila Glow
        if (hoverAnimation > 0) {
            int glowAlpha = (int)(100 * hoverAnimation) << 24;
            context.fill(
                    drawX - 3,
                    drawY - 3,
                    drawX + scaledWidth + 3,
                    drawY + scaledHeight + 3,
                    0x30AA00FF
            );
        }

        // Schwarzer Hintergrund
        context.fillGradient(
                drawX,
                drawY,
                drawX + scaledWidth,
                drawY + scaledHeight,
                0xFF0F0F12,
                0xFF1A1A1F
        );

        // Rahmen
        context.drawBorder(drawX, drawY, scaledWidth, scaledHeight, 0xFFAA00FF);

        // Text
        TextRenderer tr = MinecraftClient.getInstance().textRenderer;

        context.drawCenteredTextWithShadow(
                tr,
                getMessage(),
                drawX + scaledWidth / 2,
                drawY + (scaledHeight - 8) / 2,
                hovered ? 0xFFFFFFFF : 0xFFE0D0FF
        );
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        clickAnimation = 1f;
        onClick.run();
    }

    @Override
    protected void appendClickableNarrations(
            net.minecraft.client.gui.screen.narration.NarrationMessageBuilder builder) {
    }
}