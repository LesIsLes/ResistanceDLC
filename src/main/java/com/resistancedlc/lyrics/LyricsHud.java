package com.resistancedlc.lyrics;

import com.resistancedlc.MusicPlayerManager;
import com.resistancedlc.config.ModConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.phys.Vec3;

/**
 * LyricsHud — рисует текст в screen-space через проекцию anchor из мира.
 *
 * Коммит 3 (Категория A + B):
 *   - Угол (lyricsAngle) — наклон текста по Z
 *   - Градиент (lyricsGradientEnabled) — посимвольный, БЕЗ изменения альфы
 *   - Entrance, Pulse, Holographic, Outline — как раньше
 */
public class LyricsHud {

    private static final float REFERENCE_DISTANCE = 10.0f;
    private static final float MAX_SCREEN_SCALE = 1.5f;
    private static final float MIN_SCREEN_SCALE = 0.3f;
    private static final float MAX_VISIBLE_DISTANCE = 60.0f;

    private static final float POSITION_SMOOTHING = 0.25f;

    // ===== Entrance =====
    private static final float ENTRANCE_START_SCALE = 0.85f;

    // ===== Сглаженные позиции =====
    private static float smCurrentX = 0, smCurrentY = 0;
    private static boolean smCurrentInit = false;

    private static float smNextX = 0, smNextY = 0;
    private static boolean smNextInit = false;

    public static void render(GuiGraphicsExtractor graphics) {
        if (!ModConfig.lyricsEnabled) return;
        if (!ModConfig.musicPlayerEnabled) return;
        if (!MusicPlayerManager.isPlaying() && !MusicPlayerManager.isPaused()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        LyricsRenderer.tick();

        String currentText = LyricsRenderer.getCurrentText();
        String nextText = LyricsRenderer.getNextText();

        float curAlpha = LyricsRenderer.getCurrentAlpha();
        float nxtAlpha = LyricsRenderer.getNextAlpha();

        int sw = graphics.guiWidth();
        int sh = graphics.guiHeight();

        Vec3 curAnchor = LyricsRenderer.getCurrentAnchorWorld();
        Vec3 nextAnchor = LyricsRenderer.getNextAnchorWorld();

        Vec3 camPos = mc.gameRenderer.getMainCamera().position();
        float baseScale = ModConfig.lyricsFontSize;

        // ===== Current =====
        if (currentText != null && curAnchor != null && curAlpha > 0.001f) {
            int[] screen = projectToScreen(curAnchor, sw, sh, camPos);
            if (screen != null) {
                if (!smCurrentInit) {
                    smCurrentX = screen[0];
                    smCurrentY = screen[1];
                    smCurrentInit = true;
                } else {
                    smCurrentX += (screen[0] - smCurrentX) * POSITION_SMOOTHING;
                    smCurrentY += (screen[1] - smCurrentY) * POSITION_SMOOTHING;
                }

                float dist = (float) camPos.distanceTo(curAnchor);
                float screenScale = computeScreenScale(dist);
                if (screenScale > 0.001f) {
                    int alpha = (int) (ModConfig.lyricsAlpha * curAlpha);
                    int color = (alpha << 24) | (ModConfig.lyricsCurrentColor & 0x00FFFFFF);
                    drawScaled(graphics, mc.font, currentText,
                            Math.round(smCurrentX),
                            Math.round(smCurrentY),
                            color,
                            baseScale * 1.15f * screenScale,
                            true,
                            LyricsRenderer.getCurrentEntranceProgress());
                }
            } else {
                smCurrentInit = false;
            }
        } else {
            smCurrentInit = false;
        }

        // ===== Next =====
        if (nextText != null && nextAnchor != null && nxtAlpha > 0.001f) {
            int[] screen = projectToScreen(nextAnchor, sw, sh, camPos);
            if (screen != null) {
                if (!smNextInit) {
                    smNextX = screen[0];
                    smNextY = screen[1];
                    smNextInit = true;
                } else {
                    smNextX += (screen[0] - smNextX) * POSITION_SMOOTHING;
                    smNextY += (screen[1] - smNextY) * POSITION_SMOOTHING;
                }

                float dist = (float) camPos.distanceTo(nextAnchor);
                float screenScale = computeScreenScale(dist);
                if (screenScale > 0.001f) {
                    int alpha = (int) (ModConfig.lyricsAlpha * nxtAlpha);
                    int color = (alpha << 24) | (ModConfig.lyricsNextColor & 0x00FFFFFF);
                    drawScaled(graphics, mc.font, nextText,
                            Math.round(smNextX),
                            Math.round(smNextY),
                            color,
                            baseScale * 0.9f * screenScale,
                            false,
                            LyricsRenderer.getNextEntranceProgress());
                }
            } else {
                smNextInit = false;
            }
        } else {
            smNextInit = false;
        }
    }

    private static int[] projectToScreen(Vec3 worldPos, int sw, int sh, Vec3 camPos) {
        try {
            Minecraft mc = Minecraft.getInstance();
            var projector = (net.minecraft.world.waypoints.TrackedWaypoint.Projector)
                    mc.gameRenderer;
            Vec3 ndc = projector.projectPointToScreen(worldPos);
            if (ndc == null) return null;
            if (ndc.z > 1.0) return null;

            int x = (int) ((ndc.x + 1.0) * 0.5 * sw);
            int y = (int) ((1.0 - ndc.y) * 0.5 * sh);

            if (x < -200 || x > sw + 200 || y < -100 || y > sh + 100) return null;

            float dist = (float) camPos.distanceTo(worldPos);
            if (dist > MAX_VISIBLE_DISTANCE) return null;

            return new int[]{x, y};
        } catch (Exception e) {
            return null;
        }
    }

    private static float computeScreenScale(float actualDist) {
        if (actualDist <= 0.1f) return MAX_SCREEN_SCALE;
        float scale = REFERENCE_DISTANCE / actualDist;
        if (scale > MAX_SCREEN_SCALE) scale = MAX_SCREEN_SCALE;
        if (scale < MIN_SCREEN_SCALE) scale = MIN_SCREEN_SCALE;
        return scale;
    }

    /**
     * Рисует строку с наклоном, pulse, entrance, обводкой и градиентом.
     */
    private static void drawScaled(GuiGraphicsExtractor graphics, Font font, String text,
                                   int cx, int cy, int color, float scale,
                                   boolean isCurrent,
                                   float entranceProgress) {
        int textW = font.width(text);
        int textH = font.lineHeight;

        graphics.pose().pushMatrix();
        graphics.pose().translate(cx, cy);
// ===== Смещение по X (от центра) =====
        if (ModConfig.lyricsOffsetX != 0.0f) {
            float offsetPixels = ModConfig.lyricsOffsetX * (graphics.guiWidth() / 2.0f) * 0.8f;
            graphics.pose().translate(offsetPixels, 0);
        }
        // ===== Смещение по X (от центра) =====
        if (ModConfig.lyricsOffsetX != 0.0f) {
            float offsetPixels = ModConfig.lyricsOffsetX
                    * (graphics.guiWidth() / 2.0f) * 0.8f;
            graphics.pose().translate(offsetPixels, 0);
        }

        // ===== Entrance-анимация (scale-in с ease-out-back) =====
        if (ModConfig.lyricsEntranceEnabled && entranceProgress < 1.0f) {
            float t = entranceProgress;
            float c1 = 1.70158f;
            float c3 = c1 + 1.0f;
            float easeOutBack = 1.0f + c3 * (float) Math.pow(t - 1, 3)
                    + c1 * (float) Math.pow(t - 1, 2);
            float entranceScale = ENTRANCE_START_SCALE
                    + (1.0f - ENTRANCE_START_SCALE) * easeOutBack;
            scale *= entranceScale;
        }

        // ===== Pulse при смене строки =====
        if (ModConfig.lyricsPulseEnabled) {
            long fadeStart = isCurrent
                    ? LyricsRenderer.getCurrentFadeStart()
                    : LyricsRenderer.getNextFadeStart();
            long sinceChange = System.currentTimeMillis() - fadeStart;
            if (sinceChange >= 0 && sinceChange < 250) {
                float t = sinceChange / 250.0f;
                float pulse = (float) Math.sin(t * Math.PI);
                scale *= 1.0f + ModConfig.lyricsPulseStrength * pulse;
            }
        }

        graphics.pose().scale(scale, scale);

        int x = -textW / 2;
        int y = -textH / 2;

        // ===== Outline (обводка) — 4 направления =====
        if (ModConfig.lyricsOutlineEnabled && ModConfig.lyricsOutlineThickness > 0) {
            int outlineColor = ModConfig.lyricsOutlineColor;
            int t = ModConfig.lyricsOutlineThickness;

            graphics.text(font, text, x - t, y, outlineColor, false);
            graphics.text(font, text, x + t, y, outlineColor, false);
            graphics.text(font, text, x, y - t, outlineColor, false);
            graphics.text(font, text, x, y + t, outlineColor, false);

            if (t > 1) {
                graphics.text(font, text, x - t, y - t, outlineColor, false);
                graphics.text(font, text, x + t, y - t, outlineColor, false);
                graphics.text(font, text, x - t, y + t, outlineColor, false);
                graphics.text(font, text, x + t, y + t, outlineColor, false);
            }
        }

        // ===== Основной текст с градиентом =====
        if (ModConfig.lyricsGradientEnabled && text.length() > 1) {
            drawGradientText(graphics, font, text, x, y, color, isCurrent);
        } else {
            graphics.text(font, text, x, y, color, true);
        }

        graphics.pose().popMatrix();
    }

    /**
     * Градиент по яркости (RGB), альфа фиксирована.
     * Current: слева ярче → справа темнее.
     * Next:    слева темнее → справа ярче.
     */
    private static void drawGradientText(GuiGraphicsExtractor graphics, Font font,
                                         String text, int x, int y, int color,
                                         boolean isCurrent) {
        int total = text.length();
        int curX = x;
        int baseA = (color >>> 24) & 0xFF;
        int baseR = (color >> 16) & 0xFF;
        int baseG = (color >> 8) & 0xFF;
        int baseB = color & 0xFF;

        // Клэмп силы градиента [0.0, 0.7]
        float strength = Math.max(0.0f, Math.min(0.7f, ModConfig.lyricsGradientStrength));

        for (int i = 0; i < total; i++) {
            float t = (float) i / Math.max(1, total - 1);
            float factor = isCurrent
                    ? (1.0f - strength * t)
                    : (1.0f - strength + strength * t);
            factor = Math.max(0.3f, Math.min(1.0f, factor));

            int r = Math.min(255, (int) (baseR * factor));
            int g = Math.min(255, (int) (baseG * factor));
            int b = Math.min(255, (int) (baseB * factor));

            // АЛЬФА ФИКСИРОВАНА — иначе Minecraft рисует чёрные блоки
            int chColor = (baseA << 24) | (r << 16) | (g << 8) | b;
            String ch = String.valueOf(text.charAt(i));
            graphics.text(font, ch, curX, y, chColor, true);
            curX += font.width(ch);
        }
    }

    public static void reset() {
        smCurrentInit = false;
        smNextInit = false;
    }
}