package com.resistancedlc.util;

import net.minecraft.client.Minecraft;

/**
 * Хранит рассчитанные координаты элементов главного меню,
 * чтобы LogoRendererMixin и SplashRendererMixin могли
 * согласованно позиционироваться.
 *
 * v3.0.0-alpha.4 — адаптив под GUI Scale:
 *   - 1x–3x — стандартные позиции
 *   - 4x    — лого уходит вверх, splash между лого и кнопками
 */
public final class MainMenuLayout {

    private MainMenuLayout() {}

    // ===== Базовые размеры лого =====
    public static final int LOGO_W = 294;
    public static final int LOGO_H = 80;

    // ===== Отступы =====
    /** Минимальный отступ от верхнего края экрана на 4x. */
    public static final int TOP_MARGIN_4X = 12;

    /** Отступ от низа лого до splash на 4x. */
    public static final int SPLASH_GAP_4X = 10;

    // ===== Рассчитанные значения (обновляются в LogoRendererMixin) =====
    private static int lastLogoX = 0;
    private static int lastLogoY = 0;
    private static int lastScreenWidth = 0;
    private static int lastScreenHeight = 0;
    private static boolean lastWas4x = false;

    /**
     * Вызывается из LogoRendererMixin.renderCustomLogo.
     */
    public static void update(int logoX, int logoY, int screenWidth, int screenHeight, boolean is4x) {
        lastLogoX = logoX;
        lastLogoY = logoY;
        lastScreenWidth = screenWidth;
        lastScreenHeight = screenHeight;
        lastWas4x = is4x;
    }

    public static int getLogoX() { return lastLogoX; }
    public static int getLogoY() { return lastLogoY; }
    public static int getLogoBottom() { return lastLogoY + LOGO_H; }
    public static int getScreenWidth() { return lastScreenWidth; }
    public static int getScreenHeight() { return lastScreenHeight; }
    public static boolean is4x() { return lastWas4x; }

    /**
     * Y-координата верхней грани splash на 4x:
     * между низом лого и первой кнопкой.
     */
    public static int getSplashY4x() {
        return getLogoBottom() + SPLASH_GAP_4X;
    }

    /**
     * Рассчитать Y-позицию лого с учётом GUI Scale.
     *
     * @param screenHeight высота GUI в scaled-единицах
     * @param is4x         true, если фактический GUI Scale >= 4
     * @return Y-координата верхней грани лого
     */
    public static int calculateLogoY(int screenHeight, boolean is4x) {
        if (!is4x) {
            // 1x–3x — как раньше
            return 20;
        }
        // 4x — уходим вверх, но с отступом от края.
        // Раньше лого было на y=20. Сдвигаем к TOP_MARGIN_4X.
        return Math.max(TOP_MARGIN_4X, 20 - (20 - TOP_MARGIN_4X));
        // Упрощённо: return TOP_MARGIN_4X;
    }
}