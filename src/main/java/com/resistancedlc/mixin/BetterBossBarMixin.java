package com.resistancedlc.mixin;

import com.resistancedlc.config.BossBarStyle;
import com.resistancedlc.config.ModConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.BossHealthOverlay;
import net.minecraft.client.gui.components.LerpingBossEvent;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;
import java.util.UUID;

@Mixin(BossHealthOverlay.class)
public class BetterBossBarMixin {

    @Shadow @Final private Minecraft minecraft;

    // БЕЗ модификатора доступа — как в ванили (package-private)
    @Shadow @Final
    Map<UUID, LerpingBossEvent> events;

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void onRender(GuiGraphics graphics, CallbackInfo ci) {
        if (!ModConfig.betterBossBarEnabled) return;
        if (events.isEmpty()) return;

        ci.cancel();
        renderCustomBossBars(graphics);
    }

    private void renderCustomBossBars(GuiGraphics graphics) {
        int screenWidth = graphics.guiWidth();
        int y = 12;

        for (LerpingBossEvent event : events.values()) {
            renderBar(graphics, screenWidth, y, event);
            y += 19;

            if (y >= graphics.guiHeight() / 3) break;
        }
    }

    private void renderBar(GuiGraphics graphics, int screenWidth, int y, LerpingBossEvent event) {
        int barWidth = 182;
        int barHeight = 5;
        int x = screenWidth / 2 - barWidth / 2;

        // Название
        Component name = event.getName();
        int nameWidth = minecraft.font.width(name);
        int nameX = screenWidth / 2 - nameWidth / 2;
        int nameY = y - 9;
        graphics.drawString(minecraft.font, name, nameX, nameY, ModConfig.betterBossBarTextColor);

        // Прогресс
        int filledWidth = (int) (barWidth * event.getProgress());

        if (ModConfig.betterBossBarStyle == BossBarStyle.CLASSIC) {
            graphics.fill(x - 1, y - 1, x + barWidth + 1, y + barHeight + 1, ModConfig.betterBossBarBgColor);
            graphics.fillGradient(x, y, x + barWidth, y + barHeight,
                    ModConfig.betterBossBarEmptyColor, ModConfig.betterBossBarEmptyColor);
            if (filledWidth > 0) {
                graphics.fillGradient(x, y, x + filledWidth, y + barHeight,
                        ModConfig.betterBossBarFillColor1, ModConfig.betterBossBarFillColor2);
            }
        } else {
            graphics.fill(x, y, x + barWidth, y + barHeight, ModConfig.betterBossBarBgColor);
            if (filledWidth > 0) {
                graphics.fill(x, y, x + filledWidth, y + barHeight, ModConfig.betterBossBarFillColor1);
            }
        }
    }
}