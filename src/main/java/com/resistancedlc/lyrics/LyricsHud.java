package com.resistancedlc.lyrics;

import com.resistancedlc.MusicPlayerManager;
import com.resistancedlc.config.ModConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.phys.Vec3;

/**
 * LyricsHud — рисует текст в screen-space через проекцию anchor из мира.
 *
 * Дрожание устранено двумя приёмами:
 *   1) Сглаживание screen-координат (lerp к целевому значению).
 *   2) Округление до целых пикселей.
 */
public class LyricsHud {

    private static final float REFERENCE_DISTANCE = 10.0f;
    private static final float MAX_SCREEN_SCALE = 3.0f;
    private static final float MIN_SCREEN_SCALE = 0.2f;
    private static final float MAX_VISIBLE_DISTANCE = 40.0f;

    /** Насколько быстро экранная позиция догоняет целевую (0..1). */
    private static final float POSITION_SMOOTHING = 0.25f;

    // ===== Сглаженные позиции =====
    private static float smCurrentX = 0, smCurrentY = 0;
    private static boolean smCurrentInit = false;

    private static float smNextX = 0, smNextY = 0;
    private static boolean smNextInit = false;

    public static void render(GuiGraphics graphics) {
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
                // Сглаживание
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
                    drawScaled(graphics, mc.font, "§l" + currentText,
                            Math.round(smCurrentX),
                            Math.round(smCurrentY),
                            color,
                            baseScale * 1.15f * screenScale);
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
                            baseScale * 0.9f * screenScale);
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

    private static void drawScaled(GuiGraphics graphics, Font font, String text,
                                   int cx, int cy, int color, float scale) {
        int textW = font.width(text);
        int textH = font.lineHeight;

        graphics.pose().pushMatrix();
        graphics.pose().translate(cx, cy);
        graphics.pose().scale(scale, scale);

        int x = -textW / 2;
        int y = -textH / 2;
        graphics.drawString(font, text, x, y, color, true);

        graphics.pose().popMatrix();
    }

    public static void reset() {
        smCurrentInit = false;
        smNextInit = false;
    }
}