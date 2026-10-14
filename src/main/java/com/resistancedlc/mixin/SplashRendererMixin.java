package com.resistancedlc.mixin;

import com.resistancedlc.config.ModConfig;
import com.resistancedlc.util.MainMenuLayout;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.SplashRenderer;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * SplashRendererMixin — кастомный splash text.
 * В 26.1 render переименован в extractRenderState.
 */
@Mixin(SplashRenderer.class)
public class SplashRendererMixin {

    @Shadow
    private Component splash;

    private static final int SPLASH_Y_LEGACY = 110;
    private static final int PADDING_X = 4;
    private static final int PADDING_Y = 3;
    private static final int BG_COLOR = 0x80000000;

    @Inject(method = "extractRenderState", at = @At("HEAD"), cancellable = true, require = 0)
    private void onRender(GuiGraphicsExtractor graphics, int screenWidth, Font font, float alpha,
                          CallbackInfo ci) {
        if (!ModConfig.customMainMenuEnabled) return;
        if (this.splash == null) return;

        String text = this.splash.getString();
        if (text == null || text.isEmpty()) return;

        ci.cancel();

        int textW = font.width(text);
        int textH = 9;
        int startX, startY;

        double guiScale = Minecraft.getInstance().getWindow().getGuiScale();
        boolean is4x = guiScale >= 4.0;

        if (is4x && MainMenuLayout.is4x()) {
            startX = (screenWidth - textW) / 2;
            startY = MainMenuLayout.getSplashY4x();
        } else {
            startX = (screenWidth - textW) / 2;
            startY = SPLASH_Y_LEGACY;
        }

        int origA = (BG_COLOR >> 24) & 0xFF;
        int fadedA = (int) (origA * alpha) & 0xFF;
        int bgColor = (fadedA << 24) | (BG_COLOR & 0x00FFFFFF);

        graphics.fill(startX - PADDING_X, startY - PADDING_Y, startX + textW + PADDING_X, startY + textH + PADDING_Y, bgColor);

        int len = text.length();
        float time = (System.currentTimeMillis() % 3000L) / 3000.0f;

        for (int i = 0; i < len; i++) {
            float t = ((float) i / len + time) % 1.0f;
            int color = gradientColor(t, alpha);
            String prefix = text.substring(0, i);
            int charX = startX + font.width(prefix);
            String ch = String.valueOf(text.charAt(i));
            graphics.text(font, ch, charX, startY, color, true);
        }
    }

    private static int gradientColor(float t, float alpha) {
        int r, g, b;
        if (t < 0.33f) {
            float k = t / 0.33f;
            r = lerp(0xF4, 0xC2, k); g = lerp(0xA8, 0x28, k); b = lerp(0x54, 0x28, k);
        } else if (t < 0.66f) {
            float k = (t - 0.33f) / 0.33f;
            r = lerp(0xC2, 0xD9, k); g = lerp(0x28, 0x45, k); b = lerp(0x28, 0x8D, k);
        } else {
            float k = (t - 0.66f) / 0.34f;
            r = lerp(0xD9, 0xF4, k); g = lerp(0x45, 0xA8, k); b = lerp(0x8D, 0x54, k);
        }
        int a = (int) (255 * alpha);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static int lerp(int a, int b, float k) {
        return Math.round(a + (b - a) * k);
    }
}