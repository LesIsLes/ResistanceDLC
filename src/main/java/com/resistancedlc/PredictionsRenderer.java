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

import java.util.List;

public class PredictionsRenderer {

    private static final RenderType LINES_TYPE = RenderType.create(
            "resistancedlc_predictions_lines",
            RenderSetup.builder(RenderPipelines.LINES).createRenderSetup()
    );

    public static void render(WorldRenderContext context) {
        if (!ModConfig.predictionsEnabled) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        if (!PredictionsManager.isAiming(mc.player)) return;

        List<List<Vec3>> trajectories = PredictionsManager.simulateAllTrajectories(
                mc.player, ModConfig.predictionsSteps);
        if (trajectories.isEmpty()) return;

        int pColor = (ModConfig.predictionsAlpha << 24)
                | (ModConfig.predictionsColor & 0x00FFFFFF);

        MultiBufferSource.BufferSource bufferSource =
                (MultiBufferSource.BufferSource) context.consumers();
        if (bufferSource == null) return;

        VertexConsumer consumer = bufferSource.getBuffer(LINES_TYPE);
        PoseStack poseStack = context.matrices();
        Vec3 cameraPos = mc.gameRenderer.getMainCamera().position();

        for (List<Vec3> points : trajectories) {
            for (int i = 1; i < points.size(); i++) {
                // ✅ ФИКС: пропускаем сегменты, которые
                // проходят через камеру (слишком близко)
                Vec3 p1 = points.get(i - 1);
                Vec3 p2 = points.get(i);

                if (p1.distanceToSqr(cameraPos) < 0.25) continue;
                if (p2.distanceToSqr(cameraPos) < 0.25) continue;

                drawLine(consumer, poseStack, cameraPos, p1, p2, pColor);
            }
        }

        bufferSource.endBatch(LINES_TYPE);
    }

    private static void drawLine(VertexConsumer consumer, PoseStack poseStack,
                                 Vec3 cameraPos, Vec3 p1, Vec3 p2, int color) {
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
                .setLineWidth(ModConfig.predictionsThickness);

        consumer.addVertex(pose, x2, y2, z2)
                .setColor(color)
                .setNormal(pose, normal)
                .setLineWidth(ModConfig.predictionsThickness);
    }
}