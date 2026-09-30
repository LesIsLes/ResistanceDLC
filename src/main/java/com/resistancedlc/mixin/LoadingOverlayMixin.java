package com.resistancedlc.mixin;

import com.resistancedlc.config.ModConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.LoadingOverlay;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ReloadInstance;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * LoadingOverlayMixin — полностью заменяем render().
 *
 * Не используем ванильный fadeOutStart/fadeInStart — ведём свои таймеры.
 * Отслеживаем reload.isDone() через @Shadow.
 */
@Mixin(LoadingOverlay.class)
public class LoadingOverlayMixin {

    @Shadow private ReloadInstance reload;

    @Unique private long resistancedlc$firstRenderMs = -1L;
    @Unique private long resistancedlc$fadeOutStartMs = -1L;
    @Unique private boolean resistancedlc$overlayCleared = false;

    private static final long FADE_OUT_DURATION_MS = 1000L;

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void onRender(GuiGraphics graphics, int mouseX, int mouseY,
                          float delta, CallbackInfo ci) {
        if (!ModConfig.customLoadingScreenEnabled) return;

        // Глушим ванильный render — Mojang splash не рисуется
        ci.cancel();

        long now = Util.getMillis();
        if (resistancedlc$firstRenderMs < 0L) {
            resistancedlc$firstRenderMs = now;
        }

        int alpha = 255;

        // === Проверяем, закончилась ли загрузка ===
        boolean reloadDone = false;
        try {
            reloadDone = this.reload != null && this.reload.isDone();
        } catch (Throwable ignored) {}

        if (reloadDone) {
            if (resistancedlc$fadeOutStartMs < 0L) {
                resistancedlc$fadeOutStartMs = now;   // старт fade-out
            }
            float t = (float)(now - resistancedlc$fadeOutStartMs) / (float) FADE_OUT_DURATION_MS;
            t = Mth.clamp(t, 0.0f, 1.0f);
            alpha = (int)(255 * (1.0f - t));

            // Fade-out завершён — убираем overlay
            if (t >= 1.0f && !resistancedlc$overlayCleared) {
                resistancedlc$overlayCleared = true;
                Minecraft.getInstance().execute(() -> Minecraft.getInstance().setOverlay(null));
                return;
            }
        }

        if (alpha <= 0) return;

        int screenW = graphics.guiWidth();
        int screenH = graphics.guiHeight();

        // 1. Чёрный фон
        graphics.fill(0, 0, screenW, screenH, 0xFF000000);

        // 2. Наш фон
        Identifier bg = Identifier.fromNamespaceAndPath(
                "resistancedlc", "textures/gui/title/loading_background.png"
        );
        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                bg,
                0, 0,
                0f, 0f,
                screenW, screenH,
                screenW, screenH,
                (alpha << 24) | 0xFFFFFF
        );

        // 3. Прогресс-бар — считаем свой прогресс
        int barY = (int)(screenH * 0.8325);
        int barLeft = screenW / 2 - 100;
        int barRight = screenW / 2 + 100;
        int barTop = barY - 5;
        int barBottom = barY + 5;

        float progress = 0f;
        try {
            progress = this.reload != null ? this.reload.getActualProgress() : 0f;
        } catch (Throwable ignored) {}
        progress = Mth.clamp(progress, 0f, 1f);

        int filled = (int)((barRight - barLeft - 2) * progress);
        int barColor = (alpha << 24) | 0xFFFFFFFF;

        graphics.fill(barLeft + 2, barTop + 2, barLeft + 2 + filled, barBottom - 2, barColor);
        graphics.fill(barLeft + 1, barTop, barRight - 1, barTop + 1, barColor);
        graphics.fill(barLeft + 1, barBottom - 1, barRight - 1, barBottom, barColor);
        graphics.fill(barLeft, barTop, barLeft + 1, barBottom, barColor);
        graphics.fill(barRight - 1, barTop, barRight, barBottom, barColor);
    }
}