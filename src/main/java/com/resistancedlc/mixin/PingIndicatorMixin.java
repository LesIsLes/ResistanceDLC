package com.resistancedlc.mixin;

import com.resistancedlc.PingIndicatorManager;
import com.resistancedlc.config.ModConfig;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.client.multiplayer.PlayerInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * PingIndicatorMixin — заменяет ванильную иконку пинга на свой
 * прогресс-бар + число ms, размещённый под ником.
 *
 * Инжектится в renderPingIcon(HEAD, cancellable).
 * Если фича выключена — ваниль работает как есть.
 */
@Mixin(PlayerTabOverlay.class)
public class PingIndicatorMixin {

    @Shadow private Minecraft minecraft;

    private static final int BAR_WIDTH = 40;
    private static final int BAR_HEIGHT = 2;

    @Inject(method = "renderPingIcon", at = @At("HEAD"), cancellable = true)
    private void onRenderPingIcon(GuiGraphics graphics, int colWidth, int x, int y,
                                  PlayerInfo playerInfo, CallbackInfo ci) {
        if (!ModConfig.pingIndicatorEnabled) return;

        // Всегда отменяем ванильную иконку, когда фича включена
        ci.cancel();

        // Видим ли игрок?
        Boolean visible = PingIndicatorManager.isPlayerVisible(playerInfo);
        if (visible == null || !visible) return;

        // Средний пинг
        int ping = PingIndicatorManager.getAverageLatency(playerInfo.getProfile().id());
        int color = PingIndicatorManager.getPingColor(ping);

        // Позиция: под ником (y + 10)
        int barX = x;
        int barY = y + 10;

        // Прогресс-бар: длина = min(1.0, ping / 300.0) * BAR_WIDTH
        int filled = 0;
        if (ping > 0) {
            float pct = Math.min(1.0f, ping / 300.0f);
            filled = (int) (pct * BAR_WIDTH);
        }

        // Фон полоски (полупрозрачный)
        graphics.fill(barX, barY, barX + BAR_WIDTH, barY + BAR_HEIGHT, 0x60000000);
        // Заливка
        if (filled > 0) {
            graphics.fill(barX, barY, barX + filled, barY + BAR_HEIGHT, color);
        }

        // Текст с ms — справа от полоски
        if (ModConfig.pingIndicatorShowMs) {
            Font font = this.minecraft.font;
            String text = (ping < 0) ? "?" : (ping + "ms");
            int textColor = (ping < 0) ? 0xFF808080 : color;
            graphics.drawString(font, text, barX + BAR_WIDTH + 2, barY - 3, textColor, true);
        }
    }
}