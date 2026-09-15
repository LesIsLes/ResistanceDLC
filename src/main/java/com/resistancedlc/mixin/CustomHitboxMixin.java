package com.resistancedlc.mixin;

import com.resistancedlc.config.ModConfig;

import net.minecraft.client.renderer.debug.EntityHitboxDebugRenderer;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Custom Hitbox — подменяет цвет ванильного debug-хитбокса (F3+B)
 * на цвет из ModConfig.
 *
 * Миксинимся в приватный метод showHitboxes — он вызывается
 * для каждой сущности в зоне видимости.
 */
@Mixin(EntityHitboxDebugRenderer.class)
public class CustomHitboxMixin {

    /**
     * HEAD-инъекция: перед ванильной отрисовкой рисуем свой
     * хитбокс с нужным цветом, а ванильный отменяем через ci.cancel().
     */
    @Inject(method = "showHitboxes", at = @At("HEAD"), cancellable = true)
    private void onShowHitboxes(Entity entity, float partialTick, boolean isServerEntity, CallbackInfo ci) {
        if (!ModConfig.customHitboxEnabled) return;
        if (entity == null) return;

        // Считаем позицию с интерполяцией
        Vec3 pos = entity.position();
        Vec3 posInterp = entity.getPosition(partialTick);
        Vec3 delta = posInterp.subtract(pos);

        AABB box = entity.getBoundingBox().move(delta);

        // Наш цвет с alpha
        int baseColor = ModConfig.customHitboxColor;
        int alpha = ModConfig.customHitboxAlpha;
        int r = (baseColor >> 16) & 0xFF;
        int g = (baseColor >> 8) & 0xFF;
        int b = baseColor & 0xFF;
        int argb = (alpha << 24) | (r << 16) | (g << 8) | b;

        // Рисуем свой хитбокс
        Gizmos.cuboid(box, GizmoStyle.stroke(argb));

        // Точка центра (можно убрать, если не нужна)
        Gizmos.point(posInterp, argb, 2.0F);

        // Отменяем ванильную отрисовку
        ci.cancel();
    }
}