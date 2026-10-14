package com.resistancedlc.particles;

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

public final class AtmosphereRenderer {

    private static final RenderType DROP_TYPE = RenderType.create(
            "resistancedlc_atmosphere",
            RenderSetup.builder(RenderPipelines.DEBUG_QUADS).createRenderSetup()
    );

    private AtmosphereRenderer() {}

    public static void render(LevelRenderContext context) {
        if (!ModConfig.atmosphereEnabled) return;
        var drops = new java.util.ArrayList<>(AtmosphereManager.getDrops());
        if (drops.isEmpty()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        MultiBufferSource.BufferSource bufferSource =
                (MultiBufferSource.BufferSource) context.bufferSource();
        if (bufferSource == null) return;

        PoseStack poseStack = context.poseStack();
        PoseStack.Pose pose = poseStack.last();
        Vec3 cameraPos = mc.gameRenderer.getMainCamera().position();

        VertexConsumer consumer = bufferSource.getBuffer(DROP_TYPE);

        long now = System.currentTimeMillis();
        int baseColor = ModConfig.atmosphereColor;
        int baseAlpha = ModConfig.atmosphereAlpha & 0xFF;
        float size = 0.12f * ModConfig.atmosphereSize;
        boolean rotate = ModConfig.atmosphereRotate;
        boolean isSnow = "snow".equals(ModConfig.atmosphereType);

        for (AtmosphereManager.Drop d : drops) {
            // Fade-in первые 10%, fade-out последние 20%
            long age = now - d.spawnTime;
            float life = (float) age / d.lifetimeMs;
            float alphaFactor;
            if (life < 0.1f) alphaFactor = life / 0.1f;
            else if (life > 0.8f) alphaFactor = (1.0f - life) / 0.2f;
            else alphaFactor = 1.0f;
            if (alphaFactor <= 0) continue;

            int a = (int) (baseAlpha * alphaFactor);
            int r = (baseColor >> 16) & 0xFF;
            int g = (baseColor >> 8) & 0xFF;
            int b = baseColor & 0xFF;
            int argb = (a << 24) | (r << 16) | (g << 8) | b;

            float px = (float) (d.x - cameraPos.x);
            float py = (float) (d.y - cameraPos.y);
            float pz = (float) (d.z - cameraPos.z);

            double rotation = rotate ? d.rotation : 0.0;

            if (isSnow) {
                drawSnowflake(consumer, pose, px, py, pz, size, rotation, argb);
            } else {
                drawStar(consumer, pose, px, py, pz, size, rotation, argb);
            }
        }

        bufferSource.endBatch(DROP_TYPE);
    }

    // ==================== STAR ====================
    private static void drawStar(VertexConsumer c, PoseStack.Pose pose,
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
            triAsQuad(c, pose,
                    x, y, z,
                    x + verts[i][0], y + verts[i][1], z,
                    x + verts[j][0], y + verts[j][1], z,
                    color);
        }
    }

    // ==================== SNOWFLAKE ====================
    private static void drawSnowflake(VertexConsumer c, PoseStack.Pose pose,
                                      float x, float y, float z,
                                      float size, double spin, int color) {
        double cosA = Math.cos(spin), sinA = Math.sin(spin);

        float bigLen = size * 2.2f;
        float bigHalfW = size * 0.18f;
        float bigArmOffset = size * 0.15f;

        drawPlusArm(c, pose, x, y, z, bigLen, bigHalfW, bigArmOffset, 0.0, cosA, sinA, color);
        drawPlusArm(c, pose, x, y, z, bigLen, bigHalfW, bigArmOffset, Math.PI, cosA, sinA, color);
        drawPlusArm(c, pose, x, y, z, bigLen, bigHalfW, bigArmOffset, Math.PI / 2, cosA, sinA, color);
        drawPlusArm(c, pose, x, y, z, bigLen, bigHalfW, bigArmOffset, -Math.PI / 2, cosA, sinA, color);

        float smallLen = size * 1.3f;
        float smallHalfW = size * 0.12f;
        float smallArmOffset = size * 0.1f;

        drawPlusArm(c, pose, x, y, z, smallLen, smallHalfW, smallArmOffset,  Math.PI / 4, cosA, sinA, color);
        drawPlusArm(c, pose, x, y, z, smallLen, smallHalfW, smallArmOffset,  Math.PI * 3 / 4, cosA, sinA, color);
        drawPlusArm(c, pose, x, y, z, smallLen, smallHalfW, smallArmOffset, -Math.PI / 4, cosA, sinA, color);
        drawPlusArm(c, pose, x, y, z, smallLen, smallHalfW, smallArmOffset, -Math.PI * 3 / 4, cosA, sinA, color);
    }

    private static void drawPlusArm(VertexConsumer c, PoseStack.Pose pose,
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

        quad(c, pose,
                cx + r[0], cy + r[1], cz,
                cx + r[2], cy + r[3], cz,
                cx + r[4], cy + r[5], cz,
                cx + r[6], cy + r[7], cz,
                color);
    }

    // ==================== ХЕЛПЕРЫ ====================
    private static void triAsQuad(VertexConsumer c, PoseStack.Pose pose,
                                  float x1, float y1, float z1,
                                  float x2, float y2, float z2,
                                  float x3, float y3, float z3,
                                  int color) {
        c.addVertex(pose, x1, y1, z1).setColor(color);
        c.addVertex(pose, x2, y2, z2).setColor(color);
        c.addVertex(pose, x3, y3, z3).setColor(color);
        c.addVertex(pose, x3, y3, z3).setColor(color);
    }

    private static void quad(VertexConsumer c, PoseStack.Pose pose,
                             float x1, float y1, float z1,
                             float x2, float y2, float z2,
                             float x3, float y3, float z3,
                             float x4, float y4, float z4,
                             int color) {
        c.addVertex(pose, x1, y1, z1).setColor(color);
        c.addVertex(pose, x2, y2, z2).setColor(color);
        c.addVertex(pose, x3, y3, z3).setColor(color);
        c.addVertex(pose, x4, y4, z4).setColor(color);
    }
}