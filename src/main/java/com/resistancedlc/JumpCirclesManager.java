package com.resistancedlc;

import com.resistancedlc.config.ModConfig;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * JumpCirclesManager — кольца/фигуры под ногами при прыжке.
 *
 * Логика:
 *   1. Каждый тик проверяем, оторвался ли игрок от земли
 *   2. Если да (и нажата кнопка прыжка) — создаём фигуру в точке прыжка
 *   3. Рисуем горизонтально (лежит на земле) через ЛИНИИ (без текстур)
 *   4. Фигура живёт LiveTime секунд, fade-out через alpha
 *   5. Радиус растёт от 0 до 0.5 блока
 *
 * Стили: circle / hexagon / star
 * Все стили рисуются линиями, без .png текстур.
 */
public final class JumpCirclesManager {

    private static final List<JumpCircle> ACTIVE = new ArrayList<>();
    private static boolean wasJumping = false;

    /** Максимальный радиус в блоках — 0.5 */
    private static final double MAX_RADIUS = 0.5;

    private static final RenderType LINES_TYPE = RenderType.create(
            "resistancedlc_jump_circles_lines",
            RenderSetup.builder(RenderPipelines.LINES).createRenderSetup()
    );

    private JumpCirclesManager() {}

    // ===================== ТИК =====================

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

        // Детект прыжка: только что оторвались от земли + нажата клавиша прыжка
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

    // ===================== РЕНДЕР =====================

    public static void render(WorldRenderContext context) {
        if (!ModConfig.jumpCirclesEnabled) return;
        if (ACTIVE.isEmpty()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        MultiBufferSource.BufferSource bufferSource =
                (MultiBufferSource.BufferSource) context.consumers();
        if (bufferSource == null) return;

        Vec3 cameraPos = mc.gameRenderer.getMainCamera().position();
        PoseStack poseStack = context.matrices();
        long now = System.currentTimeMillis();

        VertexConsumer consumer = bufferSource.getBuffer(LINES_TYPE);

        for (JumpCircle circle : ACTIVE) {
            renderCircle(circle, poseStack, consumer, cameraPos, now);
        }

        bufferSource.endBatch(LINES_TYPE);
    }

    private static void renderCircle(JumpCircle circle, PoseStack poseStack,
                                     VertexConsumer consumer, Vec3 cameraPos, long now) {
        long age = now - circle.startTime;
        long lifeTime = circle.lifeTimeMs;
        float progress = Math.min(1.0f, (float) age / lifeTime);

        // Alpha: базовый + fade-out
        float alpha = ModConfig.jumpCirclesAlpha / 255.0f;
        if (ModConfig.jumpCirclesFadeOut) {
            alpha *= (1.0f - progress);
        }

        // Радиус: растёт от 0 до MAX_RADIUS * scale за первые 30% жизни
        float scale = ModConfig.jumpCirclesScale;
        float sizeAnim;
        if (progress < 0.3f) {
            sizeAnim = progress / 0.3f;
        } else {
            sizeAnim = 1.0f;
        }
        double radius = MAX_RADIUS * scale * sizeAnim;

        // Вращение
        float rotation = (now / 1000.0f) * ModConfig.jumpCirclesSpinSpeed * 360.0f;
        double rotRad = Math.toRadians(rotation);

        // ARGB цвет
        int baseColor = ModConfig.jumpCirclesColor;
        int r = (baseColor >> 16) & 0xFF;
        int g = (baseColor >> 8) & 0xFF;
        int b = baseColor & 0xFF;
        int a = (int) (alpha * 255);
        int argb = (a << 24) | (r << 16) | (g << 8) | b;

        // Позиция относительно камеры
        double cx = circle.position.x - cameraPos.x;
        double cy = circle.position.y - cameraPos.y + 0.02;  // чуть выше блока, чтобы не z-fight
        double cz = circle.position.z - cameraPos.z;

        float lineWidth = ModConfig.jumpCirclesLineWidth;

        switch (circle.style) {
            case "hexagon" -> drawHexagon(consumer, poseStack, cx, cy, cz, radius, rotRad, argb, lineWidth);
            case "star" -> drawStar(consumer, poseStack, cx, cy, cz, radius, rotRad, argb, lineWidth);
            default -> drawCircle(consumer, poseStack, cx, cy, cz, radius, rotRad, argb, lineWidth);
        }
    }

    // ===================== ФИГУРЫ =====================

    /** Окружность — 48 сегментов. */
    private static void drawCircle(VertexConsumer consumer, PoseStack poseStack,
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
            line(consumer, poseStack, x1, cy, z1, x2, cy, z2, color, lineWidth);
        }
    }

    /** Шестиугольник — 6 сегментов. */
    private static void drawHexagon(VertexConsumer consumer, PoseStack poseStack,
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
            line(consumer, poseStack, x1, cy, z1, x2, cy, z2, color, lineWidth);
        }
    }

    /** Звезда — 5 вершин, 10 сегментов (чередование внешних и внутренних точек). */
    private static void drawStar(VertexConsumer consumer, PoseStack poseStack,
                                 double cx, double cy, double cz,
                                 double radius, double rotation,
                                 int color, float lineWidth) {
        int points = 5;
        double innerRadius = radius * 0.4;   // внутренний радиус звезды
        int totalVertices = points * 2;      // 5 внешних + 5 внутренних

        for (int i = 0; i < totalVertices; i++) {
            double r1 = (i % 2 == 0) ? radius : innerRadius;
            double r2 = ((i + 1) % 2 == 0) ? radius : innerRadius;
            double a1 = rotation + i * Math.PI * 2 / totalVertices;
            double a2 = rotation + (i + 1) * Math.PI * 2 / totalVertices;
            double x1 = cx + Math.cos(a1) * r1;
            double z1 = cz + Math.sin(a1) * r1;
            double x2 = cx + Math.cos(a2) * r2;
            double z2 = cz + Math.sin(a2) * r2;
            line(consumer, poseStack, x1, cy, z1, x2, cy, z2, color, lineWidth);
        }
    }

    // ===================== ЛИНИЯ =====================

    private static void line(VertexConsumer consumer, PoseStack poseStack,
                             double x1, double y1, double z1,
                             double x2, double y2, double z2,
                             int color, float lineWidth) {
        Vector3f normal = new Vector3f(
                (float)(x2 - x1),
                (float)(y2 - y1),
                (float)(z2 - z1)
        );
        if (normal.lengthSquared() > 0.000001f) {
            normal.normalize();
        }

        PoseStack.Pose pose = poseStack.last();

        consumer.addVertex(pose, (float)x1, (float)y1, (float)z1)
                .setColor(color)
                .setNormal(pose, normal)
                .setLineWidth(lineWidth);

        consumer.addVertex(pose, (float)x2, (float)y2, (float)z2)
                .setColor(color)
                .setNormal(pose, normal)
                .setLineWidth(lineWidth);
    }

    // ===================== RESET =====================

    public static void reset() {
        ACTIVE.clear();
        wasJumping = false;
    }

    // ===================== RECORD =====================

    private record JumpCircle(
            Vec3 position,
            long startTime,
            long lifeTimeMs,
            String style
    ) {
        boolean isExpired() {
            return System.currentTimeMillis() - startTime > lifeTimeMs;
        }
    }
}