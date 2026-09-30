package com.resistancedlc.hud;

import net.minecraft.client.gui.GuiGraphics;

/**
 * HudWidget — интерфейс одного перетаскиваемого HUD-модуля.
 */
public interface HudWidget {

    String getId();

    String getDisplayName();

    /** Сырое X из ModConfig (может быть -1 = авто/центр). */
    int getRawX();

    /** Сырое Y из ModConfig (может быть -1 = авто/центр). */
    int getRawY();

    /** Эффективная X-координата с учётом -1 (для рендера/хит-теста). */
    int getEffectiveX(int screenWidth);

    /** Эффективная Y-координата с учётом -1. */
    int getEffectiveY(int screenHeight);

    /** Установить абсолютные координаты (drag). */
    void setPosition(int x, int y);

    int getDefaultX();
    int getDefaultY();

    boolean isEnabled();
    void setEnabled(boolean enabled);

    /** Хит-тест: попадает ли точка (mouseX, mouseY) в виджет. */
    boolean hitTest(double mouseX, double mouseY, int screenWidth, int screenHeight);

    int renderPreview(GuiGraphics graphics, int mouseX, int mouseY, float alpha);

    int getWidth();
    int getHeight();
}