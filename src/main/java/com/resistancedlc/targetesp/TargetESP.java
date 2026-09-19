package com.resistancedlc.targetesp;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.resistancedlc.config.ModConfig;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldExtractionContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public final class TargetESP {

    private final TargetManager targetManager;

    private static final RenderType LINES_TYPE = RenderType.create(
            "resistancedlc_targetesp_lines",
            RenderSetup.builder(RenderPipelines.LINES).createRenderSetup()
    );

    private double smoothX = 0, smoothY = 0, smoothZ = 0;
    private boolean smoothInit = false;
    private long lastTargetSwitch = 0L;

    public TargetESP(TargetManager targetManager) {
        this.targetManager = targetManager;
    }

    public void resetTransientState() {
        smoothInit = false;
    }

    public void extract(WorldExtractionContext context) {}

    public void draw(WorldRenderContext context) {
        if (!ModConfig.targetEspEnabled) return;

        LivingEntity target = targetManager.getLivingTarget();
        if (target == null) {
            smoothInit = false;
            return;
        }

        int baseColor = ModConfig.targetEspColor;
        int r = (baseColor >> 16) & 0xFF;
        int g = (baseColor >> 8) & 0xFF;
        int b = baseColor & 0xFF;
        int argb = (0xFF << 24) | (r << 16) | (g << 8) | b;

        Variant variant = Variant.fromId(ModConfig.targetEspVariant);

        double rawX = target.getX();
        double rawY = target.getY();
        double rawZ = target.getZ();

        long switchTime = targetManager.getTargetSwitchTime();
        if (switchTime != lastTargetSwitch) {
            smoothInit = false;
            lastTargetSwitch = switchTime;
        }

        if (!smoothInit) {
            smoothX = rawX; smoothY = rawY; smoothZ = rawZ;
            smoothInit = true;
        } else {
            double k = 0.25;
            smoothX += (rawX - smoothX) * k;
            smoothY += (rawY - smoothY) * k;
            smoothZ += (rawZ - smoothZ) * k;
        }

        double halfWidth = target.getBbWidth() / 2.0;
        double height = target.getBbHeight();

        AABB box = new AABB(
                smoothX - halfWidth, smoothY, smoothZ - halfWidth,
                smoothX + halfWidth, smoothY + height, smoothZ + halfWidth
        );

        MultiBufferSource.BufferSource bufferSource =
                (MultiBufferSource.BufferSource) context.consumers();
        if (bufferSource == null) return;

        VertexConsumer consumer = bufferSource.getBuffer(LINES_TYPE);
        PoseStack poseStack = context.matrices();
        Vec3 cameraPos = Minecraft.getInstance()
                .gameRenderer
                .getMainCamera()
                .position();

        switch (variant) {
            case CRYSTALS -> drawCrystals(box, argb, consumer, poseStack, cameraPos);
            case RING -> drawRing(box, argb, consumer, poseStack, cameraPos);
            case CUBES -> drawCubes(box, argb, consumer, poseStack, cameraPos);
            case GHOSTS -> drawGhosts(box, argb, consumer, poseStack, cameraPos);
            default -> {}
        }

        bufferSource.endBatch(LINES_TYPE);
    }

    private void drawCrystals(AABB box, int color, VertexConsumer consumer,
                              PoseStack poseStack, Vec3 cameraPos) {
        double cx = (box.minX + box.maxX) / 2;
        double cz = (box.minZ + box.maxZ) / 2;
        double radius = (box.maxX - box.minX) / 2 + 0.8;

        double height = box.maxY - box.minY;
        double baseY = box.minY;

        long time = System.currentTimeMillis();
        double timeSec = time / 1000.0;

        int count = 12;
        double totalTurns = 2.5;

        float sizeMul = ModConfig.targetEspSize;
        float rotationMul = ModConfig.targetEspRotationSpeed;
        float pulseAmt = ModConfig.targetEspPulse;
        double pulseGlobal = 1.0 + Math.sin(timeSec * 3.0) * pulseAmt;

        double spiralRotation = timeSec * rotationMul * 0.8;

        for (int i = 0; i < count; i++) {
            double t = (double) i / (count - 1);

            double angle = spiralRotation + t * totalTurns * Math.PI * 2;
            double py = baseY + t * height;
            double r = radius * (1.0 + Math.sin(t * Math.PI * 2) * 0.15);

            double px = cx + Math.cos(angle) * r;
            double pz = cz + Math.sin(angle) * r;

            double pulseLocal = 1.0 + Math.sin(timeSec * 4.0 + i * 1.2) * pulseAmt;
            double size = 0.12 * sizeMul * pulseGlobal * pulseLocal;

            double spin = timeSec * 40.0 + i * 47.0;

            drawOctahedron(px, py, pz, size, spin, color, consumer, poseStack, cameraPos);
        }
    }

    private void drawOctahedron(double x, double y, double z, double size,
                                double spin, int color, VertexConsumer consumer,
                                PoseStack poseStack, Vec3 cameraPos) {
        double[][] vertices = new double[][] {
                { 0, -size * 1.5, 0 },
                { 0,  size * 1.5, 0 },
                { size, 0, 0 },
                { -size, 0, 0 },
                { 0, 0, size },
                { 0, 0, -size },
        };

        double rad = Math.toRadians(spin);
        double cosA = Math.cos(rad);
        double sinA = Math.sin(rad);

        double[][] rotated = new double[vertices.length][3];
        for (int i = 0; i < vertices.length; i++) {
            double vx = vertices[i][0];
            double vy = vertices[i][1];
            double vz = vertices[i][2];
            rotated[i][0] = vx * cosA - vz * sinA;
            rotated[i][1] = vy;
            rotated[i][2] = vx * sinA + vz * cosA;
        }

        int[][] edges = {
                {1, 2}, {1, 3}, {1, 4}, {1, 5},
                {0, 2}, {0, 3}, {0, 4}, {0, 5},
                {2, 4}, {4, 3}, {3, 5}, {5, 2},
        };

        for (int[] edge : edges) {
            double[] v1 = rotated[edge[0]];
            double[] v2 = rotated[edge[1]];
            line(x + v1[0], y + v1[1], z + v1[2],
                    x + v2[0], y + v2[1], z + v2[2],
                    color, consumer, poseStack, cameraPos);
        }
    }

    private void drawRing(AABB box, int color, VertexConsumer consumer,
                          PoseStack poseStack, Vec3 cameraPos) {
        double cx = (box.minX + box.maxX) / 2;
        double cz = (box.minZ + box.maxZ) / 2;
        double radius = (box.maxX - box.minX) / 2 + 0.5;
        double height = box.maxY - box.minY;

        long time = System.currentTimeMillis();
        double timeSec = time / 1000.0;

        double baseY = box.minY;
        double ringY = baseY + Math.abs(Math.sin(timeSec * 1.2)) * height * 0.9;

        int segments = 48;
        double prevPx = 0, prevPz = 0;
        for (int i = 0; i <= segments; i++) {
            double a = i * Math.PI * 2 / segments;
            double px = cx + Math.cos(a) * radius;
            double pz = cz + Math.sin(a) * radius;

            if (i > 0) {
                line(prevPx, ringY, prevPz, px, ringY, pz, color,
                        consumer, poseStack, cameraPos);
            }
            prevPx = px;
            prevPz = pz;
        }
    }

    private void drawCubes(AABB box, int color, VertexConsumer consumer,
                           PoseStack poseStack, Vec3 cameraPos) {
        double cx = (box.minX + box.maxX) / 2;
        double cy = (box.minY + box.maxY) / 2;
        double cz = (box.minZ + box.maxZ) / 2;
        double radius = (box.maxX - box.minX) / 2 + 0.7;

        long time = System.currentTimeMillis();
        double timeSec = time / 1000.0;
        int count = 12;

        float sizeMul = ModConfig.targetEspSize;
        float rotationMul = ModConfig.targetEspRotationSpeed;
        float pulseAmt = ModConfig.targetEspPulse;
        double pulseGlobal = 1.0 + Math.sin(timeSec * 3.0) * pulseAmt;

        for (int i = 0; i < count; i++) {
            double angle = timeSec * rotationMul + i * Math.PI * 2 / count;
            double px = cx + Math.cos(angle) * radius;
            double pz = cz + Math.sin(angle) * radius;
            double py = cy + Math.sin(timeSec * 0.5 + i) * 0.6;
            double size = 0.10 * sizeMul * pulseGlobal;

            drawCubeWireframe(px, py, pz, size, color, consumer, poseStack, cameraPos);
        }
    }

    private void drawGhosts(AABB box, int color, VertexConsumer consumer,
                            PoseStack poseStack, Vec3 cameraPos) {
        double cx = (box.minX + box.maxX) / 2;
        double cy = (box.minY + box.maxY) / 2;
        double cz = (box.minZ + box.maxZ) / 2;

        double radius = (box.maxX - box.minX) / 2 + 0.5;

        long time = System.currentTimeMillis();
        double timeSec = time / 1000.0;

        float sizeMul = ModConfig.targetEspSize;
        float rotationMul = ModConfig.targetEspRotationSpeed;
        float pulseAmt = ModConfig.targetEspPulse;
        double pulseGlobal = 1.0 + Math.sin(timeSec * 3.0) * pulseAmt;

        int ghostCount = 3;
        int segments = 12;

        for (int ghost = 0; ghost < ghostCount; ghost++) {
            double ghostBaseAngle = ghost * Math.PI * 2 / ghostCount;

            double prevPx = 0, prevPy = 0, prevPz = 0;
            for (int i = 0; i < segments; i++) {
                double t = (double) i / (segments - 1);

                double scale = 1.0 - t * 0.75;
                double size = 0.11 * sizeMul * scale * pulseGlobal;

                double angle = ghostBaseAngle + timeSec * rotationMul * 0.6
                        - t * 0.45;

                double currentRadius = radius + t * 0.6;

                double px = cx + Math.cos(angle) * currentRadius;
                double pz = cz + Math.sin(angle) * currentRadius;
                double py = cy + 0.7 - t * 0.7
                        + Math.sin(timeSec * 0.8 + t * 2.5) * 0.1;

                drawCubeWireframe(px, py, pz, size, color, consumer, poseStack, cameraPos);

                if (i > 0) {
                    line(prevPx, prevPy, prevPz, px, py, pz, color,
                            consumer, poseStack, cameraPos);
                }

                prevPx = px;
                prevPy = py;
                prevPz = pz;
            }
        }
    }

    private void line(double x1, double y1, double z1,
                      double x2, double y2, double z2,
                      int color, VertexConsumer consumer,
                      PoseStack poseStack, Vec3 cameraPos) {
        float fx1 = (float) (x1 - cameraPos.x);
        float fy1 = (float) (y1 - cameraPos.y);
        float fz1 = (float) (z1 - cameraPos.z);
        float fx2 = (float) (x2 - cameraPos.x);
        float fy2 = (float) (y2 - cameraPos.y);
        float fz2 = (float) (z2 - cameraPos.z);

        Vector3f normal = new Vector3f(fx2 - fx1, fy2 - fy1, fz2 - fz1);
        if (normal.lengthSquared() > 0.000001f) {
            normal.normalize();
        }

        PoseStack.Pose pose = poseStack.last();

        consumer.addVertex(pose, fx1, fy1, fz1)
                .setColor(color)
                .setNormal(pose, normal)
                .setLineWidth(2.0f);

        consumer.addVertex(pose, fx2, fy2, fz2)
                .setColor(color)
                .setNormal(pose, normal)
                .setLineWidth(2.0f);
    }

    private void drawCubeWireframe(double x, double y, double z, double size,
                                   int color, VertexConsumer consumer,
                                   PoseStack poseStack, Vec3 cameraPos) {
        double x0 = x - size, x1 = x + size;
        double y0 = y - size, y1 = y + size;
        double z0 = z - size, z1 = z + size;

        line(x0, y0, z0, x1, y0, z0, color, consumer, poseStack, cameraPos);
        line(x1, y0, z0, x1, y0, z1, color, consumer, poseStack, cameraPos);
        line(x1, y0, z1, x0, y0, z1, color, consumer, poseStack, cameraPos);
        line(x0, y0, z1, x0, y0, z0, color, consumer, poseStack, cameraPos);

        line(x0, y1, z0, x1, y1, z0, color, consumer, poseStack, cameraPos);
        line(x1, y1, z0, x1, y1, z1, color, consumer, poseStack, cameraPos);
        line(x1, y1, z1, x0, y1, z1, color, consumer, poseStack, cameraPos);
        line(x0, y1, z1, x0, y1, z0, color, consumer, poseStack, cameraPos);

        line(x0, y0, z0, x0, y1, z0, color, consumer, poseStack, cameraPos);
        line(x1, y0, z0, x1, y1, z0, color, consumer, poseStack, cameraPos);
        line(x1, y0, z1, x1, y1, z1, color, consumer, poseStack, cameraPos);
        line(x0, y0, z1, x0, y1, z1, color, consumer, poseStack, cameraPos);
    }
}