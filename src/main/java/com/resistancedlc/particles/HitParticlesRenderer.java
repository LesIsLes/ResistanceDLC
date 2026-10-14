package com.resistancedlc.particles;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.resistancedlc.config.ModConfig;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.world.phys.Vec3;

public final class HitParticlesRenderer {

    private static final RenderType PARTICLE_TYPE = RenderType.create(
            "resistancedlc_hit_particles",
            RenderSetup.builder(RenderPipelines.DEBUG_QUADS).createRenderSetup()
    );

    private HitParticlesRenderer() {}

    public static void render(LevelRenderContext context) {
        if (!ModConfig.hitParticlesEnabled) return;
        var particles = new java.util.ArrayList<>(HitParticlesManager.getParticles());
        if (particles.isEmpty()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        MultiBufferSource.BufferSource bufferSource =
                (MultiBufferSource.BufferSource) context.bufferSource();
        if (bufferSource == null) return;

        Vec3 cameraPos = mc.gameRenderer.getMainCamera().position();
        VertexConsumer consumer = bufferSource.getBuffer(PARTICLE_TYPE);

        long now = System.currentTimeMillis();
        for (HitParticle p : particles) {
            Vec3 pos = p.getCurrentPosition(now);
            float age = p.getAge(now);
            float alphaFactor = age < 0.6f ? 1.0f : (1.0f - (age - 0.6f) / 0.4f);
            if (alphaFactor <= 0) continue;

            int a = (int) (((p.color >> 24) & 0xFF) * alphaFactor);
            int r = (p.color >> 16) & 0xFF;
            int g = (p.color >> 8) & 0xFF;
            int b = p.color & 0xFF;
            int argb = (a << 24) | (r << 16) | (g << 8) | b;

            double timeSec = now / 1000.0;
            double rotation = timeSec * p.rotationSpeed;
            float pulse = 1.0f + (float) Math.sin(timeSec * 8.0) * 0.15f;
            float size = p.size * pulse;

            float px = (float) (pos.x - cameraPos.x);
            float py = (float) (pos.y - cameraPos.y);
            float pz = (float) (pos.z - cameraPos.z);

            switch (p.shape) {
                case CRYSTAL   -> drawCrystal(consumer, px, py, pz, size, rotation, argb);
                case STAR      -> drawStar(consumer, px, py, pz, size, rotation, argb);
                case SNOWFLAKE -> drawSnowflake(consumer, px, py, pz, size, rotation, argb);
                case PYRAMID   -> drawPyramid(consumer, px, py, pz, size, rotation, argb);
            }
        }

        bufferSource.endBatch(PARTICLE_TYPE);
    }

    // ==================== CRYSTAL (октаэдр) ====================
    private static void drawCrystal(VertexConsumer c,
                                    float x, float y, float z,
                                    float size, double spin, int color) {
        double cosA = Math.cos(spin), sinA = Math.sin(spin);

        float topY = size * 1.5f;
        float botY = -size * 1.5f;
        float eqR = size;

        float[][] baseEq = {
                {  eqR, 0,    0 },
                {  0,   0,   eqR },
                { -eqR, 0,    0 },
                {  0,   0,  -eqR },
        };
        float[][] eq = new float[4][3];
        for (int i = 0; i < 4; i++) {
            eq[i][0] = (float) (baseEq[i][0] * cosA - baseEq[i][2] * sinA);
            eq[i][1] = baseEq[i][1];
            eq[i][2] = (float) (baseEq[i][0] * sinA + baseEq[i][2] * cosA);
        }

        float ty = y + topY;
        float by = y + botY;

        for (int i = 0; i < 4; i++) {
            int j = (i + 1) % 4;

            // Верхняя грань: T → e[i] → e[j]
            triAsQuadBoth(c,
                    x, ty, z,
                    x + eq[i][0], y + eq[i][1], z + eq[i][2],
                    x + eq[j][0], y + eq[j][1], z + eq[j][2],
                    color);

            // Нижняя грань: B → e[i] → e[j] (тот же порядок!)
            triAsQuadBoth(c,
                    x, by, z,
                    x + eq[i][0], y + eq[i][1], z + eq[i][2],
                    x + eq[j][0], y + eq[j][1], z + eq[j][2],
                    color);
        }
    }

    // ==================== STAR (5-конечная звёздочка) ====================
    private static void drawStar(VertexConsumer c,
                                 float x, float y, float z,
                                 float size, double spin, int color) {
        int points = 5;
        float outerR = size * 1.8f;
        float innerR = size * 0.75f;

        double cosA = Math.cos(spin), sinA = Math.sin(spin);

        float[][] verts = new float[points * 2][2];
        for (int i = 0; i < points * 2; i++) {
            float r = (i % 2 == 0) ? outerR : innerR;
            double angle = -Math.PI / 2 + i * Math.PI / points;
            float vx = (float) (Math.cos(angle) * r);
            float vy = (float) (Math.sin(angle) * r);
            verts[i][0] = (float) (vx * cosA - vy * sinA);
            verts[i][1] = (float) (vx * sinA + vy * cosA);
        }

        for (int i = 0; i < points * 2; i++) {
            int j = (i + 1) % (points * 2);
            triAsQuadBoth(c,
                    x, y, z,
                    x + verts[i][0], y + verts[i][1], z,
                    x + verts[j][0], y + verts[j][1], z,
                    color);
        }
    }

    // ==================== SNOWFLAKE ====================
    private static void drawSnowflake(VertexConsumer c,
                                      float x, float y, float z,
                                      float size, double spin, int color) {
        double cosA = Math.cos(spin), sinA = Math.sin(spin);

        float bigLen = size * 2.2f;
        float bigHalfW = size * 0.18f;
        float bigArmOffset = size * 0.15f;

        drawPlusArm(c, x, y, z, bigLen, bigHalfW, bigArmOffset, 0.0, cosA, sinA, color);
        drawPlusArm(c, x, y, z, bigLen, bigHalfW, bigArmOffset, Math.PI, cosA, sinA, color);
        drawPlusArm(c, x, y, z, bigLen, bigHalfW, bigArmOffset, Math.PI / 2, cosA, sinA, color);
        drawPlusArm(c, x, y, z, bigLen, bigHalfW, bigArmOffset, -Math.PI / 2, cosA, sinA, color);

        float smallLen = size * 1.3f;
        float smallHalfW = size * 0.12f;
        float smallArmOffset = size * 0.1f;

        drawPlusArm(c, x, y, z, smallLen, smallHalfW, smallArmOffset,  Math.PI / 4, cosA, sinA, color);
        drawPlusArm(c, x, y, z, smallLen, smallHalfW, smallArmOffset,  Math.PI * 3 / 4, cosA, sinA, color);
        drawPlusArm(c, x, y, z, smallLen, smallHalfW, smallArmOffset, -Math.PI / 4, cosA, sinA, color);
        drawPlusArm(c, x, y, z, smallLen, smallHalfW, smallArmOffset, -Math.PI * 3 / 4, cosA, sinA, color);
    }

    private static void drawPlusArm(VertexConsumer c,
                                    float cx, float cy, float cz,
                                    float length, float halfW, float offset,
                                    double angle, double cosRot, double sinRot,
                                    int color) {
        double cosA = Math.cos(angle), sinA = Math.sin(angle);

        double[][] local = {
                { cosA * offset - sinA * halfW, sinA * offset + cosA * halfW },
                { cosA * offset + sinA * halfW, sinA * offset - cosA * halfW },
                { cosA * (offset + length) + sinA * halfW, sinA * (offset + length) - cosA * halfW },
                { cosA * (offset + length) - sinA * halfW, sinA * (offset + length) + cosA * halfW },
        };

        float[] r = new float[8];
        for (int i = 0; i < 4; i++) {
            double px = local[i][0], py = local[i][1];
            r[i * 2]     = (float) (px * cosRot - py * sinRot);
            r[i * 2 + 1] = (float) (px * sinRot + py * cosRot);
        }

        // Двусторонний
        quadBoth(c,
                cx + r[0], cy + r[1], cz,
                cx + r[2], cy + r[3], cz,
                cx + r[4], cy + r[5], cz,
                cx + r[6], cy + r[7], cz,
                color);
    }

    // ==================== PYRAMID ====================
    private static void drawPyramid(VertexConsumer c,
                                    float x, float y, float z,
                                    float size, double spin, int color) {
        double cosA = Math.cos(spin), sinA = Math.sin(spin);
        float h = size * 1.8f;
        float w = size;

        float[][] base = {
                {-w, -h / 2, -w},
                { w, -h / 2, -w},
                { w, -h / 2,  w},
                {-w, -h / 2,  w},
        };
        float[][] rBase = new float[4][3];
        for (int i = 0; i < 4; i++) {
            float bx = base[i][0], by = base[i][1], bz = base[i][2];
            rBase[i][0] = (float) (bx * cosA - bz * sinA);
            rBase[i][1] = by;
            rBase[i][2] = (float) (bx * sinA + bz * cosA);
        }

        float apexY = y + h / 2;

        // 4 боковые грани
        for (int i = 0; i < 4; i++) {
            int j = (i + 1) % 4;
            triAsQuadBoth(c,
                    x, apexY, z,
                    x + rBase[i][0], y + rBase[i][1], z + rBase[i][2],
                    x + rBase[j][0], y + rBase[j][1], z + rBase[j][2],
                    color);
        }
        // Основание
        quadBoth(c,
                x + rBase[0][0], y + rBase[0][1], z + rBase[0][2],
                x + rBase[1][0], y + rBase[1][1], z + rBase[1][2],
                x + rBase[2][0], y + rBase[2][1], z + rBase[2][2],
                x + rBase[3][0], y + rBase[3][1], z + rBase[3][2],
                color);
    }

    // ==================== ХЕЛПЕРЫ ====================

    /**
     * Треугольник как вырожденный quad, но нарисованный ДВАЖДЫ —
     * прямой и обратный порядок вершин. Это гарантирует, что грань
     * видна с обеих сторон, независимо от backface culling.
     */
    private static void triAsQuadBoth(VertexConsumer c,
                                      float x1, float y1, float z1,
                                      float x2, float y2, float z2,
                                      float x3, float y3, float z3,
                                      int color) {
        // Прямой порядок
        c.addVertex(x1, y1, z1).setColor(color);
        c.addVertex(x2, y2, z2).setColor(color);
        c.addVertex(x3, y3, z3).setColor(color);
        c.addVertex(x3, y3, z3).setColor(color);
        // Обратный порядок
        c.addVertex(x3, y3, z3).setColor(color);
        c.addVertex(x2, y2, z2).setColor(color);
        c.addVertex(x1, y1, z1).setColor(color);
        c.addVertex(x1, y1, z1).setColor(color);
    }

    /** Quad, нарисованный дважды — с двух сторон. */
    private static void quadBoth(VertexConsumer c,
                                 float x1, float y1, float z1,
                                 float x2, float y2, float z2,
                                 float x3, float y3, float z3,
                                 float x4, float y4, float z4,
                                 int color) {
        // Прямой
        c.addVertex(x1, y1, z1).setColor(color);
        c.addVertex(x2, y2, z2).setColor(color);
        c.addVertex(x3, y3, z3).setColor(color);
        c.addVertex(x4, y4, z4).setColor(color);
        // Обратный
        c.addVertex(x4, y4, z4).setColor(color);
        c.addVertex(x3, y3, z3).setColor(color);
        c.addVertex(x2, y2, z2).setColor(color);
        c.addVertex(x1, y1, z1).setColor(color);
    }
}