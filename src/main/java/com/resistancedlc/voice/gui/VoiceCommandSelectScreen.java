package com.resistancedlc.voice.gui;

import com.resistancedlc.LocalizationManager;
import com.resistancedlc.voice.VoiceCommandAction;
import com.resistancedlc.voice.VoiceCommandManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * VoiceCommandSelectScreen — список действий для записи голосовых команд.
 */
public class VoiceCommandSelectScreen extends Screen {

    private static final int PANEL_W = 400;
    private static final int PANEL_H = 320;

    private final Screen parent;
    private int panelX;
    private int panelY;

    public VoiceCommandSelectScreen(Screen parent) {
        super(Component.literal(LocalizationManager.get("gui.resistancedlc.voice.select.title")));
        this.parent = parent;
    }

    @Override
    protected void init() {
        this.panelX = (this.width - PANEL_W) / 2;
        this.panelY = (this.height - PANEL_H) / 2;

        int btnH = 32;
        int btnGap = 8;
        int startY = panelY + 70;

        VoiceCommandAction[] actions = VoiceCommandAction.values();
        for (int i = 0; i < actions.length; i++) {
            final VoiceCommandAction action = actions[i];
            int y = startY + i * (btnH + btnGap);

            boolean recorded = VoiceCommandManager.hasCommand(action);
            String status = recorded ? "§a✓" : "§7—";
            String name = LocalizationManager.get(
                    "gui.resistancedlc.voice.action." + action.getId());

            Button btn = Button.builder(
                            Component.literal(status + " §f" + name),
                            (b) -> Minecraft.getInstance().gui.setScreen(
                                    new VoiceCommandRecordScreen(this, action)))
                    .bounds(panelX + 20, y, PANEL_W - 40, btnH)
                    .build();
            this.addRenderableWidget(btn);
        }

        // Close button
        Button close = Button.builder(
                        Component.literal(LocalizationManager.get("gui.resistancedlc.panel.apply")),
                        (b) -> this.onClose())
                .bounds(panelX + PANEL_W / 2 - 40, panelY + PANEL_H - 40, 80, 24)
                .build();
        this.addRenderableWidget(close);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        // Затемнение фона
        graphics.fill(0, 0, this.width, this.height, 0x80000000);

        // Панель
        graphics.fill(panelX, panelY, panelX + PANEL_W, panelY + PANEL_H, 0xC0000000);

        // Рамка
        int borderColor = 0xFF00FF00;
        graphics.fill(panelX, panelY, panelX + PANEL_W, panelY + 2, borderColor);
        graphics.fill(panelX, panelY + PANEL_H - 2, panelX + PANEL_W, panelY + PANEL_H, borderColor);
        graphics.fill(panelX, panelY, panelX + 2, panelY + PANEL_H, borderColor);
        graphics.fill(panelX + PANEL_W - 2, panelY, panelX + PANEL_W, panelY + PANEL_H, borderColor);

        // Заголовок
        graphics.text(this.font,
                "§l" + LocalizationManager.get("gui.resistancedlc.voice.select.title"),
                panelX + 20, panelY + 20, borderColor, true);

        // Подзаголовок
        graphics.text(this.font,
                "§7" + LocalizationManager.get("gui.resistancedlc.voice.select.desc"),
                panelX + 20, panelY + 40, 0xFFFFFFFF, false);

        super.extractRenderState(graphics, mouseX, mouseY, delta);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.gui.setScreen(parent);
        }
    }
}