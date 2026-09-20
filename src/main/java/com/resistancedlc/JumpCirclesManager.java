package com.resistancedlc;

import com.resistancedlc.config.ModConfig;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

/**
 * JumpCirclesManager — кольца под ногами при прыжке.
 *
 * Логика:
 *   1. Каждый тик проверяем, оторвался ли игрок от земли
 *   2. Если да (и нажата кнопка прыжка) — создаём круг в точке прыжка
 *   3. Рендерим горизонтально (лежит на земле)
 *   4. Круг живёт LiveTime секунд, fade-out через alpha
 *
 * Текстуры: assets/resistancedlc/textures/jump_circles/{circle,hexagon,portal}.png
 */
public final class JumpCirclesManager {

    private static final List<JumpCircle> ACTIVE = new ArrayList<>();
    private static boolean wasJumping = false;

    private static final Identifier[] TEXTURES = {
            Identifier.fromNamespaceAndPath(ResistanceDLC.MOD_ID, "textures/jump_circles/circle.png"),
            Identifier.fromNamespaceAndPath(ResistanceDLC.MOD_ID, "textures/jump_circles/hexagon.png"),
            Identifier.fromNamespaceAndPath(ResistanceDLC.MOD_ID, "textures/jump_circles/portal.png")
    };

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

        // Удаляем просроченные круги
        ACTIVE.removeIf(JumpCircle::isExpired);
    }

    private static void spawnCircle(Vec3 pos) {
        ACTIVE.add(new JumpCircle(
                new Vec3(pos.x, pos.y, pos.z),
                System.currentTimeMillis(),
                ModConfig.jumpCirclesLiveTime * 1000L,
                getSelectedTexture()
        ));
    }

    private static Identifier getSelectedTexture() {
        return switch (ModConfig.jumpCirclesStyle) {
            case "hexagon" -> TEXTURES[1];
            case "portal"  -> TEXTURES[2];
            default        -> TEXTURES[0];
        };
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

        Vec3 cameraPos = Minecraft.getInstance().gameRenderer.getMainCamera().position();
        PoseStack poseStack = context.matrices();
        long now = System.currentTimeMillis();

        for (JumpCircle circle : ACTIVE) {
            renderCircle(circle, poseStack, bufferSource, cameraPos, now);
        }

        bufferSource.endBatch();
    }

    private static void renderCircle(JumpCircle circle, PoseStack poseStack,
                                     MultiBufferSource.BufferSource bufferSource,
                                     Vec3 cameraPos, long now) {
        long age = now - circle.startTime;
        long lifeTime = circle.lifeTimeMs;
        float progress = Math.min(1.0f, (float) age / lifeTime);

        float alpha = ModConfig.jumpCirclesAlpha / 255.0f;
        float brightness = ModConfig.jumpCirclesBrightness;
        float scale = ModConfig.jumpCirclesScale;

        if (ModConfig.jumpCirclesFadeOut) {
            alpha *= (1.0f - progress);
        }

        // Анимация размера: от 0.5 до 1.0 от scale в первые 30% жизни
        float sizeAnim;
        if (progress < 0.3f) {
            sizeAnim = 0.5f + (progress / 0.3f) * 0.5f;
        } else {
            sizeAnim = 1.0f;
        }
        float finalScale = scale * sizeAnim * 2.0f; // базовый размер ~2 блока

        // Вращение
        float rotation = (now / 1000.0f) * ModConfig.jumpCirclesSpinSpeed * 360.0f;

        // Цвет: 0xFFFFFF + brightness
        int r = (int) Math.min(255, 255 * brightness);
        int g = (int) Math.min(255, 255 * brightness);
        int b = (int) Math.min(255, 255 * brightness);
        int a = (int) (alpha * 255);
        int argb = (a << 24) | (r << 16) | (g << 8) | b;

        // Позиция относительно камеры
        double x = circle.position.x - cameraPos.x;
        double y = circle.position.y - cameraPos.y + 0.02; // чуть выше блока, чтобы не z-fight
        double z = circle.position.z - cameraPos.z;

        poseStack.pushPose();
        poseStack.translate(x, y, z);
        // Горизонтально: наклоняем на 90° по X
        poseStack.mulPose(Axis.XP.rotationDegrees(90));
        // Вращение вокруг вертикальной оси (в плоскости круга)
        poseStack.mulPose(Axis.ZP.rotationDegrees(rotation));

        Matrix4f matrix = poseStack.last().pose();
        PoseStack.Pose pose = poseStack.last();

        VertexConsumer consumer = bufferSource.getBuffer(
                RenderTypes.entityTranslucent(circle.texture)
        );

        float half = finalScale / 2.0f;

        // Quad
        consumer.addVertex(matrix, -half, -half, 0)
                .setColor(argb)
                .setUv(0f, 0f)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(0xF000F0)
                .setNormal(pose, 0f, 0f, 1f);

        consumer.addVertex(matrix, -half, half, 0)
                .setColor(argb)
                .setUv(0f, 1f)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(0xF000F0)
                .setNormal(pose, 0f, 0f, 1f);

        consumer.addVertex(matrix, half, half, 0)
                .setColor(argb)
                .setUv(1f, 1f)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(0xF000F0)
                .setNormal(pose, 0f, 0f, 1f);

        consumer.addVertex(matrix, half, -half, 0)
                .setColor(argb)
                .setUv(1f, 0f)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(0xF000F0)
                .setNormal(pose, 0f, 0f, 1f);

        poseStack.popPose();
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
            Identifier texture
    ) {
        boolean isExpired() {
            return System.currentTimeMillis() - startTime > lifeTimeMs;
        }
    }
}