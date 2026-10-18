package com.resistancedlc.hud;

import com.resistancedlc.config.ModConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.entity.LivingEntity;

public class TargetHud {

    public static void render(GuiGraphics graphics) {
        if (!ModConfig.targetHudEnabled) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.options.hideGui) return;

        LivingEntity target = TargetHudManager.getTarget();

        if (target == null && mc.crosshairPickEntity instanceof LivingEntity living) {
            target = living;
        }

        if (target == null || target == mc.player) return;

        int guiW = mc.getWindow().getGuiScaledWidth();
        int guiH = mc.getWindow().getGuiScaledHeight();

        int x = ModConfig.targetHudX < 0 ? (guiW + 182) / 2 + 10 : ModConfig.targetHudX;
        int y = ModConfig.targetHudY < 0 ? guiH - 70 : ModConfig.targetHudY;

        int w = 140;
        int h = 46;

        int bg = (ModConfig.targetHudBgColor & 0x00FFFFFF) | (ModConfig.targetHudAlpha << 24);
        int border = (ModConfig.targetHudBorderColor & 0x00FFFFFF) | (ModConfig.targetHudAlpha << 24);

        graphics.fill(x, y, x + w, y + h, bg);
        graphics.fill(x, y, x + w, y + 1, border);
        graphics.fill(x, y + h - 1, x + w, y + h, border);
        graphics.fill(x, y, x + 1, y + h, border);
        graphics.fill(x + w - 1, y, x + w, y + h, border);

        boolean invisible = target.isInvisible() || target.isInvisibleTo(mc.player);

        if (invisible) {
            // 1. Квадратик с "?" вместо иконки (слева)
            int boxSize = 32;
            int boxX = x + 4;
            int boxY = y + 7;
            graphics.fill(boxX, boxY, boxX + boxSize, boxY + boxSize, 0xFF202020);
            graphics.renderOutline(boxX, boxY, boxSize, boxSize, 0xFF505050);

            String qMark = "?";
            int qWidth = mc.font.width(qMark);
            graphics.drawString(mc.font, qMark, boxX + (boxSize - qWidth) / 2, boxY + (boxSize - 8) / 2, 0xFFAAAAAA, false);
        } else {
            // 1. Обычная иконка моба/игрока
            InventoryScreen.renderEntityInInventoryFollowsMouse(
                    graphics, x + 4, y + 4, x + 36, y + 42, 20, 0.0625f, 0f, 0f, target
            );
        }

        // 2. Имя (сдвинуто вправо, чтобы не налезать на иконку)
        // Используем символ ? для невидимок
        String name = invisible ? "???" : target.getName().getString();
        graphics.drawString(mc.font, "§l" + name, x + 40, y + 6, 0xFFFFFFFF, true);

        // 3. HP и полоса (всегда справа от иконки)
        float hp = target.getHealth();
        float maxHp = target.getMaxHealth();
        float pct = Math.max(0f, Math.min(1f, hp / maxHp));

        // Если здоровье целое, убираем .0
        String hpText = hp == (int) hp ? String.valueOf((int) hp) : String.format("%.1f", hp);

        int hpColor = pct > 0.5f ? 0xFF00FF00 : (pct > 0.25f ? 0xFFFFFF00 : 0xFFFF0000);
        graphics.drawString(mc.font, hpText, x + 40, y + 20, hpColor, true);

        int barW = 90;
        int barH = 4;
        int barX = x + 40; // Начинается сразу после иконки
        int barY = y + 32;

        graphics.fill(barX, barY, barX + barW, barY + barH, 0xFF400000);
        graphics.fill(barX, barY, barX + (int)(barW * pct), barY + barH, hpColor);
    }
}