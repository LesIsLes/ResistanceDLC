package com.resistancedlc;

import com.resistancedlc.config.ModConfig;
import net.minecraft.client.gui.GuiGraphics;

/**
 * ThemeManager — анимированные темы HUD/GUI.
 *
 * HUD-текст: посимвольная волна (как Splash), цвет зависит от позиции символа.
 * GUI-рамки: волна по периметру (бежит по кругу).
 * GUI-акценты (стрелки, полоски): однотонный цвет по времени.
 *
 * Vanilla (index 0) — статичная тема, анимация игнорируется.
 */
public class ThemeManager {

    /**
     * Пресеты: {gui1, gui2, hud1, hud2}.
     * gui1/gui2 — цвета переливания для рамок GUI.
     * hud1/hud2 — цвета переливания для HUD-текста.
     */
    public static final int[][] PRESETS = {
            // Vanilla (не анимируется)
            {0xFF00FF00, 0xFF00FF00, 0xFF00FF00, 0xFF00FF00},
            // Dark: тёмно-серый ↔ почти чёрный
            {0xFF606060, 0xFF101010, 0xFF606060, 0xFF101010},
            // Candy: розовый ↔ нежно-банановый
            {0xFFFFB6C1, 0xFFFFF0A0, 0xFFFFB6C1, 0xFFFFF0A0},
            // Neon: небесно-голубой ↔ чёрный
            {0xFF00E5FF, 0xFF101010, 0xFF00E5FF, 0xFF101010},
            // Blood: красный ↔ бордовый
            {0xFFFF2020, 0xFF6A0000, 0xFFFF2020, 0xFF6A0000},
    };

    private static int currentThemeIndex = 0;

    /**
     * Определить индекс темы по текущим ModConfig.guiColor / hudColor.
     */
    public static void updateThemeIndexFromConfig() {
        int gui = ModConfig.guiColor;
        int hud = ModConfig.hudColor;
        for (int i = 0; i < PRESETS.length; i++) {
            if (PRESETS[i][0] == gui || PRESETS[i][2] == hud) {
                currentThemeIndex = i;
                break;
            }
        }
    }

    public static void setThemeIndex(int idx) {
        if (idx < 0 || idx >= PRESETS.length) return;
        currentThemeIndex = idx;
    }

    public static int getThemeIndex() {
        return currentThemeIndex;
    }

    /**
     * Общая фаза анимации (0..1), общая для HUD и GUI.
     * Один цикл ~2 сек при speed=1.0.
     */
    private static float getPhase() {
        long time = System.currentTimeMillis();
        float phase = (time * 0.0005f * ModConfig.themeAnimationSpeed) % 1.0f;
        if (phase < 0) phase += 1.0f;
        return phase;
    }

    /**
     * Однотонный цвет GUI по времени (для акцентов: стрелки, полоски).
     * Плавно пульсирует между A и B.
     */
    public static int getCurrentGuiColor() {
        if (currentThemeIndex == 0 || !ModConfig.themeAnimationEnabled) {
            return ModConfig.guiColor;
        }
        int[] p = PRESETS[currentThemeIndex];
        float phase = getPhase();
        float wave = (float) (Math.sin(phase * Math.PI * 2.0) * 0.5 + 0.5);
        return lerpColor(p[0], p[1], wave);
    }

    /**
     * Однотонный цвет HUD (без волны — для тех мест, где волна не нужна).
     */
    public static int getCurrentHudColor() {
        if (currentThemeIndex == 0 || !ModConfig.themeAnimationEnabled) {
            return ModConfig.hudColor;
        }
        int[] p = PRESETS[currentThemeIndex];
        float phase = getPhase();
        float wave = (float) (Math.sin(phase * Math.PI * 2.0) * 0.5 + 0.5);
        return lerpColor(p[2], p[3], wave);
    }

    /**
     * Посимвольный цвет HUD.
     * Волна сдвигается на 8% за каждый символ.
     */
    public static int getHudColorAt(int charIndex, int totalChars) {
        if (currentThemeIndex == 0 || !ModConfig.themeAnimationEnabled) {
            return ModConfig.hudColor;
        }

        int[] p = PRESETS[currentThemeIndex];
        int A = p[2];
        int B = p[3];

        float phase = getPhase();
        float pos = charIndex * 0.08f;

        float t = (pos + phase) % 1.0f;

        // A → B → A (пилообразный цикл)
        if (t < 0.5f) return lerpColor(A, B, t * 2.0f);
        else return lerpColor(B, A, (t - 0.5f) * 2.0f);
    }

    /**
     * Цвет точки рамки по позиции на периметре.
     */
    private static int getGuiColorAtPosition(float position, float total) {
        if (currentThemeIndex == 0 || !ModConfig.themeAnimationEnabled) {
            return ModConfig.guiColor;
        }

        int[] p = PRESETS[currentThemeIndex];
        int A = p[0];
        int B = p[1];

        float pos = (total > 0) ? (position / total) : 0.0f;
        float t = (pos + getPhase()) % 1.0f;

        if (t < 0.5f) return lerpColor(A, B, t * 2.0f);
        else return lerpColor(B, A, (t - 0.5f) * 2.0f);
    }

    /**
     * Рисует прямоугольную рамку с бегущей волной по периметру.
     * Шаг = 4 px для производительности.
     */
    public static void drawAnimatedBorder(GuiGraphics g, int x1, int y1, int x2, int y2, int thickness) {
        if (currentThemeIndex == 0 || !ModConfig.themeAnimationEnabled) {
            int color = ModConfig.guiColor;
            g.fill(x1, y1, x2, y1 + thickness, color);
            g.fill(x1, y2 - thickness, x2, y2, color);
            g.fill(x1, y1, x1 + thickness, y2, color);
            g.fill(x2 - thickness, y1, x2, y2, color);
            return;
        }

        final int STEP = 4;
        int W = x2 - x1;
        int H = y2 - y1;
        if (W <= 0 || H <= 0) return;
        float total = 2f * (W + H);

        // Верх (слева-направо)
        for (int x = x1; x < x2; x += STEP) {
            float pos = x - x1;
            int color = getGuiColorAtPosition(pos, total);
            g.fill(x, y1, Math.min(x + STEP, x2), y1 + thickness, color);
        }
        // Право (сверху-вниз)
        for (int y = y1; y < y2; y += STEP) {
            float pos = W + (y - y1);
            int color = getGuiColorAtPosition(pos, total);
            g.fill(x2 - thickness, y, x2, Math.min(y + STEP, y2), color);
        }
        // Низ (справа-налево)
        for (int x = x2; x > x1; x -= STEP) {
            float pos = W + H + (x2 - x);
            int color = getGuiColorAtPosition(pos, total);
            g.fill(Math.max(x - STEP, x1), y2 - thickness, x, y2, color);
        }
        // Лево (снизу-вверх)
        for (int y = y2; y > y1; y -= STEP) {
            float pos = 2f * W + H + (y2 - y);
            int color = getGuiColorAtPosition(pos, total);
            g.fill(x1, Math.max(y - STEP, y1), x1 + thickness, y, color);
        }
    }

    public static int lerpColor(int c1, int c2, float t) {
        if (t < 0) t = 0;
        if (t > 1) t = 1;

        int a1 = (c1 >> 24) & 0xFF, r1 = (c1 >> 16) & 0xFF, g1 = (c1 >> 8) & 0xFF, b1 = c1 & 0xFF;
        int a2 = (c2 >> 24) & 0xFF, r2 = (c2 >> 16) & 0xFF, g2 = (c2 >> 8) & 0xFF, b2 = c2 & 0xFF;

        int a = (int) (a1 + (a2 - a1) * t);
        int r = (int) (r1 + (r2 - r1) * t);
        int g = (int) (g1 + (g2 - g1) * t);
        int b = (int) (b1 + (b2 - b1) * t);

        return (a << 24) | (r << 16) | (g << 8) | b;
    }
}