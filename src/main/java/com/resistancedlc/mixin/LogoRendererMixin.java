package com.resistancedlc.mixin;

import com.resistancedlc.config.ModConfig;
import com.resistancedlc.util.MainMenuLayout;
import net.minecraft.client.Minecraft;
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
 * Целевой размер: ~294 x 80
 *
 * v3.0.0-alpha.4 — адаптив под GUI Scale:
 *   - 1x–3x — лого на y = 20 (как раньше)
 *   - 4x    — лого уходит вверх, к TOP_MARGIN_4X
 */
@Mixin(LogoRenderer.class)
public class LogoRendererMixin {

    private static final int TEX_W = 1280;
    private static final int TEX_H = 348;
    private static final int RENDER_H = MainMenuLayout.LOGO_H;   // 80
    private static final int RENDER_W = MainMenuLayout.LOGO_W;   // 294

    private static final int PAD_X = 8;
    private static final int PAD_Y = 4;
    private static final int BG_COLOR = 0x80000000;

    @Inject(method = "renderLogo(Lnet/minecraft/client/gui/GuiGraphics;IF)V",
            at = @At("HEAD"), cancellable = true)
    private void onRenderLogo3(GuiGraphics graphics, int screenWidth, float alpha,
                               CallbackInfo ci) {
        if (!ModConfig.customMainMenuEnabled) return;
        renderCustomLogo(graphics, screenWidth, alpha);
        ci.cancel();
    }

    @Inject(method = "renderLogo(Lnet/minecraft/client/gui/GuiGraphics;IFI)V",
            at = @At("HEAD"), cancellable = true)
    private void onRenderLogo4(GuiGraphics graphics, int screenWidth, float alpha, int yOffset,
                               CallbackInfo ci) {
        if (!ModConfig.customMainMenuEnabled) return;
        renderCustomLogo(graphics, screenWidth, alpha);
        ci.cancel();
    }

    private void renderCustomLogo(GuiGraphics graphics, int screenWidth, float alpha) {
        Identifier logo = Identifier.fromNamespaceAndPath(
                "resistancedlc", "textures/gui/title/custom_logo.png"
        );

        int screenHeight = graphics.guiHeight();
        double guiScale = Minecraft.getInstance().getWindow().getGuiScale();
        boolean is4x = guiScale >= 4.0;

        int y = MainMenuLayout.calculateLogoY(screenHeight, is4x);
        int x = (screenWidth - RENDER_W) / 2;

        // Обновляем общий layout, чтобы SplashRendererMixin знал позицию
        MainMenuLayout.update(x, y, screenWidth, screenHeight, is4x);

        // Fade-in
        int originalA = (BG_COLOR >> 24) & 0xFF;
        int fadedA = (int) (originalA * alpha) & 0xFF;
        int bgColor = (fadedA << 24) | (BG_COLOR & 0x00FFFFFF);

        int tintA = (int) (255 * alpha) & 0xFF;
        int tint = (tintA << 24) | 0xFFFFFF;

        graphics.fill(
                x - PAD_X, y - PAD_Y,
                x + RENDER_W + PAD_X, y + RENDER_H + PAD_Y,
                bgColor
        );

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