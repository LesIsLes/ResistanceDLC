package com.resistancedlc;

import com.mojang.blaze3d.vertex.PoseStack;
import com.resistancedlc.config.ModConfig;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

public final class JumpCirclesManager {

    private static final List<JumpCircle> ACTIVE = new ArrayList<>();
    private static boolean wasJumping = false;

    private static final double MAX_RADIUS = 0.5;

    private static final RenderType LINES_TYPE = RenderType.create(
            "resistancedlc_jump_circles_lines",
            RenderSetup.builder(RenderPipelines.LINES).createRenderSetup()
    );

    private JumpCirclesManager() {}

    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            ACTIVE.clear();
            wasJumping = false;
            return;
        }
        if (!ModConfig.jumpCirclesEnabled) {
            ACTIVE.clear();
            wasJumping = false;
            return;
        }

        boolean isJumping = !mc.player.onGround();
        if (isJumping && !wasJumping && mc.options.keyJump.isDown()) {
            spawnCircle(mc.player.position());
        }
        wasJumping = isJumping;

        ACTIVE.removeIf(JumpCircle::isExpired);
    }

    private static void spawnCircle(Vec3 pos) {
        ACTIVE.add(new JumpCircle(
                new Vec3(pos.x, pos.y, pos.z),
                System.currentTimeMillis(),
                ModConfig.jumpCirclesLiveTime * 1000L,
                ModConfig.jumpCirclesStyle
        ));
    }

    public static void render(LevelRenderContext context) {
        if (!ModConfig.jumpCirclesEnabled) return;
        if (ACTIVE.isEmpty()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        SubmitNodeCollector buf = context.submitNodeCollector();
        if (buf == null) return;

        Vec3 cameraPos = mc.gameRenderer.mainCamera().position();
        PoseStack poseStack = context.poseStack();
        long now = System.currentTimeMillis();

        for (JumpCircle circle : ACTIVE) {
            renderCircle(circle, poseStack, buf, cameraPos, now);
        }
    }

    private static void renderCircle(JumpCircle circle, PoseStack poseStack,
                                     SubmitNodeCollector buf, Vec3 cameraPos, long now) {
        long age = now - circle.startTime;
        long lifeTime = circle.lifeTimeMs;
        float progress = Math.min(1.0f, (float) age / lifeTime);

        float alpha = ModConfig.jumpCirclesAlpha / 255.0f;
        if (ModConfig.jumpCirclesFadeOut) {
            alpha *= (1.0f - progress);
        }

        float scale = ModConfig.jumpCirclesScale;
        float sizeAnim = progress < 0.3f ? progress / 0.3f : 1.0f;
        double radius = MAX_RADIUS * scale * sizeAnim;

        float rotation = (now / 1000.0f) * ModConfig.jumpCirclesSpinSpeed * 360.0f;
        double rotRad = Math.toRadians(rotation);

        int baseColor = ModConfig.jumpCirclesColor;
        int r = (baseColor >> 16) & 0xFF;
        int g = (baseColor >> 8) & 0xFF;
        int b = baseColor & 0xFF;
        int a = (int) (alpha * 255);
        int argb = (a << 24) | (r << 16) | (g << 8) | b;

        double cx = circle.position.x - cameraPos.x;
        double cy = circle.position.y - cameraPos.y + 0.02;
        double cz = circle.position.z - cameraPos.z;

        float lineWidth = ModConfig.jumpCirclesLineWidth;

        switch (circle.style) {
            case "hexagon" -> drawHexagon(buf, poseStack, cx, cy, cz, radius, rotRad, argb, lineWidth);
            case "star" -> drawStar(buf, poseStack, cx, cy, cz, radius, rotRad, argb, lineWidth);
            default -> drawCircle(buf, poseStack, cx, cy, cz, radius, rotRad, argb, lineWidth);
        }
    }

    private static void drawCircle(SubmitNodeCollector buf, PoseStack poseStack,
                                   double cx, double cy, double cz,
                                   double radius, double rotation,
                                   int color, float lineWidth) {
        int segments = 48;
        for (int i = 0; i < segments; i++) {
            double a1 = rotation + i * Math.PI * 2 / segments;
            double a2 = rotation + (i + 1) * Math.PI * 2 / segments;
            double x1 = cx + Math.cos(a1) * radius;
            double z1 = cz + Math.sin(a1) * radius;
            double x2 = cx + Math.cos(a2) * radius;
            double z2 = cz + Math.sin(a2) * radius;
            line(buf, poseStack, x1, cy, z1, x2, cy, z2, color, lineWidth);
        }
    }

    private static void drawHexagon(SubmitNodeCollector buf, PoseStack poseStack,
                                    double cx, double cy, double cz,
                                    double radius, double rotation,
                                    int color, float lineWidth) {
        int segments = 6;
        for (int i = 0; i < segments; i++) {
            double a1 = rotation + i * Math.PI * 2 / segments;
            double a2 = rotation + (i + 1) * Math.PI * 2 / segments;
            double x1 = cx + Math.cos(a1) * radius;
            double z1 = cz + Math.sin(a1) * radius;
            double x2 = cx + Math.cos(a2) * radius;
            double z2 = cz + Math.sin(a2) * radius;
            line(buf, poseStack, x1, cy, z1, x2, cy, z2, color, lineWidth);
        }
    }

    private static void drawStar(SubmitNodeCollector buf, PoseStack poseStack,
                                 double cx, double cy, double cz,
                                 double radius, double rotation,
                                 int color, float lineWidth) {
        int points = 5;
        double innerRadius = radius * 0.4;
        int totalVertices = points * 2;

        for (int i = 0; i < totalVertices; i++) {
            double r1 = (i % 2 == 0) ? radius : innerRadius;
            double r2 = ((i + 1) % 2 == 0) ? radius : innerRadius;
            double a1 = rotation + i * Math.PI * 2 / totalVertices;
            double a2 = rotation + (i + 1) * Math.PI * 2 / totalVertices;
            double x1 = cx + Math.cos(a1) * r1;
            double z1 = cz + Math.sin(a1) * r1;
            double x2 = cx + Math.cos(a2) * r2;
            double z2 = cz + Math.sin(a2) * r2;
            line(buf, poseStack, x1, cy, z1, x2, cy, z2, color, lineWidth);
        }
    }

    private static void line(SubmitNodeCollector buf, PoseStack poseStack,
                             double x1, double y1, double z1,
                             double x2, double y2, double z2,
                             int color, float lineWidth) {
        Vector3f normal = new Vector3f(
                (float)(x2 - x1), (float)(y2 - y1), (float)(z2 - z1)
        );
        if (normal.lengthSquared() > 0.000001f) normal.normalize();

        final float fx1 = (float) x1;
        final float fy1 = (float) y1;
        final float fz1 = (float) z1;
        final float fx2 = (float) x2;
        final float fy2 = (float) y2;
        final float fz2 = (float) z2;

        buf.submitCustomGeometry(poseStack, LINES_TYPE, (pose, consumer) -> {
            consumer.addVertex(pose, fx1, fy1, fz1)
                    .setColor(color)
                    .setNormal(pose, normal)
                    .setLineWidth(lineWidth);
            consumer.addVertex(pose, fx2, fy2, fz2)
                    .setColor(color)
                    .setNormal(pose, normal)
                    .setLineWidth(lineWidth);
        });
    }

    public static void reset() {
        ACTIVE.clear();
        wasJumping = false;
    }

    private record JumpCircle(Vec3 position, long startTime, long lifeTimeMs, String style) {
        boolean isExpired() {
            return System.currentTimeMillis() - startTime > lifeTimeMs;
        }
    }
}