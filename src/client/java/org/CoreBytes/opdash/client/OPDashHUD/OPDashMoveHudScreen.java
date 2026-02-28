package org.CoreBytes.opdash.client.OPDashHUD;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

public class OPDashMoveHudScreen extends Screen {

    private final OPDashHUD hud;

    public OPDashMoveHudScreen(OPDashHUD hud) {
        super(Text.of("Move HUD"));
        this.hud = hud;

        // Cursor sichtbar erzwingen
        MinecraftClient.getInstance().mouse.unlockCursor();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        hud.renderOverlay(context, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            hud.startDragging((int) mouseX, (int) mouseY);
            return true;
        }
        return true; // unbedingt true, damit Minecraft die Maus nicht selbst behandelt
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            hud.stopDragging();
            return true;
        }
        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (button == 0) {
            hud.dragTo((int) mouseX, (int) mouseY);
            return true;
        }
        return true;
    }

    @Override
    public void removed() {
        // MoveMode beendet
        hud.setMoveModeActive(false);
        super.removed();
    }

    @Override
    public boolean shouldPause() { return false; }
}