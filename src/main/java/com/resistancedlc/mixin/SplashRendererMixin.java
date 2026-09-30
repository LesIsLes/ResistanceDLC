package com.resistancedlc.mixin;

import com.resistancedlc.config.ModConfig;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.SplashRenderer;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * SplashRendererMixin — кастомный splash text.
 *
 * Ванильный splash:
 *   - Наклон -20°, пульсирующая анимация, жёлтый цвет, позиция centerX+123 y=69
 *
 * Наш splash (когда customMainMenuEnabled):
 *   - Прямой, статичный, по центру, под логотипом
 *   - Анимированный 3-цветный градиент (F4A854 → C22828 → D9458D → F4A854)
 *   - Чёрная полупрозрачная подложка
 */
@Mixin(SplashRenderer.class)
public class SplashRendererMixin {

    @Shadow
    private Component splash;

    // ===== Настройки позиции =====
    private static final int SPLASH_Y = 110;   // было 118
    private static final int PADDING_X = 4;    // было 6
    private static final int PADDING_Y = 3;
    private static final int BG_COLOR = 0x80000000;

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void onRender(GuiGraphics graphics, int screenWidth, Font font, float alpha,
                          CallbackInfo ci) {
        if (!ModConfig.customMainMenuEnabled) return;   // ванильный, если выключено

        if (this.splash == null) return;
        String text = this.splash.getString();
        if (text == null || text.isEmpty()) return;

        // Глушим ванильный render
        ci.cancel();

        int textW = font.width(text);
        int textH = 9;   // высота строки шрифта

        // По центру экрана
        int startX = (screenWidth - textW) / 2;
        int startY = SPLASH_Y;

        // ===== 1. Подложка — с учётом fade-in alpha (УМНОЖАЕМ, не заменяем) =====
        int origA = (BG_COLOR >> 24) & 0xFF;   // 0x80 = 128
        int fadedA = (int) (origA * alpha) & 0xFF;
        int bgColor = (fadedA << 24) | (BG_COLOR & 0x00FFFFFF);

        graphics.fill(
                startX - PADDING_X,
                startY - PADDING_Y,
                startX + textW + PADDING_X,
                startY + textH + PADDING_Y,
                bgColor
        );

        // ===== 2. Анимированный градиентный текст (посимвольно) =====
        int len = text.length();
        // Время для анимации: 0..1 каждые 3000 мс
        float time = (System.currentTimeMillis() % 3000L) / 3000.0f;

        for (int i = 0; i < len; i++) {
            // Градиент двигается: t = (i/len + time) % 1
            float t = ((float) i / len + time) % 1.0f;
            int color = gradientColor(t, alpha);

            String prefix = text.substring(0, i);
            int charX = startX + font.width(prefix);

            String ch = String.valueOf(text.charAt(i));
            graphics.drawString(font, ch, charX, startY, color, true);
        }
    }

    /**
     * Градиент по t ∈ [0, 1] с циклом через 3 цвета:
     *   t ∈ [0, 0.33)  → F4A854 → C22828
     *   t ∈ [0.33, 0.66) → C22828 → D9458D
     *   t ∈ [0.66, 1]  → D9458D → F4A854
     */
    private static int gradientColor(float t, float alpha) {
        int r, g, b;
        if (t < 0.33f) {
            float k = t / 0.33f;
            r = lerp(0xF4, 0xC2, k);
            g = lerp(0xA8, 0x28, k);
            b = lerp(0x54, 0x28, k);
        } else if (t < 0.66f) {
            float k = (t - 0.33f) / 0.33f;
            r = lerp(0xC2, 0xD9, k);
            g = lerp(0x28, 0x45, k);
            b = lerp(0x28, 0x8D, k);
        } else {
            float k = (t - 0.66f) / 0.34f;
            r = lerp(0xD9, 0xF4, k);
            g = lerp(0x45, 0xA8, k);
            b = lerp(0x8D, 0x54, k);
        }
        int a = (int) (255 * alpha);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static int lerp(int a, int b, float k) {
        return Math.round(a + (b - a) * k);
    }
}