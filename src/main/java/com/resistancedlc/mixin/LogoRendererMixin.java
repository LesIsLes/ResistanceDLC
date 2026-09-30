package com.resistancedlc.mixin;

import com.resistancedlc.config.ModConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.LogoRenderer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * LogoRendererMixin — заменяем ванильный логотип MINECRAFT на свой.
 *
 * custom_logo.png: 1280 x 348
 * Целевой размер: ~294 x 80 (по центру, отступ сверху 20 px)
 * Под логотипом — полупрозрачная чёрная подложка.
 */
@Mixin(LogoRenderer.class)
public class LogoRendererMixin {

    // Размеры твоего логотипа (фактические пиксели картинки)
    private static final int TEX_W = 1280;
    private static final int TEX_H = 348;

    // Целевой размер на экране
    private static final int RENDER_H = 80;
    private static final int RENDER_W = (int) ((float) TEX_W / TEX_H * RENDER_H); // ~294

    private static final int LOGO_Y = 20;
    private static final int PAD_X = 8;   // было 12
    private static final int PAD_Y = 4;   // было 6
    private static final int BG_COLOR = 0x80000000;

    // ===== 3-параметный overload =====
    @Inject(method = "renderLogo(Lnet/minecraft/client/gui/GuiGraphics;IF)V",
            at = @At("HEAD"), cancellable = true)
    private void onRenderLogo3(GuiGraphics graphics, int screenWidth, float alpha,
                               CallbackInfo ci) {
        if (!ModConfig.customMainMenuEnabled) return;
        renderCustomLogo(graphics, screenWidth, alpha);
        ci.cancel();
    }

    // ===== 4-параметный overload =====
    @Inject(method = "renderLogo(Lnet/minecraft/client/gui/GuiGraphics;IFI)V",
            at = @At("HEAD"), cancellable = true)
    private void onRenderLogo4(GuiGraphics graphics, int screenWidth, float alpha, int yOffset,
                               CallbackInfo ci) {
        if (!ModConfig.customMainMenuEnabled) return;
        renderCustomLogo(graphics, screenWidth, alpha);
        ci.cancel();
    }

    // ===== Общая логика =====
    private void renderCustomLogo(GuiGraphics graphics, int screenWidth, float alpha) {
        Identifier logo = Identifier.fromNamespaceAndPath(
                "resistancedlc", "textures/gui/title/custom_logo.png"
        );

        int x = (screenWidth - RENDER_W) / 2;
        int y = LOGO_Y;

        // ✅ Правильный fade-in: УМНОЖАЕМ исходную alpha на fade-фактор,
        //    а не заменяем её. BG_COLOR = 0x80000000, alpha-канал = 0x80.
        int originalA = (BG_COLOR >> 24) & 0xFF;   // 128
        int fadedA = (int) (originalA * alpha) & 0xFF;
        int bgColor = (fadedA << 24) | (BG_COLOR & 0x00FFFFFF);

        // Tint для логотипа — то же самое: верхние биты = alpha, нижние = белый
        int tintA = (int) (255 * alpha) & 0xFF;
        int tint = (tintA << 24) | 0xFFFFFF;

        // === Полупрозрачная подложка ===
        graphics.fill(
                x - PAD_X, y - PAD_Y,
                x + RENDER_W + PAD_X, y + RENDER_H + PAD_Y,
                bgColor
        );

        // === Сам логотип ===
        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                logo,
                x, y,
                0f, 0f,
                RENDER_W, RENDER_H,
                TEX_W, TEX_H,
                TEX_W, TEX_H,
                tint
        );
    }
}