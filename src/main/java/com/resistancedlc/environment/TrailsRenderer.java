package com.resistancedlc.environment;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.resistancedlc.config.ModConfig;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.world.phys.Vec3;

public final class TrailsRenderer {

    private static final RenderType TRAILS_TYPE = RenderType.create(
            "resistancedlc_trails",
            RenderSetup.builder(RenderPipelines.DEBUG_QUADS).createRenderSetup()
    );

    private TrailsRenderer() {}

    public static void render(LevelRenderContext context) {
        if (!ModConfig.trailsEnabled) return;

        var points = TrailsManager.getPoints();
        if (points.size() < 2) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        MultiBufferSource.BufferSource bufferSource =
                (MultiBufferSource.BufferSource) context.bufferSource();
        if (bufferSource == null) return;

        Vec3 cameraPos = mc.gameRenderer.getMainCamera().position();
        PoseStack poseStack = context.poseStack();
        PoseStack.Pose pose = poseStack.last();

        VertexConsumer consumer = bufferSource.getBuffer(TRAILS_TYPE);

        long now = System.currentTimeMillis();
        long lifetimeMs = (long)(ModConfig.trailsLength * 1000.0f);

        // Цвета
        int c1 = ModConfig.trailsColor1;
        int c2 = ModConfig.trailsColor2;
        float baseAlpha = ModConfig.trailsAlpha;

        float width = ModConfig.trailsWidth;

        // Преобразуем deque в массив для удобства
        var arr = points.toArray(new TrailsManager.TrailPoint[0]);

        for (int i = 0; i < arr.length - 1; i++) {
            TrailsManager.TrailPoint p1 = arr[i];
            TrailsManager.TrailPoint p2 = arr[i + 1];

            // Прогресс от 0 (старое) до 1 (новое)
            float t = (float) i / (arr.length - 1);

            // Fade-out по времени
            long ageMs = now - p1.time;
            float fade = 1.0f - Math.min(1.0f, (float) ageMs / lifetimeMs);
            if (fade <= 0.0f) continue;

            // Цвет с градиентом
            int color = ModConfig.trailsGradient
                    ? lerpColor(c1, c2, t)
                    : c1;

            float alpha = baseAlpha * fade;
            int a = (int)(255 * alpha);
            int r = (color >> 16) & 0xFF;
            int g = (color >> 8) & 0xFF;
            int b = color & 0xFF;
            int argb = (a << 24) | (r << 16) | (g << 8) | b;

            // Вычисляем нормаль для ширины trail'а — перпендикуляр к направлению
            double dx = p2.x - p1.x;
            double dz = p2.z - p1.z;
            double len = Math.sqrt(dx * dx + dz * dz);
            if (len < 0.001) continue;
            double nx = -dz / len * width * 0.5;
            double nz = dx / len * width * 0.5;

            // 4 вершины квада (относительно камеры)
            float x1a = (float)(p1.x - cameraPos.x + nx);
            float z1a = (float)(p1.z - cameraPos.z + nz);
            float x1b = (float)(p1.x - cameraPos.x - nx);
            float z1b = (float)(p1.z - cameraPos.z - nz);

            float x2a = (float)(p2.x - cameraPos.x + nx);
            float z2a = (float)(p2.z - cameraPos.z + nz);
            float x2b = (float)(p2.x - cameraPos.x - nx);
            float z2b = (float)(p2.z - cameraPos.z - nz);

            float y1 = (float)(p1.y - cameraPos.y);
            float y2 = (float)(p2.y - cameraPos.y);

            // Квад: p1a → p1b → p2b → p2a
            consumer.addVertex(pose, x1a, y1, z1a).setColor(argb);
            consumer.addVertex(pose, x1b, y1, z1b).setColor(argb);
            consumer.addVertex(pose, x2b, y2, z2b).setColor(argb);
            consumer.addVertex(pose, x2a, y2, z2a).setColor(argb);
        }

        bufferSource.endBatch(TRAILS_TYPE);
    }

    private static int lerpColor(int c1, int c2, float t) {
        int r1 = (c1 >> 16) & 0xFF, g1 = (c1 >> 8) & 0xFF, b1 = c1 & 0xFF;
        int r2 = (c2 >> 16) & 0xFF, g2 = (c2 >> 8) & 0xFF, b2 = c2 & 0xFF;
        int r = (int)(r1 + (r2 - r1) * t);
        int g = (int)(g1 + (g2 - g1) * t);
        int b = (int)(b1 + (b2 - b1) * t);
        return (r << 16) | (g << 8) | b;
    }
}