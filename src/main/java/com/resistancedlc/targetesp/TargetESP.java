package com.resistancedlc.targetesp;

import com.resistancedlc.config.ModConfig;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldExtractionContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * TargetESP — рисует эффекты вокруг цели через Gizmos.
 *
 * 5 режимов:
 *   - CRYSTALS — октаэдры-в-октаэдрах вокруг цели (одно кольцо из 8)
 *   - RING     — кольцо, плавно поднимается и опускается
 *   - CUBES    — 12 кубиков вокруг цели
 *   - GHOSTS_2 — 3 щупальца с хвостами
 *   - BRACKET  — bracket-рамка + вращающийся крест в центре
 */
public final class TargetESP {

    private final TargetManager targetManager;

    public TargetESP(TargetManager targetManager) {
        this.targetManager = targetManager;
    }

    public void resetTransientState() {}

    public void extract(WorldExtractionContext context) {}

    // ===================== DRAW =====================

    public void draw(WorldRenderContext context) {
        if (!ModConfig.targetEspEnabled) return;

        LivingEntity target = targetManager.getLivingTarget();
        if (target == null) return;

        float tickProgress = Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false);

        int baseColor = ModConfig.targetEspColor;
        int r = (baseColor >> 16) & 0xFF;
        int g = (baseColor >> 8) & 0xFF;
        int b = baseColor & 0xFF;
        int argb = (0xFF << 24) | (r << 16) | (g << 8) | b;

        Variant variant = Variant.fromId(ModConfig.targetEspVariant);

        Vec3 pos = target.getPosition(tickProgress);
        AABB box = target.getBoundingBox().move(pos.subtract(target.position()));

        switch (variant) {
            case CRYSTALS -> drawCrystals(box, argb);
            case RING -> drawRing(box, argb);
            case CUBES -> drawCubes(box, argb);
            case GHOSTS_2 -> drawGhosts2(box, argb);
            default -> {}
        }
    }

    // ===================== MODE: CRYSTALS =====================

    private void drawCrystals(AABB box, int color) {
        double cx = (box.minX + box.maxX) / 2;
        double cy = (box.minY + box.maxY) / 2;
        double cz = (box.minZ + box.maxZ) / 2;
        double radius = (box.maxX - box.minX) / 2 + 0.9;

        long time = System.currentTimeMillis();
        double timeSec = time / 1000.0;
        int count = 8;

        float sizeMul = ModConfig.targetEspSize;
        float rotationMul = ModConfig.targetEspRotationSpeed;
        float pulseAmt = ModConfig.targetEspPulse;
        double pulseGlobal = 1.0 + Math.sin(timeSec * 3.0) * pulseAmt;

        // Одно кольцо из 8 кристаллов
        for (int i = 0; i < count; i++) {
            double angle = timeSec * rotationMul * 1.5 + (i * Math.PI * 2 / count);
            double px = cx + Math.cos(angle) * radius;
            double pz = cz + Math.sin(angle) * radius;
            double py = cy + Math.sin(timeSec * 1.5 + i * 0.8) * 0.5;

            double pulseLocal = 1.0 + Math.sin(timeSec * 4.0 + i * 1.2) * pulseAmt;
            double size = 0.10 * sizeMul * pulseGlobal * pulseLocal;

            double spin = timeSec * 40.0 + i * 30.0;

            drawOctahedron(px, py, pz, size, spin, color);
            drawOctahedron(px, py, pz, size * 0.4, spin + 45.0, color);
        }
    }

    /**
     * Рисует октаэдр (6 вершин, 12 рёбер) через Gizmos.line().
     *
     * @param spin вращение вокруг вертикальной оси в градусах
     */
    private void drawOctahedron(double x, double y, double z, double size,
                                double spin, int color) {
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
                    x + v2[0], y + v2[1], z + v2[2], color);
        }
    }

    // ===================== MODE: RING =====================

    private void drawRing(AABB box, int color) {
        double cx = (box.minX + box.maxX) / 2;
        double cy = (box.minY + box.maxY) / 2;
        double cz = (box.minZ + box.maxZ) / 2;
        double radius = (box.maxX - box.minX) / 2 + 0.5;
        double height = box.maxY - box.minY;

        long time = System.currentTimeMillis();
        double timeSec = time / 1000.0;

        // Плавное движение вверх-вниз
        double ringY = cy + Math.sin(timeSec * 1.2) * (height * 0.6);

        float sizeMul = ModConfig.targetEspSize;
        float rotationMul = ModConfig.targetEspRotationSpeed;
        float pulseAmt = ModConfig.targetEspPulse;
        double pulseGlobal = 1.0 + Math.sin(timeSec * 3.0) * pulseAmt;
        double angleOffset = timeSec * rotationMul * 2.0;

        int segments = 24;
        for (int i = 0; i < segments; i++) {
            double a = angleOffset + i * Math.PI * 2 / segments;
            double px = cx + Math.cos(a) * radius;
            double pz = cz + Math.sin(a) * radius;
            double size = 0.04 * sizeMul * pulseGlobal;

            Gizmos.cuboid(
                    new AABB(px - size, ringY - size, pz - size,
                            px + size, ringY + size, pz + size),
                    GizmoStyle.stroke(color));
        }
    }

    // ===================== MODE: CUBES =====================

    private void drawCubes(AABB box, int color) {
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

            Gizmos.cuboid(
                    new AABB(px - size, py - size, pz - size,
                            px + size, py + size, pz + size),
                    GizmoStyle.stroke(color));
        }
    }

    // ===================== MODE: GHOSTS_2 =====================

    private void drawGhosts2(AABB box, int color) {
        double cx = (box.minX + box.maxX) / 2;
        double cy = (box.minY + box.maxY) / 2;
        double cz = (box.minZ + box.maxZ) / 2;
        double radius = (box.maxX - box.minX) / 2 + 0.6;

        long time = System.currentTimeMillis();
        double timeSec = time / 1000.0;

        float sizeMul = ModConfig.targetEspSize;
        float rotationMul = ModConfig.targetEspRotationSpeed;
        float pulseAmt = ModConfig.targetEspPulse;
        double pulseGlobal = 1.0 + Math.sin(timeSec * 3.0) * pulseAmt;

        for (int arm = 0; arm < 3; arm++) {
            double armOffset = arm * Math.PI * 2 / 3;
            for (int sample = 0; sample < 14; sample++) {
                double angle = armOffset + timeSec * rotationMul * 0.8 + sample * 0.15;
                double px = cx + Math.cos(angle) * radius;
                double pz = cz + Math.sin(angle) * radius;
                double py = cy + Math.sin(timeSec * 0.8 + sample * 0.4) * 0.6;
                double size = 0.06 * sizeMul * pulseGlobal;

                Gizmos.cuboid(
                        new AABB(px - size, py - size, pz - size,
                                px + size, py + size, pz + size),
                        GizmoStyle.stroke(color));
            }
        }
    }

    // ===================== ХЕЛПЕРЫ =====================

    /** Хелпер — линия через Gizmos. */
    private void line(double x1, double y1, double z1,
                      double x2, double y2, double z2, int color) {
        Gizmos.line(new Vec3(x1, y1, z1), new Vec3(x2, y2, z2), color);
    }
}