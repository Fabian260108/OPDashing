package org.CoreBytes.opdash.client.CommandsOverlay;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import org.CoreBytes.opdash.client.Config.ConfigManager;

public class EditCommandScreen extends Screen {

    private final CommandButtonScreen parent;
    private final ConfigManager config;
    private final int buttonIndex;
    private TextFieldWidget titleField;
    private TextFieldWidget commandField;

    public EditCommandScreen(CommandButtonScreen parent, ConfigManager config, int index) {
        super(Text.literal("Edit Command"));
        this.parent = parent;
        this.config = config;
        this.buttonIndex = index;
    }

    @Override
    protected void init() {
        this.clearChildren();

        var buttonMap = config.getCommandButtons().get(buttonIndex);

        // Titel-Feld (max 128 Zeichen)
        titleField = new TextFieldWidget(this.textRenderer, width / 2 - 100, 50, 200, 20, Text.literal(""));
        titleField.setMaxLength(128);

        // Command/Chat-Feld (max 1024 Zeichen)
        commandField = new TextFieldWidget(this.textRenderer, width / 2 - 100, 90, 200, 20, Text.literal(""));
        commandField.setMaxLength(1024);

        titleField.setText(buttonMap.get("title"));
        commandField.setText(buttonMap.get("command"));

        this.addSelectableChild(titleField);
        this.addSelectableChild(commandField);
        titleField.setFocused(true);

        // Speichern Button
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Speichern"), b -> save())
                .dimensions(width / 2 - 80, 130, 70, 20).build());

        // Abbrechen Button
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Abbrechen"), b ->
                        this.client.setScreen(parent))
                .dimensions(width / 2 + 10, 130, 70, 20).build());
    }

    private void save() {
        String title = titleField.getText();
        String command = commandField.getText();
        if (title.isEmpty() || command.isEmpty()) return;

        var map = config.getCommandButtons().get(buttonIndex);
        map.put("title", title);
        map.put("command", command);
        config.setCommandButtons(config.getCommandButtons());

        parent.refreshButtons();
        this.client.setScreen(parent);
    }

    @Override
    public void render(DrawContext drawContext, int mouseX, int mouseY, float delta) {
        this.renderBackground(drawContext);
        super.render(drawContext, mouseX, mouseY, delta);

        drawContext.drawCenteredTextWithShadow(textRenderer, "Titel", width / 2, 40, 0xFFFFFF);
        drawContext.drawCenteredTextWithShadow(textRenderer, "Command / Nachricht", width / 2, 80, 0xFFFFFF);

        titleField.render(drawContext, mouseX, mouseY, delta);
        commandField.render(drawContext, mouseX, mouseY, delta);
    }

    public void renderBackground(DrawContext drawContext) {
        drawContext.fillGradient(0, 0, width, height, 0xAA000000, 0xAA000000);
    }

    @Override
    public boolean shouldPause() { return false; }
}