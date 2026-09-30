package com.resistancedlc;

import com.resistancedlc.config.ModConfig;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * CrosshairHeatmapHud — рендер точек попаданий/промахов в мировых координатах.
 *
 * На каждом кадре проектируем мировую точку метки в экран.
 * За камерой (ndc.z > 1) или дальше crosshairHeatmapRadius блоков — не рисуем.
 */
public final class CrosshairHeatmapHud {

    private CrosshairHeatmapHud() {}

    public static void render(GuiGraphics graphics) {
        if (!ModConfig.crosshairHeatmapEnabled) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) return;
        if (mc.gameRenderer == null) return;

        List<CrosshairHeatmapManager.Mark> marks = CrosshairHeatmapManager.getActiveMarks();
        if (marks.isEmpty()) return;

        int guiW = graphics.guiWidth();
        int guiH = graphics.guiHeight();

        long now = System.currentTimeMillis();
        long lifetime = Math.max(100, ModConfig.crosshairHeatmapMarkLifetime);

        int baseAlpha = Math.max(0, Math.min(255, ModConfig.crosshairHeatmapAlpha));
        int hitBaseColor = ModConfig.crosshairHeatmapColor;
        int hitR = (hitBaseColor >> 16) & 0xFF;
        int hitG = (hitBaseColor >> 8) & 0xFF;
        int hitB = hitBaseColor & 0xFF;

        int size = Math.max(1, ModConfig.crosshairHeatmapMarkSize);

        Vec3 playerPos = mc.player.position();
        double maxDistSq = (double) ModConfig.crosshairHeatmapRadius * ModConfig.crosshairHeatmapRadius;

        for (CrosshairHeatmapManager.Mark m : marks) {
            long age = now - m.timestamp;
            if (age < 0) age = 0;
            if (age > lifetime) continue;

            // Отсеиваем по дистанции до игрока
            double distSq = m.worldPos.distanceToSqr(playerPos);
            if (distSq > maxDistSq) continue;

            // Проекция в экран
            Vec3 ndc;
            try {
                ndc = mc.gameRenderer.projectPointToScreen(m.worldPos);
            } catch (Exception e) {
                continue;
            }
            if (ndc == null) continue;
            if (ndc.z > 1.0) continue;   // за камерой

            int screenX = (int) ((ndc.x + 1.0) * 0.5 * guiW);
            int screenY = (int) ((1.0 - ndc.y) * 0.5 * guiH);

            float t = (float) age / (float) lifetime;
            float fade = 1.0f - t;

            int a = (int) (baseAlpha * fade);
            if (a <= 0) continue;

            int color;
            if (m.hit) {
                color = (a << 24) | (hitR << 16) | (hitG << 8) | hitB;
            } else {
                color = (a << 24) | 0xFFFFFF;
            }

            graphics.fill(screenX - size / 2, screenY - size / 2,
                    screenX - size / 2 + size, screenY - size / 2 + size, color);
        }
    }
}