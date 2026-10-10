package com.resistancedlc;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.resistancedlc.config.ModConfig;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import java.util.List;

public class PredictionsRenderer {

    private static final RenderType LINES_TYPE = RenderType.create(
            "resistancedlc_predictions_lines",
            RenderSetup.builder(RenderPipelines.LINES).createRenderSetup()
    );

    /** Дистанция до near-plane камеры, ниже которой сегмент обрезается. */
    private static final double NEAR_PLANE = 0.05;

    public static void render(WorldRenderContext context) {
        if (!ModConfig.predictionsEnabled) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        if (!PredictionsManager.isAiming(mc.player)) return;

        List<PredictionsManager.Trajectory> trajectories =
                PredictionsManager.simulateAllTrajectories(mc.player, ModConfig.predictionsSteps);
        if (trajectories.isEmpty()) return;

        int pColor = (ModConfig.predictionsAlpha << 24)
                | (ModConfig.predictionsColor & 0x00FFFFFF);
        int impactColor = (ModConfig.predictionsAlpha << 24)
                | (ModConfig.predictionsImpactColor & 0x00FFFFFF);

        MultiBufferSource.BufferSource bufferSource =
                (MultiBufferSource.BufferSource) context.consumers();
        if (bufferSource == null) return;

        VertexConsumer consumer = bufferSource.getBuffer(LINES_TYPE);
        PoseStack poseStack = context.matrices();
        Vec3 cameraPos = mc.gameRenderer.getMainCamera().position();

        // ✅ ФИКС: forwardVector() возвращает Vector3fc — конвертим в Vec3
        Vector3fc fwd = mc.gameRenderer.getMainCamera().forwardVector();
        Vec3 look = new Vec3(fwd.x(), fwd.y(), fwd.z());

        for (PredictionsManager.Trajectory traj : trajectories) {
            List<Vec3> points = traj.points;
            for (int i = 1; i < points.size(); i++) {
                Vec3 p1 = points.get(i - 1);
                Vec3 p2 = points.get(i);

                // ✅ Near-plane clipping вместо выкидывания
                double d1 = p1.subtract(cameraPos).dot(look);
                double d2 = p2.subtract(cameraPos).dot(look);

                if (d1 < NEAR_PLANE && d2 < NEAR_PLANE) continue;   // весь сегмент за камерой

                if (d1 < NEAR_PLANE) {
                    // обрезаем p1 до near-plane
                    double t = (NEAR_PLANE - d1) / (d2 - d1);
                    p1 = p1.add(p2.subtract(p1).scale(t));
                }
                if (d2 < NEAR_PLANE) {
                    // обрезаем p2 до near-plane
                    double t = (NEAR_PLANE - d2) / (d1 - d2);
                    p2 = p2.add(p1.subtract(p2).scale(t));
                }

                drawLine(consumer, poseStack, cameraPos, p1, p2,
                        pColor, ModConfig.predictionsThickness);
            }

            // ✅ Маркер точки приземления
            if (ModConfig.predictionsShowImpact && traj.impactPoint != null) {
                drawImpactMarker(consumer, poseStack, cameraPos, look,
                        traj.impactPoint, impactColor);
            }
        }

        bufferSource.endBatch(LINES_TYPE);
    }

    /**
     * Рисует маркер точки приземления.
     * style 0 = крестик (X), style 1 = толстая точка.
     */
    private static void drawImpactMarker(VertexConsumer consumer, PoseStack poseStack,
                                         Vec3 cameraPos, Vec3 look,
                                         Vec3 impact, int color) {
        float size = ModConfig.predictionsImpactSize;
        int style = ModConfig.predictionsImpactStyle;

        if (style == 1) {
            // Точка — очень короткий отрезок с большой толщиной
            Vec3 a = impact.add(look.scale(-0.01));
            Vec3 b = impact.add(look.scale(0.01));
            drawLine(consumer, poseStack, cameraPos, a, b, color, 8.0f);
            return;
        }

        // Крестик — в плоскости, перпендикулярной взгляду
        Vec3 right = look.cross(new Vec3(0, 1, 0));
        if (right.lengthSqr() < 1.0E-6) right = new Vec3(1, 0, 0);
        right = right.normalize();
        Vec3 up = right.cross(look).normalize();

        Vec3 p1a = impact.subtract(right.scale(size));
        Vec3 p1b = impact.add(right.scale(size));
        Vec3 p2a = impact.subtract(up.scale(size));
        Vec3 p2b = impact.add(up.scale(size));

        // ✅ Центр крестика и 2 перпендикулярных линии
        drawLine(consumer, poseStack, cameraPos, p1a, p1b, color, 2.5f);
        drawLine(consumer, poseStack, cameraPos, p2a, p2b, color, 2.5f);
    }

    private static void drawLine(VertexConsumer consumer, PoseStack poseStack,
                                 Vec3 cameraPos, Vec3 p1, Vec3 p2,
                                 int color, float width) {
        float x1 = (float)(p1.x - cameraPos.x);
        float y1 = (float)(p1.y - cameraPos.y);
        float z1 = (float)(p1.z - cameraPos.z);
        float x2 = (float)(p2.x - cameraPos.x);
        float y2 = (float)(p2.y - cameraPos.y);
        float z2 = (float)(p2.z - cameraPos.z);

        Vector3f normal = new Vector3f(x2 - x1, y2 - y1, z2 - z1);
        if (normal.lengthSquared() > 0.000001f) normal.normalize();

        PoseStack.Pose pose = poseStack.last();

        consumer.addVertex(pose, x1, y1, z1)
                .setColor(color)
                .setNormal(pose, normal)
                .setLineWidth(width);

        consumer.addVertex(pose, x2, y2, z2)
                .setColor(color)
                .setNormal(pose, normal)
                .setLineWidth(width);
    }
}