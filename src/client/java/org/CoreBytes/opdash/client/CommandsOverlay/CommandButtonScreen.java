package org.CoreBytes.opdash.client.CommandsOverlay;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.CoreBytes.opdash.client.BlockOverlay.OverlaySettingsScreen;
import org.CoreBytes.opdash.client.Config.ConfigManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CommandButtonScreen extends Screen {

    private final ConfigManager config;
    private final List<CommandButton> buttons = new ArrayList<>();

    public CommandButtonScreen(ConfigManager config) {
        super(Text.literal("Command Buttons"));
        this.config = config;
        loadButtonsFromConfig();
    }

    private void loadButtonsFromConfig() {
        buttons.clear();
        for (var map : config.getCommandButtons()) {
            buttons.add(new CommandButton(map.get("title"), map.get("command")));
        }
    }

    @Override
    protected void init() {
        this.clearChildren();
        int y = 20;
        int screenWidth = this.width;

        for (int i = 0; i < buttons.size(); i++) {
            CommandButton btn = buttons.get(i);
            int index = i;

            // Hauptbutton
            this.addDrawableChild(ButtonWidget.builder(btn.getColoredTitle(), b -> btn.execute())
                    .dimensions(screenWidth / 2 - 110, y, 220, 25)
                    .tooltip(Tooltip.of(Text.literal(btn.getCommand())))
                    .build());

            this.addDrawableChild(ButtonWidget.builder(
                    Text.literal("✎").formatted(Formatting.GREEN),
                    b -> this.client.setScreen(new EditCommandScreen(this, config, index))
            ).dimensions(screenWidth / 2 + 115, y, 25, 25).build());

            // Delete ❌
            this.addDrawableChild(ButtonWidget.builder(Text.literal("§c❌"), b -> {
                buttons.remove(index);
                saveButtons();
                init();
            }).dimensions(screenWidth / 2 + 145, y, 25, 25).build());

            y += 35; // mehr Abstand für Übersicht
        }

        // + Button unten rechts, größer & auffällig
        this.addDrawableChild(ButtonWidget.builder(Text.literal("+"), b ->
                this.client.setScreen(new AddCommandScreen(this, config))
        ).dimensions(screenWidth - 45, this.height - 45, 35, 35).build());

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Overlay Color"), b ->
                this.client.setScreen(new OverlaySettingsScreen(this, config))
        ).dimensions(10, 10, 110, 20).build());
    }

    @Override
    public void render(DrawContext drawContext, int mouseX, int mouseY, float delta) {
        this.renderBackground(drawContext);

        // Hover Effekt: Buttons leicht heller beim Überfahren
        for (var child : this.children()) {
            if (child instanceof ButtonWidget btn && btn.isHovered()) {
                drawContext.fillGradient(btn.getX(), btn.getY(), btn.getX() + btn.getWidth(),
                        btn.getY() + btn.getHeight(), 0x77FFFFFF, 0x77FFFFFF);
            }
        }

        super.render(drawContext, mouseX, mouseY, delta);
    }

    public void renderBackground(DrawContext drawContext) {
        drawContext.fillGradient(0, 0, this.width, this.height, 0xAA000000, 0xAA000000);
    }

    @Override
    public boolean shouldPause() { return false; }

    public void saveButtons() {
        List<Map<String, String>> list = new ArrayList<>();
        for (CommandButton b : buttons) {
            HashMap<String, String> map = new HashMap<>();
            map.put("title", b.getTitle());
            map.put("command", b.getCommand());
            list.add(map);
        }
        config.setCommandButtons(list);
    }

    public void refreshButtons() {
        loadButtonsFromConfig();
        init();
    }
}
