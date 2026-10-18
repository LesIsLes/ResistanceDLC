package com.resistancedlc.headwear;

import com.mojang.blaze3d.vertex.PoseStack;
import com.resistancedlc.FriendListManager;
import com.resistancedlc.config.ModConfig;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * HeadwearRenderer — процедурные шляпы на голове игрока (Minecraft 26.2).
 *
 * v13 — синхронизация с моделью головы:
 *   Модель игрока рендерится с partialTick = getGameTimeDeltaPartialTick(false),
 *   интерполируя позицию через Mth.lerp(xo, getX()) и yaw через Mth.rotLerp(yRotO, getYRot()).
 *   Мы делаем ТОЧНО ТАК ЖЕ, чтобы шляпа двигалась синхронно с моделью
 *   (это убирает дёрганье при повороте и асинхронность при прыжке).
 */
public final class HeadwearRenderer {

    private static final RenderType HAT_TYPE = RenderType.create(
            "resistancedlc_headwear",
            RenderSetup.builder(RenderPipelines.DEBUG_QUADS).createRenderSetup()
    );

    /** Смещение от pivot (шеи) до верхней точки модели головы. */
    private static final float PIVOT_TO_TOP = 0.3f;

    private HeadwearRenderer() {}

    // ===================== ГЛАВНЫЙ ВХОД =====================

    public static void render(LevelRenderContext context) {
        if (!ModConfig.headwearEnabled) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        HeadwearType type = HeadwearType.fromId(ModConfig.headwearType);
        if (type == HeadwearType.NONE) return;

        SubmitNodeCollector buf = context.submitNodeCollector();
        if (buf == null) return;

        PoseStack poseStack = context.poseStack();
        var camera = mc.gameRenderer.mainCamera();
        Vec3 cam = camera.position();

        // ⭐ Тот же partialTick, что и у модели сущности.
        float partialTick = mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);

        int color = ModConfig.headwearColor;
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        int a = ModConfig.headwearAlpha & 0xFF;
        int argb = (a << 24) | (r << 16) | (g << 8) | b;

        boolean firstPerson = mc.options.getCameraType() == CameraType.FIRST_PERSON;
        if (ModConfig.headwearSelfEnabled && !firstPerson) {
            drawForPlayer(mc.player, type, argb, buf, poseStack, cam, partialTick);
        }

        if (ModConfig.headwearShowFriends && ModConfig.friendListEnabled) {
            for (AbstractClientPlayer other : mc.level.players()) {
                if (other == mc.player) continue;
                if (!FriendListManager.isFriend(other.getName().getString())) continue;
                drawForPlayer(other, type, argb, buf, poseStack, cam, partialTick);
            }
        }
    }

    // ===================== ОДИН ИГРОК =====================

    private static void drawForPlayer(AbstractClientPlayer player,
                                      HeadwearType type,
                                      int argb,
                                      SubmitNodeCollector buf,
                                      PoseStack poseStack,
                                      Vec3 cam,
                                      float pt) {
        // Интерполированная позиция (для плавного движения/прыжка)
        double px = Mth.lerp(pt, player.xo, player.getX());
        double py = Mth.lerp(pt, player.yo, player.getY());
        double pz = Mth.lerp(pt, player.zo, player.getZ());

        double pivotX = px;
        double pivotY = py + player.getBbHeight() - 0.3 + ModConfig.headwearOffsetY;
        double pivotZ = pz;

        // ⭐ Вариант 2 — прямой yHeadRot + xRot (синхронно с моделью головы)
        float headYaw = player.yHeadRot;
        float pitch   = player.getXRot();

        float yawRad = (float) Math.toRadians(-headYaw);
        float pitchRad = (float) Math.toRadians(pitch);
        float scale = ModConfig.headwearScale;

        switch (type) {
            case CHINESE_HAT -> drawChineseHat(pivotX, pivotY, pivotZ,
                    yawRad, pitchRad, scale, argb, buf, poseStack, cam);
            case CAP -> drawCap(pivotX, pivotY, pivotZ,
                    yawRad, pitchRad, scale, argb, buf, poseStack, cam);
            case TOP_HAT -> drawTopHat(pivotX, pivotY, pivotZ,
                    yawRad, pitchRad, scale, argb, buf, poseStack, cam);
            default -> {}
        }
    }

    // ===================== МАТЕМАТИКА =====================

    /** Порядок как в Minecraft: pitch (вокруг X) → yaw (вокруг Y). */
    private static float[] rotate(float x, float y, float z, float yaw, float pitch) {
        float cosP = (float) Math.cos(pitch);
        float sinP = (float) Math.sin(pitch);
        float x1 = x;
        float y1 = y * cosP - z * sinP;
        float z1 = y * sinP + z * cosP;

        float cosY = (float) Math.cos(yaw);
        float sinY = (float) Math.sin(yaw);
        float x2 = x1 * cosY + z1 * sinY;
        float z2 = -x1 * sinY + z1 * cosY;
        float y2 = y1;

        return new float[]{x2, y2, z2};
    }

    private static float[] rot(double px, double py, double pz,
                               float yaw, float pitch, float scale, Vec3 cam,
                               float lx, float ly, float lz) {
        float[] r = rotate(
                lx * scale,
                ly * scale + PIVOT_TO_TOP,
                lz * scale,
                yaw, pitch
        );
        return new float[]{
                (float)(px + r[0] - cam.x),
                (float)(py + r[1] - cam.y),
                (float)(pz + r[2] - cam.z)
        };
    }

    private static void submitQuad(SubmitNodeCollector buf, PoseStack poseStack,
                                   float[] p1, float[] p2, float[] p3, float[] p4, int argb) {
        buf.submitCustomGeometry(poseStack, HAT_TYPE, (pose, c) -> {
            c.addVertex(pose, p1[0], p1[1], p1[2]).setColor(argb);
            c.addVertex(pose, p2[0], p2[1], p2[2]).setColor(argb);
            c.addVertex(pose, p3[0], p3[1], p3[2]).setColor(argb);
            c.addVertex(pose, p4[0], p4[1], p4[2]).setColor(argb);
        });
    }

    private static void submitTri(SubmitNodeCollector buf, PoseStack poseStack,
                                  float[] p1, float[] p2, float[] p3, int argb) {
        buf.submitCustomGeometry(poseStack, HAT_TYPE, (pose, c) -> {
            c.addVertex(pose, p1[0], p1[1], p1[2]).setColor(argb);
            c.addVertex(pose, p2[0], p2[1], p2[2]).setColor(argb);
            c.addVertex(pose, p3[0], p3[1], p3[2]).setColor(argb);
            c.addVertex(pose, p3[0], p3[1], p3[2]).setColor(argb);
        });
    }

    // ===================== КИТАЙСКАЯ ШЛЯПА =====================
    private static void drawChineseHat(double px, double py, double pz,
                                       float yaw, float pitch, float scale,
                                       int argb,
                                       SubmitNodeCollector buf, PoseStack poseStack, Vec3 cam) {
        float outerR = 0.50f;
        float innerR = 0.28f;
        float brimY = 0.0f;
        float coneTopY = 0.30f;
        int seg = 24;

        for (int i = 0; i < seg; i++) {
            double a1 = 2 * Math.PI * i / seg;
            double a2 = 2 * Math.PI * (i + 1) / seg;
            float xo1 = (float)(Math.cos(a1) * outerR);
            float zo1 = (float)(Math.sin(a1) * outerR);
            float xo2 = (float)(Math.cos(a2) * outerR);
            float zo2 = (float)(Math.sin(a2) * outerR);
            float xi1 = (float)(Math.cos(a1) * innerR);
            float zi1 = (float)(Math.sin(a1) * innerR);
            float xi2 = (float)(Math.cos(a2) * innerR);
            float zi2 = (float)(Math.sin(a2) * innerR);

            float[] p1 = rot(px, py, pz, yaw, pitch, scale, cam, xo1, brimY, zo1);
            float[] p2 = rot(px, py, pz, yaw, pitch, scale, cam, xo2, brimY, zo2);
            float[] p3 = rot(px, py, pz, yaw, pitch, scale, cam, xi2, brimY, zi2);
            float[] p4 = rot(px, py, pz, yaw, pitch, scale, cam, xi1, brimY, zi1);
            submitQuad(buf, poseStack, p1, p2, p3, p4, argb);
            submitQuad(buf, poseStack, p4, p3, p2, p1, argb);
        }

        for (int i = 0; i < seg; i++) {
            double a1 = 2 * Math.PI * i / seg;
            double a2 = 2 * Math.PI * (i + 1) / seg;
            float x1 = (float)(Math.cos(a1) * innerR);
            float z1 = (float)(Math.sin(a1) * innerR);
            float x2 = (float)(Math.cos(a2) * innerR);
            float z2 = (float)(Math.sin(a2) * innerR);

            float[] p1 = rot(px, py, pz, yaw, pitch, scale, cam, x1, brimY, z1);
            float[] p2 = rot(px, py, pz, yaw, pitch, scale, cam, x2, brimY, z2);
            float[] p3 = rot(px, py, pz, yaw, pitch, scale, cam, 0f, coneTopY, 0f);
            submitTri(buf, poseStack, p1, p2, p3, argb);
            submitTri(buf, poseStack, p3, p2, p1, argb);
        }
    }

    // ===================== КОЛПАК =====================
    private static void drawCap(double px, double py, double pz,
                                float yaw, float pitch, float scale,
                                int argb,
                                SubmitNodeCollector buf, PoseStack poseStack, Vec3 cam) {
        float baseR = 0.24f;
        float topY  = 0.42f;
        float pomY  = 0.50f;
        int seg = 16;

        for (int i = 0; i < seg; i++) {
            double a1 = 2 * Math.PI * i / seg;
            double a2 = 2 * Math.PI * (i + 1) / seg;
            float x1 = (float)(Math.cos(a1) * baseR);
            float z1 = (float)(Math.sin(a1) * baseR);
            float x2 = (float)(Math.cos(a2) * baseR);
            float z2 = (float)(Math.sin(a2) * baseR);

            float[] p1 = rot(px, py, pz, yaw, pitch, scale, cam, x1, 0f, z1);
            float[] p2 = rot(px, py, pz, yaw, pitch, scale, cam, x2, 0f, z2);
            float[] p3 = rot(px, py, pz, yaw, pitch, scale, cam, 0f, topY, 0f);
            submitTri(buf, poseStack, p1, p2, p3, argb);
            submitTri(buf, poseStack, p3, p2, p1, argb);
        }

        drawPom(px, py, pz, pomY, yaw, pitch, scale, cam, argb, buf, poseStack);
    }

    // ===================== ЦИЛИНДР =====================
    private static void drawTopHat(double px, double py, double pz,
                                   float yaw, float pitch, float scale,
                                   int argb,
                                   SubmitNodeCollector buf, PoseStack poseStack, Vec3 cam) {
        float brimOuterR = 0.40f;
        float brimInnerR = 0.28f;
        float brimY = 0.0f;
        float topY  = 0.55f;
        int seg = 24;

        for (int i = 0; i < seg; i++) {
            double a1 = 2 * Math.PI * i / seg;
            double a2 = 2 * Math.PI * (i + 1) / seg;
            float xo1 = (float)(Math.cos(a1) * brimOuterR);
            float zo1 = (float)(Math.sin(a1) * brimOuterR);
            float xo2 = (float)(Math.cos(a2) * brimOuterR);
            float zo2 = (float)(Math.sin(a2) * brimOuterR);
            float xi1 = (float)(Math.cos(a1) * brimInnerR);
            float zi1 = (float)(Math.sin(a1) * brimInnerR);
            float xi2 = (float)(Math.cos(a2) * brimInnerR);
            float zi2 = (float)(Math.sin(a2) * brimInnerR);

            float[] p1 = rot(px, py, pz, yaw, pitch, scale, cam, xo1, brimY, zo1);
            float[] p2 = rot(px, py, pz, yaw, pitch, scale, cam, xo2, brimY, zo2);
            float[] p3 = rot(px, py, pz, yaw, pitch, scale, cam, xi2, brimY, zi2);
            float[] p4 = rot(px, py, pz, yaw, pitch, scale, cam, xi1, brimY, zi1);
            submitQuad(buf, poseStack, p1, p2, p3, p4, argb);
            submitQuad(buf, poseStack, p4, p3, p2, p1, argb);
        }

        for (int i = 0; i < seg; i++) {
            double a1 = 2 * Math.PI * i / seg;
            double a2 = 2 * Math.PI * (i + 1) / seg;
            float x1 = (float)(Math.cos(a1) * brimInnerR);
            float z1 = (float)(Math.sin(a1) * brimInnerR);
            float x2 = (float)(Math.cos(a2) * brimInnerR);
            float z2 = (float)(Math.sin(a2) * brimInnerR);

            float[] p1 = rot(px, py, pz, yaw, pitch, scale, cam, x1, brimY, z1);
            float[] p2 = rot(px, py, pz, yaw, pitch, scale, cam, x2, brimY, z2);
            float[] p3 = rot(px, py, pz, yaw, pitch, scale, cam, x2, topY, z2);
            float[] p4 = rot(px, py, pz, yaw, pitch, scale, cam, x1, topY, z1);
            submitQuad(buf, poseStack, p1, p2, p3, p4, argb);
            submitQuad(buf, poseStack, p4, p3, p2, p1, argb);
        }

        for (int i = 0; i < seg; i++) {
            double a1 = 2 * Math.PI * i / seg;
            double a2 = 2 * Math.PI * (i + 1) / seg;
            float x1 = (float)(Math.cos(a1) * brimInnerR);
            float z1 = (float)(Math.sin(a1) * brimInnerR);
            float x2 = (float)(Math.cos(a2) * brimInnerR);
            float z2 = (float)(Math.sin(a2) * brimInnerR);

            float[] center = rot(px, py, pz, yaw, pitch, scale, cam, 0f, topY, 0f);
            float[] p1 = rot(px, py, pz, yaw, pitch, scale, cam, x1, topY, z1);
            float[] p2 = rot(px, py, pz, yaw, pitch, scale, cam, x2, topY, z2);
            submitTri(buf, poseStack, center, p1, p2, argb);
            submitTri(buf, poseStack, center, p2, p1, argb);
        }
    }

    // ===================== ПОМПОН =====================
    private static void drawPom(double px, double py, double pz, float localY,
                                float yaw, float pitch, float scale, Vec3 cam, int argb,
                                SubmitNodeCollector buf, PoseStack poseStack) {
        float r = 0.10f;
        float[][] v = {
                {0, -r * 1.5f, 0}, {0, r * 1.5f, 0},
                {r, 0, 0}, {-r, 0, 0},
                {0, 0, r}, {0, 0, -r}
        };
        int[][] faces = {
                {0,2,4}, {0,4,3}, {0,3,5}, {0,5,2},
                {1,4,2}, {1,3,4}, {1,5,3}, {1,2,5}
        };
        for (int[] f : faces) {
            float[] p1 = rot(px, py, pz, yaw, pitch, scale, cam,
                    v[f[0]][0], localY + v[f[0]][1], v[f[0]][2]);
            float[] p2 = rot(px, py, pz, yaw, pitch, scale, cam,
                    v[f[1]][0], localY + v[f[1]][1], v[f[1]][2]);
            float[] p3 = rot(px, py, pz, yaw, pitch, scale, cam,
                    v[f[2]][0], localY + v[f[2]][1], v[f[2]][2]);
            submitTri(buf, poseStack, p1, p2, p3, argb);
            submitTri(buf, poseStack, p3, p2, p1, argb);
        }
    }
}