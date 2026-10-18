package com.resistancedlc.voice.gui;

import com.resistancedlc.ConfigManager;
import com.resistancedlc.LocalizationManager;
import com.resistancedlc.ResistanceDLC;
import com.resistancedlc.voice.VoiceCommandAction;
import com.resistancedlc.voice.VoiceCommandListener;
import com.resistancedlc.voice.VoiceCommandManager;
import com.resistancedlc.voice.VoiceCommandRecorder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.io.File;

/**
 * VoiceCommandRecordScreen — запись голосовой команды.
 * Кнопка ▶ / ⏹ + визуализация столбиков.
 */
public class VoiceCommandRecordScreen extends Screen {

    private static final int PANEL_W = 380;
    private static final int PANEL_H = 260;

    private static final int BARS_COUNT = 32;
    private static final int BARS_WIDTH = 280;
    private static final int BARS_HEIGHT = 60;

    private final Screen parent;
    private final VoiceCommandAction action;

    private int panelX;
    private int panelY;

    private boolean recording = false;
    private long recordStartTime = 0;
    private static final long MAX_RECORD_MS = 5000; // максимум 5 сек

    private boolean saved = false;
    private String statusMessage = "";

    public VoiceCommandRecordScreen(Screen parent, VoiceCommandAction action) {
        super(Component.literal(LocalizationManager.get("gui.resistancedlc.voice.record.title")));
        this.parent = parent;
        this.action = action;
    }

    @Override
    protected void init() {
        this.panelX = (this.width - PANEL_W) / 2;
        this.panelY = (this.height - PANEL_H) / 2;

        updateRecordButton();
    }

    private void updateRecordButton() {
        // Удаляем старые кнопки
        this.clearWidgets();

        int btnSize = 48;
        int btnX = panelX + PANEL_W / 2 - btnSize / 2;
        int btnY = panelY + PANEL_H - 80;

        String icon = recording ? "⏹" : "▶";
        Button recBtn = Button.builder(
                        Component.literal("§l" + icon),
                        (b) -> toggleRecording())
                .bounds(btnX, btnY, btnSize, btnSize)
                .build();
        this.addRenderableWidget(recBtn);

        // Кнопка «Сохранить и выйти» — только если saved
        if (saved) {
            Button doneBtn = Button.builder(
                            Component.literal(LocalizationManager.get("gui.resistancedlc.panel.apply")),
                            (b) -> this.onClose())
                    .bounds(panelX + PANEL_W / 2 - 40, panelY + PANEL_H - 25, 80, 20)
                    .build();
            this.addRenderableWidget(doneBtn);
        } else {
            // Кнопка «Отмена»
            Button cancelBtn = Button.builder(
                            Component.literal(LocalizationManager.get("gui.resistancedlc.panel.cancel")),
                            (b) -> this.onClose())
                    .bounds(panelX + PANEL_W / 2 - 40, panelY + PANEL_H - 25, 80, 20)
                    .build();
            this.addRenderableWidget(cancelBtn);
        }
    }

    private void toggleRecording() {
        if (recording) {
            stopRecording();
        } else {
            startRecording();
        }
    }

    private void startRecording() {
        // Останавливаем Listener
        VoiceCommandListener.stop();
        try { Thread.sleep(500); } catch (InterruptedException ignored) {}

        VoiceCommandRecorder.start();
        recording = true;
        recordStartTime = System.currentTimeMillis();
        saved = false;
        statusMessage = LocalizationManager.get("gui.resistancedlc.voice.record.recording");

        updateRecordButton();
    }

    private void stopRecording() {
        short[] samples = VoiceCommandRecorder.stop();
        recording = false;

        if (samples == null || samples.length == 0) {
            statusMessage = "§c" + LocalizationManager.get("gui.resistancedlc.voice.record.no_samples");
            updateRecordButton();
            return;
        }

        // Сохраняем в voice/<action_id>.wav
        File out = VoiceCommandManager.getVoiceDir()
                .resolve(action.getId() + ".wav").toFile();
        boolean ok = VoiceCommandRecorder.saveWav(samples, out);

        if (ok) {
            VoiceCommandManager.reload(action);
            statusMessage = "§a" + LocalizationManager.get("gui.resistancedlc.voice.record.saved",
                    samples.length / 16000.0f);
            saved = true;
            ResistanceDLC.LOGGER.info("[Voice] Saved command " + action.getId()
                    + " (" + samples.length + " samples)");
        } else {
            statusMessage = "§c" + LocalizationManager.get("gui.resistancedlc.voice.record.save_failed");
        }

        updateRecordButton();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        // Авто-стоп через MAX_RECORD_MS
        if (recording && System.currentTimeMillis() - recordStartTime > MAX_RECORD_MS) {
            stopRecording();
        }

        // Фон
        graphics.fill(0, 0, this.width, this.height, 0x80000000);

        // Панель
        graphics.fill(panelX, panelY, panelX + PANEL_W, panelY + PANEL_H, 0xC0000000);

        // Рамка
        int borderColor = recording ? 0xFFFF2222 : 0xFF00FF00;
        graphics.fill(panelX, panelY, panelX + PANEL_W, panelY + 2, borderColor);
        graphics.fill(panelX, panelY + PANEL_H - 2, panelX + PANEL_W, panelY + PANEL_H, borderColor);
        graphics.fill(panelX, panelY, panelX + 2, panelY + PANEL_H, borderColor);
        graphics.fill(panelX + PANEL_W - 2, panelY, panelX + PANEL_W, panelY + PANEL_H, borderColor);

        // Заголовок
        graphics.text(this.font,
                "§l" + LocalizationManager.get("gui.resistancedlc.voice.record.title"),
                panelX + 20, panelY + 15, 0xFFFFFFFF, true);

        // Название действия
        String actionName = LocalizationManager.get(
                "gui.resistancedlc.voice.action." + action.getId());
        graphics.text(this.font, "§e" + actionName,
                panelX + 20, panelY + 35, 0xFFFFAA00, false);

        // Визуализация
        drawVisualizer(graphics);

        // Статус
        if (!statusMessage.isEmpty()) {
            int textW = this.font.width(statusMessage);
            graphics.text(this.font, statusMessage,
                    panelX + PANEL_W / 2 - textW / 2,
                    panelY + PANEL_H - 105, 0xFFFFFFFF, false);
        }

        // Подсказка
        if (!recording && !saved) {
            String hint = "§7" + LocalizationManager.get("gui.resistancedlc.voice.record.hint");
            int textW = this.font.width(hint);
            graphics.text(this.font, hint,
                    panelX + PANEL_W / 2 - textW / 2,
                    panelY + PANEL_H - 105, 0xFFFFAA00, false);
        }

        super.extractRenderState(graphics, mouseX, mouseY, delta);
    }

    private void drawVisualizer(GuiGraphicsExtractor graphics) {
        int bx = panelX + (PANEL_W - BARS_WIDTH) / 2;
        int by = panelY + 70;

        // Фон визуализации
        graphics.fill(bx, by, bx + BARS_WIDTH, by + BARS_HEIGHT, 0xFF101010);
        graphics.fill(bx, by, bx + BARS_WIDTH, by + 1, 0xFF404040);
        graphics.fill(bx, by + BARS_HEIGHT - 1, bx + BARS_WIDTH, by + BARS_HEIGHT, 0xFF404040);
        graphics.fill(bx, by, bx + 1, by + BARS_HEIGHT, 0xFF404040);
        graphics.fill(bx + BARS_WIDTH - 1, by, bx + BARS_WIDTH, by + BARS_HEIGHT, 0xFF404040);

        if (!recording) {
            // Неактивное состояние — плоская линия
            int midY = by + BARS_HEIGHT / 2;
            graphics.fill(bx + 4, midY, bx + BARS_WIDTH - 4, midY + 1, 0xFF404040);
            return;
        }

        // Активная визуализация
        float[] bars = VoiceCommandRecorder.getVisualizerBars(BARS_COUNT);
        int barW = BARS_WIDTH / BARS_COUNT;

        for (int i = 0; i < BARS_COUNT; i++) {
            float amp = bars[i];
            int h = (int) (amp * (BARS_HEIGHT - 4));
            int x = bx + i * barW;

            int color;
            if (amp < 0.4f) color = 0xFF00FF00;
            else if (amp < 0.7f) color = 0xFFFFFF00;
            else color = 0xFFFF2222;

            int midY = by + BARS_HEIGHT / 2;
            int top = midY - h / 2;
            graphics.fill(x + 1, top, x + barW - 1, top + h, color);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        if (recording) {
            VoiceCommandRecorder.stop();
        }
        if (this.minecraft != null) {
            this.minecraft.gui.setScreen(parent);
        }
    }
}