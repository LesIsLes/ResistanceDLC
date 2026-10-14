package com.resistancedlc.targetesp;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.resistancedlc.config.ModConfig;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelExtractionContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class TargetESP {

    private final TargetManager targetManager;

    private static final RenderType FILL_TYPE = RenderType.create(
            "resistancedlc_targetesp_fill",
            RenderSetup.builder(RenderPipelines.DEBUG_QUADS).createRenderSetup()
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

    public void extract(LevelExtractionContext context) {}

    public void draw(LevelRenderContext context) {
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
        int alpha = ModConfig.targetEspAlpha & 0xFF;
        int argb = (alpha << 24) | (r << 16) | (g << 8) | b;

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
                (MultiBufferSource.BufferSource) context.bufferSource();
        if (bufferSource == null) return;

        Vec3 cameraPos = Minecraft.getInstance()
                .gameRenderer
                .getMainCamera()
                .position();

        switch (variant) {
            case CRYSTALS -> drawCrystalsFilled(box, argb, bufferSource, cameraPos);
            case RING     -> drawRingFilled(box, argb, bufferSource, cameraPos);
            case CUBES    -> drawCubesFilled(box, argb, bufferSource, cameraPos);
            case GHOSTS   -> drawGhostsFilled(box, argb, bufferSource, cameraPos);
            default -> {}
        }
    }

    // ================ CRYSTALS (заливка) ================
    private void drawCrystalsFilled(AABB box, int color,
                                    MultiBufferSource.BufferSource buf,
                                    Vec3 cameraPos) {
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

        VertexConsumer consumer = buf.getBuffer(FILL_TYPE);

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
            drawOctahedronFilled(px, py, pz, size, spin, color, consumer, cameraPos);
        }

        buf.endBatch(FILL_TYPE);
    }

    private void drawOctahedronFilled(double x, double y, double z, double size,
                                      double spin, int color, VertexConsumer c,
                                      Vec3 cam) {
        double[][] v = {
                { 0, -size * 1.5, 0 },
                { 0,  size * 1.5, 0 },
                { size, 0, 0 },
                { -size, 0, 0 },
                { 0, 0, size },
                { 0, 0, -size },
        };
        double rad = Math.toRadians(spin);
        double cosA = Math.cos(rad), sinA = Math.sin(rad);
        double[][] r = new double[6][3];
        for (int i = 0; i < 6; i++) {
            double vx = v[i][0], vy = v[i][1], vz = v[i][2];
            r[i][0] = vx * cosA - vz * sinA;
            r[i][1] = vy;
            r[i][2] = vx * sinA + vz * cosA;
        }
        int[][] faces = {
                {0, 2, 4}, {0, 4, 3}, {0, 3, 5}, {0, 5, 2},
                {1, 4, 2}, {1, 3, 4}, {1, 5, 3}, {1, 2, 5},
        };
        for (int[] f : faces) {
            vertexTri(c, r[f[0]], r[f[1]], r[f[2]], x, y, z, color, cam);
            // + обратная сторона (для двусторонности)
            vertexTri(c, r[f[2]], r[f[1]], r[f[0]], x, y, z, color, cam);
        }
    }

    // ================ RING (заливка) ================
    private void drawRingFilled(AABB box, int color,
                                MultiBufferSource.BufferSource buf,
                                Vec3 cameraPos) {
        double cx = (box.minX + box.maxX) / 2;
        double cz = (box.minZ + box.maxZ) / 2;
        double radius = (box.maxX - box.minX) / 2 + 0.5;
        double height = box.maxY - box.minY;

        long time = System.currentTimeMillis();
        double timeSec = time / 1000.0;
        double ringY = box.minY + Math.abs(Math.sin(timeSec * 1.2)) * height * 0.9;
        double thickness = 0.15;

        int segments = 48;
        VertexConsumer consumer = buf.getBuffer(FILL_TYPE);

        for (int i = 0; i < segments; i++) {
            double a1 = i * Math.PI * 2 / segments;
            double a2 = (i + 1) * Math.PI * 2 / segments;

            double rOuter = radius + thickness;
            double rInner = radius - thickness;

            double x1o = cx + Math.cos(a1) * rOuter;
            double z1o = cz + Math.sin(a1) * rOuter;
            double x2o = cx + Math.cos(a2) * rOuter;
            double z2o = cz + Math.sin(a2) * rOuter;
            double x1i = cx + Math.cos(a1) * rInner;
            double z1i = cz + Math.sin(a1) * rInner;
            double x2i = cx + Math.cos(a2) * rInner;
            double z2i = cz + Math.sin(a2) * rInner;

            quad(consumer,
                    (float)(x1o - cameraPos.x), (float)(ringY - cameraPos.y), (float)(z1o - cameraPos.z),
                    (float)(x2o - cameraPos.x), (float)(ringY - cameraPos.y), (float)(z2o - cameraPos.z),
                    (float)(x2i - cameraPos.x), (float)(ringY - cameraPos.y), (float)(z2i - cameraPos.z),
                    (float)(x1i - cameraPos.x), (float)(ringY - cameraPos.y), (float)(z1i - cameraPos.z),
                    color);
        }

        buf.endBatch(FILL_TYPE);
    }

    // ================ CUBES (заливка) ================
    private void drawCubesFilled(AABB box, int color,
                                 MultiBufferSource.BufferSource buf,
                                 Vec3 cameraPos) {
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

        VertexConsumer consumer = buf.getBuffer(FILL_TYPE);

        for (int i = 0; i < count; i++) {
            double angle = timeSec * rotationMul + i * Math.PI * 2 / count;
            double px = cx + Math.cos(angle) * radius;
            double pz = cz + Math.sin(angle) * radius;
            double py = cy + Math.sin(timeSec * 0.5 + i) * 0.6;
            double size = 0.10 * sizeMul * pulseGlobal;
            drawFilledCube(px, py, pz, size, color, consumer, cameraPos);
        }

        buf.endBatch(FILL_TYPE);
    }

    private void drawFilledCube(double x, double y, double z, double size, int color,
                                VertexConsumer c, Vec3 cam) {
        double x0 = x - size, x1 = x + size;
        double y0 = y - size, y1 = y + size;
        double z0 = z - size, z1 = z + size;

        // 6 граней куба (каждая — двусторонняя)
        // Front (+z)
        quad(c, (float)(x0-cam.x), (float)(y0-cam.y), (float)(z1-cam.z),
                (float)(x1-cam.x), (float)(y0-cam.y), (float)(z1-cam.z),
                (float)(x1-cam.x), (float)(y1-cam.y), (float)(z1-cam.z),
                (float)(x0-cam.x), (float)(y1-cam.y), (float)(z1-cam.z), color);
        quad(c, (float)(x0-cam.x), (float)(y1-cam.y), (float)(z1-cam.z),
                (float)(x1-cam.x), (float)(y1-cam.y), (float)(z1-cam.z),
                (float)(x1-cam.x), (float)(y0-cam.y), (float)(z1-cam.z),
                (float)(x0-cam.x), (float)(y0-cam.y), (float)(z1-cam.z), color);
        // Back (-z)
        quad(c, (float)(x1-cam.x), (float)(y0-cam.y), (float)(z0-cam.z),
                (float)(x0-cam.x), (float)(y0-cam.y), (float)(z0-cam.z),
                (float)(x0-cam.x), (float)(y1-cam.y), (float)(z0-cam.z),
                (float)(x1-cam.x), (float)(y1-cam.y), (float)(z0-cam.z), color);
        quad(c, (float)(x1-cam.x), (float)(y1-cam.y), (float)(z0-cam.z),
                (float)(x0-cam.x), (float)(y1-cam.y), (float)(z0-cam.z),
                (float)(x0-cam.x), (float)(y0-cam.y), (float)(z0-cam.z),
                (float)(x1-cam.x), (float)(y0-cam.y), (float)(z0-cam.z), color);
        // Top (+y)
        quad(c, (float)(x0-cam.x), (float)(y1-cam.y), (float)(z1-cam.z),
                (float)(x1-cam.x), (float)(y1-cam.y), (float)(z1-cam.z),
                (float)(x1-cam.x), (float)(y1-cam.y), (float)(z0-cam.z),
                (float)(x0-cam.x), (float)(y1-cam.y), (float)(z0-cam.z), color);
        quad(c, (float)(x0-cam.x), (float)(y1-cam.y), (float)(z0-cam.z),
                (float)(x1-cam.x), (float)(y1-cam.y), (float)(z0-cam.z),
                (float)(x1-cam.x), (float)(y1-cam.y), (float)(z1-cam.z),
                (float)(x0-cam.x), (float)(y1-cam.y), (float)(z1-cam.z), color);
        // Bottom (-y)
        quad(c, (float)(x0-cam.x), (float)(y0-cam.y), (float)(z0-cam.z),
                (float)(x1-cam.x), (float)(y0-cam.y), (float)(z0-cam.z),
                (float)(x1-cam.x), (float)(y0-cam.y), (float)(z1-cam.z),
                (float)(x0-cam.x), (float)(y0-cam.y), (float)(z1-cam.z), color);
        quad(c, (float)(x0-cam.x), (float)(y0-cam.y), (float)(z1-cam.z),
                (float)(x1-cam.x), (float)(y0-cam.y), (float)(z1-cam.z),
                (float)(x1-cam.x), (float)(y0-cam.y), (float)(z0-cam.z),
                (float)(x0-cam.x), (float)(y0-cam.y), (float)(z0-cam.z), color);
        // Right (+x)
        quad(c, (float)(x1-cam.x), (float)(y0-cam.y), (float)(z1-cam.z),
                (float)(x1-cam.x), (float)(y0-cam.y), (float)(z0-cam.z),
                (float)(x1-cam.x), (float)(y1-cam.y), (float)(z0-cam.z),
                (float)(x1-cam.x), (float)(y1-cam.y), (float)(z1-cam.z), color);
        quad(c, (float)(x1-cam.x), (float)(y1-cam.y), (float)(z1-cam.z),
                (float)(x1-cam.x), (float)(y1-cam.y), (float)(z0-cam.z),
                (float)(x1-cam.x), (float)(y0-cam.y), (float)(z0-cam.z),
                (float)(x1-cam.x), (float)(y0-cam.y), (float)(z1-cam.z), color);
        // Left (-x)
        quad(c, (float)(x0-cam.x), (float)(y0-cam.y), (float)(z0-cam.z),
                (float)(x0-cam.x), (float)(y0-cam.y), (float)(z1-cam.z),
                (float)(x0-cam.x), (float)(y1-cam.y), (float)(z1-cam.z),
                (float)(x0-cam.x), (float)(y1-cam.y), (float)(z0-cam.z), color);
        quad(c, (float)(x0-cam.x), (float)(y1-cam.y), (float)(z0-cam.z),
                (float)(x0-cam.x), (float)(y1-cam.y), (float)(z1-cam.z),
                (float)(x0-cam.x), (float)(y0-cam.y), (float)(z1-cam.z),
                (float)(x0-cam.x), (float)(y0-cam.y), (float)(z0-cam.z), color);
    }

    // ================ GHOSTS (заливка маленьких кубиков со шлейфом) ================
    private void drawGhostsFilled(AABB box, int color,
                                  MultiBufferSource.BufferSource buf,
                                  Vec3 cameraPos) {
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

        VertexConsumer consumer = buf.getBuffer(FILL_TYPE);

        for (int ghost = 0; ghost < ghostCount; ghost++) {
            double ghostBaseAngle = ghost * Math.PI * 2 / ghostCount;

            for (int i = 0; i < segments; i++) {
                double t = (double) i / (segments - 1);

                double scale = 1.0 - t * 0.75;
                double size = 0.11 * sizeMul * scale * pulseGlobal;

                double angle = ghostBaseAngle + timeSec * rotationMul * 0.6 - t * 0.45;
                double currentRadius = radius + t * 0.6;

                double px = cx + Math.cos(angle) * currentRadius;
                double pz = cz + Math.sin(angle) * currentRadius;
                double py = cy + 0.7 - t * 0.7 + Math.sin(timeSec * 0.8 + t * 2.5) * 0.1;

                drawFilledCube(px, py, pz, size, color, consumer, cameraPos);
            }
        }

        buf.endBatch(FILL_TYPE);
    }

    // ================ ХЕЛПЕРЫ ================

    private static void vertexTri(VertexConsumer c,
                                  double[] v1, double[] v2, double[] v3,
                                  double ox, double oy, double oz,
                                  int color, Vec3 cam) {
        c.addVertex((float)(ox + v1[0] - cam.x), (float)(oy + v1[1] - cam.y), (float)(oz + v1[2] - cam.z)).setColor(color);
        c.addVertex((float)(ox + v2[0] - cam.x), (float)(oy + v2[1] - cam.y), (float)(oz + v2[2] - cam.z)).setColor(color);
        c.addVertex((float)(ox + v3[0] - cam.x), (float)(oy + v3[1] - cam.y), (float)(oz + v3[2] - cam.z)).setColor(color);
        c.addVertex((float)(ox + v3[0] - cam.x), (float)(oy + v3[1] - cam.y), (float)(oz + v3[2] - cam.z)).setColor(color);
    }

    private static void quad(VertexConsumer c,
                             float x1, float y1, float z1,
                             float x2, float y2, float z2,
                             float x3, float y3, float z3,
                             float x4, float y4, float z4,
                             int color) {
        c.addVertex(x1, y1, z1).setColor(color);
        c.addVertex(x2, y2, z2).setColor(color);
        c.addVertex(x3, y3, z3).setColor(color);
        c.addVertex(x4, y4, z4).setColor(color);
    }
}