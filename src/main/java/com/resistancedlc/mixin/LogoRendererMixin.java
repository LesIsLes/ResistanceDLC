package com.resistancedlc.mixin;

import com.resistancedlc.config.ModConfig;
import com.resistancedlc.util.MainMenuLayout;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.LogoRenderer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * LogoRendererMixin — заменяем ванильный логотип MINECRAFT на свой.
 * В 26.1 метод renderLogo переименован в extractRenderState.
 */
@Mixin(LogoRenderer.class)
public class LogoRendererMixin {

    private static final int TEX_W = 1280;
    private static final int TEX_H = 348;
    private static final int RENDER_H = MainMenuLayout.LOGO_H;
    private static final int RENDER_W = MainMenuLayout.LOGO_W;

    private static final int PAD_X = 8;
    private static final int PAD_Y = 4;
    private static final int BG_COLOR = 0x80000000;

    // Основной вариант: (GuiGraphicsExtractor, int, float)
    @Inject(method = "extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IF)V",
            at = @At("HEAD"), cancellable = true)
    private void onExtractRenderState3(GuiGraphicsExtractor graphics, int width, float alpha,
                                       CallbackInfo ci) {
        if (!ModConfig.customMainMenuEnabled) return;
        renderCustomLogo(graphics, width, alpha);
        ci.cancel();
    }

    // Вариант с heightOffset: (GuiGraphicsExtractor, int, float, int)
    @Inject(method = "extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IFI)V",
            at = @At("HEAD"), cancellable = true)
    private void onExtractRenderState4(GuiGraphicsExtractor graphics, int width, float alpha, int heightOffset,
                                       CallbackInfo ci) {
        if (!ModConfig.customMainMenuEnabled) return;
        renderCustomLogo(graphics, width, alpha);
        ci.cancel();
    }

    private void renderCustomLogo(GuiGraphicsExtractor graphics, int screenWidth, float alpha) {
        Identifier logo = Identifier.fromNamespaceAndPath(
                "resistancedlc", "textures/gui/title/custom_logo.png"
        );

        int screenHeight = graphics.guiHeight();
        double guiScale = Minecraft.getInstance().getWindow().getGuiScale();
        boolean is4x = guiScale >= 4.0;

        int y = MainMenuLayout.calculateLogoY(screenHeight, is4x);
        int x = (screenWidth - RENDER_W) / 2;

        MainMenuLayout.update(x, y, screenWidth, screenHeight, is4x);

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