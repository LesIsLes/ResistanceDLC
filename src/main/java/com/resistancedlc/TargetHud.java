package com.resistancedlc;

import com.resistancedlc.config.ModConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.entity.LivingEntity;

/**
 * TargetHud — HUD-виджет с информацией о цели под прицелом.
 *
 * Показывает:
 *   - Мини-модель цели (слева)
 *   - Ник (сверху)
 *   - HP (снизу)
 *
 * Стиль — как уменьшенный MusicPlayerHud.
 * Фон — регулируемая прозрачность.
 * Цвета — градиент от color1 к color2 (анимированный).
 */
public class TargetHud {

    private static final int WIDGET_W = 130;
    private static final int WIDGET_H = 40;
    private static final int SKIN_SIZE = 28;
    private static final int PADDING = 6;
    private static final float GRADIENT_PERIOD = 60.0f;

    public static void render(GuiGraphicsExtractor graphics) {
        if (!ModConfig.targetHudEnabled) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        if (mc.gui.hud.isHidden()) return;

        // Цель — сущность под прицелом
        var target = mc.crosshairPickEntity;
        if (!(target instanceof LivingEntity living)) return;
        if (living == mc.player) return;
        if (!living.isAlive()) return;

        int screenW = graphics.guiWidth();
        int screenH = graphics.guiHeight();

        // Позиция — правее hotbar'а по центру
        int x = (ModConfig.targetHudX < 0)
                ? (screenW / 2 + 100)
                : ModConfig.targetHudX;
        int y = (ModConfig.targetHudY < 0)
                ? (screenH / 2 + 40)
                : ModConfig.targetHudY;

        // ⚠ Фиксируем реальную позицию — чтобы F6 работал корректно
        if (ModConfig.targetHudX < 0) ModConfig.targetHudX = x;
        if (ModConfig.targetHudY < 0) ModConfig.targetHudY = y;

        int color = getGradientColor();

        // Фон
        int bgAlpha = ModConfig.targetHudBgAlpha;
        int bgColor = (bgAlpha << 24) | 0x101010;
        graphics.fill(x, y, x + WIDGET_W, y + WIDGET_H, bgColor);

        // Границы
        int borderColor = (bgAlpha << 24) | (color & 0x00FFFFFF);
        graphics.fill(x, y, x + WIDGET_W, y + 1, borderColor);
        graphics.fill(x, y + WIDGET_H - 1, x + WIDGET_W, y + WIDGET_H, borderColor);
        graphics.fill(x, y, x + 1, y + WIDGET_H, borderColor);
        graphics.fill(x + WIDGET_W - 1, y, x + WIDGET_W, y + WIDGET_H, borderColor);

        // Скин (слева)
        int textX = x + PADDING;
        if (ModConfig.targetHudShowSkin) {
            drawSkin(graphics, mc, x + PADDING, y + (WIDGET_H - SKIN_SIZE) / 2, living);
            textX += SKIN_SIZE + 6;
        }

        // Ник
        if (ModConfig.targetHudShowName) {
            String name = living.getName().getString();
            if (living.isInvisible()) {
                name = "???";
            }
            if (name.length() > 16) name = name.substring(0, 14) + "…";
            graphics.text(mc.font, "§l" + name, textX, y + 6, color, true);
        }

        // HP
        if (ModConfig.targetHudShowHp) {
            float hp = living.getHealth();
            float maxHp = living.getMaxHealth();
            int hpColor = getHpColor(hp, maxHp);
            String hpStr = String.format(java.util.Locale.ROOT, "%.1f / %.1f", hp, maxHp);
            graphics.text(mc.font, hpStr, textX, y + 20, hpColor, false);
        }
    }

    // ===================== СКИН =====================

    private static void drawSkin(GuiGraphicsExtractor graphics, Minecraft mc, int x, int y, LivingEntity living) {
        // Невидимый — рисуем "?"
        if (living.isInvisible()) {
            drawEmptySkin(graphics, mc, x, y);
            return;
        }

        try {
            InventoryScreen.extractEntityInInventoryFollowsMouse(
                    graphics,
                    x, y,
                    x + SKIN_SIZE, y + SKIN_SIZE,
                    14,
                    0.0625f,
                    x + SKIN_SIZE / 2.0f,
                    y + SKIN_SIZE / 2.0f,
                    living
            );
        } catch (Throwable t) {
            drawEmptySkin(graphics, mc, x, y);
        }
    }

    private static void drawEmptySkin(GuiGraphicsExtractor graphics, Minecraft mc, int x, int y) {
        graphics.fill(x, y, x + SKIN_SIZE, y + SKIN_SIZE, 0xFF333333);
        graphics.fill(x, y, x + SKIN_SIZE, y + 1, 0xFF555555);
        graphics.fill(x, y + SKIN_SIZE - 1, x + SKIN_SIZE, y + SKIN_SIZE, 0xFF555555);
        graphics.fill(x, y, x + 1, y + SKIN_SIZE, 0xFF555555);
        graphics.fill(x + SKIN_SIZE - 1, y, x + SKIN_SIZE, y + SKIN_SIZE, 0xFF555555);
        graphics.text(mc.font, "§7?", x + SKIN_SIZE / 2 - 3, y + SKIN_SIZE / 2 - 4, 0xFF888888, false);
    }

    // ===================== ГРАДИЕНТ =====================

    private static int getGradientColor() {
        long time = System.currentTimeMillis();
        float period = GRADIENT_PERIOD * 1000.0f / Math.max(0.1f, ModConfig.targetHudGradientSpeed);
        float t = (time % (long)period) / period;

        int c1 = ModConfig.targetHudColor1;
        int c2 = ModConfig.targetHudColor2;

        int a = (int)(((c1 >> 24) & 0xFF) + (((c2 >> 24) & 0xFF) - ((c1 >> 24) & 0xFF)) * t);
        int r = (int)(((c1 >> 16) & 0xFF) + (((c2 >> 16) & 0xFF) - ((c1 >> 16) & 0xFF)) * t);
        int g = (int)(((c1 >> 8) & 0xFF) + (((c2 >> 8) & 0xFF) - ((c1 >> 8) & 0xFF)) * t);
        int b = (int)((c1 & 0xFF) + ((c2 & 0xFF) - (c1 & 0xFF)) * t);

        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static int getHpColor(float hp, float maxHp) {
        float percent = hp / Math.max(0.1f, maxHp);
        if (percent > 0.75f) return 0xFF00FF00;
        if (percent > 0.50f) return 0xFFFFFF00;
        if (percent > 0.25f) return 0xFFFF8800;
        return 0xFFFF0000;
    }
}