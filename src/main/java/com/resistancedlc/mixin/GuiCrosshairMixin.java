package com.resistancedlc.mixin;

import com.resistancedlc.config.ModConfig;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * GuiCrosshairMixin — кастомный прицел в 26.2.
 * В 26.2 прицел рисуется в Hud.extractCrosshair, а не в Gui.
 */
@Mixin(Hud.class)
public class GuiCrosshairMixin {

    @Inject(method = "extractCrosshair", at = @At("HEAD"), cancellable = true)
    private void onExtractCrosshair(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        if (!ModConfig.crosshairEnabled) return;  // ванильный прицел

        // Отменяем ванильный прицел — рисуем свой
        ci.cancel();

        // ===== Свой прицел =====
        int centerX = graphics.guiWidth() / 2;
        int centerY = graphics.guiHeight() / 2;

        int chColor = (ModConfig.crosshairColor & 0x00FFFFFF) | (ModConfig.crosshairAlpha << 24);
        int size = ModConfig.crosshairSize;
        int thick = ModConfig.crosshairThickness;
        int gap = ModConfig.crosshairGap;
        int shape = ModConfig.crosshairShape;

        switch (shape) {
            case 0 -> {
                // Cross
                graphics.fill(centerX - thick / 2, centerY - gap - size,
                        centerX + (thick + 1) / 2, centerY - gap, chColor);
                graphics.fill(centerX - thick / 2, centerY + gap,
                        centerX + (thick + 1) / 2, centerY + gap + size, chColor);
                graphics.fill(centerX - gap - size, centerY - thick / 2,
                        centerX - gap, centerY + (thick + 1) / 2, chColor);
                graphics.fill(centerX + gap, centerY - thick / 2,
                        centerX + gap + size, centerY + (thick + 1) / 2, chColor);
            }
            case 1 -> {
                // Dot
                int dotSize = Math.max(1, thick);
                int half = dotSize / 2;
                graphics.fill(centerX - half, centerY - half,
                        centerX - half + dotSize, centerY - half + dotSize, chColor);
            }
            case 2 -> {
                // Circle
                int radius = Math.max(2, size);
                int points = 24;
                for (int i = 0; i < points; i++) {
                    double angle = 2 * Math.PI * i / points;
                    int px = centerX + (int) Math.round(Math.cos(angle) * radius);
                    int py = centerY + (int) Math.round(Math.sin(angle) * radius);
                    int pt = Math.max(1, thick);
                    graphics.fill(px - pt / 2, py - pt / 2,
                            px - pt / 2 + pt, py - pt / 2 + pt, chColor);
                }
            }
            case 3 -> {
                // Arrows
                drawTriangle(graphics, centerX, centerY - gap - size, size, thick, 0, chColor);
                drawTriangle(graphics, centerX, centerY + gap + size, size, thick, 1, chColor);
                drawTriangle(graphics, centerX - gap - size, centerY, size, thick, 2, chColor);
                drawTriangle(graphics, centerX + gap + size, centerY, size, thick, 3, chColor);
            }
            case 4 -> {
                // Cross + Dot
                graphics.fill(centerX - thick / 2, centerY - gap - size,
                        centerX + (thick + 1) / 2, centerY - gap, chColor);
                graphics.fill(centerX - thick / 2, centerY + gap,
                        centerX + (thick + 1) / 2, centerY + gap + size, chColor);
                graphics.fill(centerX - gap - size, centerY - thick / 2,
                        centerX - gap, centerY + (thick + 1) / 2, chColor);
                graphics.fill(centerX + gap, centerY - thick / 2,
                        centerX + gap + size, centerY + (thick + 1) / 2, chColor);
                int dotSize = Math.max(1, thick);
                int half = dotSize / 2;
                graphics.fill(centerX - half, centerY - half,
                        centerX - half + dotSize, centerY - half + dotSize, chColor);
            }
        }
    }

    private static void drawTriangle(GuiGraphicsExtractor graphics, int tipX, int tipY,
                                     int size, int thick, int direction, int color) {
        int baseSize = Math.max(2, thick * 2);
        for (int i = 0; i < size; i++) {
            int width = (int) Math.round((double) baseSize * i / size);
            if (width < 1) width = 1;
            switch (direction) {
                case 0 -> { int y = tipY + i; graphics.fill(tipX - width / 2, y, tipX - width / 2 + width, y + 1, color); }
                case 1 -> { int y = tipY - i; graphics.fill(tipX - width / 2, y, tipX - width / 2 + width, y + 1, color); }
                case 2 -> { int x = tipX + i; graphics.fill(x, tipY - width / 2, x + 1, tipY - width / 2 + width, color); }
                case 3 -> { int x = tipX - i; graphics.fill(x, tipY - width / 2, x + 1, tipY - width / 2 + width, color); }
            }
        }
    }
}