package com.resistancedlc.lyrics;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

/**
 * LyricsNotificationHud — fade-in/out уведомление «No lyrics» в центре экрана.
 */
public class LyricsNotificationHud {

    private static String message = null;
    private static long startTime = 0;
    private static final long FADE_IN_MS = 3000L;    // 3 сек
    private static final long HOLD_MS = 1500L;       // 1.5 сек удержания
    private static final long FADE_OUT_MS = 500L;    // 0.5 сек выцветания
    private static final long TOTAL_MS = FADE_IN_MS + HOLD_MS + FADE_OUT_MS;

    /** Показать уведомление (перезаписывает предыдущее). */
    public static void show(String msg) {
        message = msg;
        startTime = System.currentTimeMillis();
    }

    public static void render(GuiGraphics graphics) {
        if (message == null) return;
        long elapsed = System.currentTimeMillis() - startTime;
        if (elapsed > TOTAL_MS) {
            message = null;
            return;
        }

        float alpha;
        if (elapsed < FADE_IN_MS) {
            alpha = (float) elapsed / FADE_IN_MS;
        } else if (elapsed < FADE_IN_MS + HOLD_MS) {
            alpha = 1.0f;
        } else {
            alpha = 1.0f - (float) (elapsed - FADE_IN_MS - HOLD_MS) / FADE_OUT_MS;
        }
        alpha = Math.max(0f, Math.min(1f, alpha));

        Minecraft mc = Minecraft.getInstance();
        int w = graphics.guiWidth();
        int h = graphics.guiHeight();
        int textW = mc.font.width(message);
        int x = (w - textW) / 2;
        int y = h / 2 - 30;

        int a = (int) (255 * alpha);
        int color = (a << 24) | 0xFFFFFF;

        // подложка
        int bgA = (int) (160 * alpha);
        graphics.fill(x - 8, y - 6, x + textW + 8, y + 14, (bgA << 24));
        graphics.drawString(mc.font, message, x, y, color, true);
    }

    public static void reset() {
        message = null;
    }
}