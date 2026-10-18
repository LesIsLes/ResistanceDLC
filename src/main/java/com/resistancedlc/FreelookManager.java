package com.resistancedlc;

import com.resistancedlc.config.ModConfig;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;

/**
 * FreelookManager — свободная камера СЗАДИ тела.
 *
 * При зажатии клавиши:
 *   - CameraType принудительно THIRD_PERSON_BACK
 *   - камера СЗАДИ тела на расстоянии freelookDistance
 *   - вращается мышкой вокруг тела (yaw/pitch)
 *   - тело НЕ вращается
 *
 * При отпускании:
 *   - возвращаем исходный CameraType
 *
 * Позиция камеры считается от ИНТЕРПОЛИРОВАННОЙ позиции глаз игрока
 * (getEyePosition(partialTick)), чтобы не было дрожания.
 */
public final class FreelookManager {

    private static boolean active = false;
    private static CameraType savedCameraType = null;

    private static float cameraYaw = 0.0f;
    private static float cameraPitch = 0.0f;

    private FreelookManager() {}

    // ===================== TICK =====================

    public static void tick() {
        if (!ModConfig.freelookEnabled) {
            if (active) setActive(false);
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            if (active) setActive(false);
            return;
        }

        // Отключаем при открытых экранах (чат, GUI)
        if (mc.gui.screen() != null) {
            if (active) setActive(false);
            return;
        }

        boolean shouldBeActive = KeyBindings.freelookKey != null
                && KeyBindings.freelookKey.isDown();

        if (shouldBeActive != active) {
            setActive(shouldBeActive);
        }
    }

    // ===================== ACTIVATION =====================

    public static void setActive(boolean value) {
        if (active == value) return;
        active = value;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        if (active) {
            cameraYaw = mc.player.getYRot();
            cameraPitch = mc.player.getXRot();

            // Принудительно 3-е лицо
            savedCameraType = mc.options.getCameraType();
            if (savedCameraType.isFirstPerson()) {
                mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
            }
        } else {
            cameraYaw = 0.0f;
            cameraPitch = 0.0f;

            // Возвращаем исходный CameraType
            if (savedCameraType != null) {
                mc.options.setCameraType(savedCameraType);
                savedCameraType = null;
            }
        }
    }

    // ===================== MOUSE UPDATE =====================

    public static void onMouseMove(double dx, double dy) {
        if (!active) return;
        if (!ModConfig.freelookEnabled) return;

        float sensitivity = ModConfig.freelookSensitivity;

        cameraYaw += (float) dx * 0.15f * sensitivity;
        cameraPitch += (float) dy * 0.15f * sensitivity;

        if (cameraPitch > 90.0f) cameraPitch = 90.0f;
        if (cameraPitch < -90.0f) cameraPitch = -90.0f;

        cameraYaw = ((cameraYaw % 360.0f) + 360.0f) % 360.0f;
    }

    // ===================== GETTERS =====================

    public static boolean isActive() {
        return active;
    }

    public static float getCameraYaw() {
        return cameraYaw;
    }

    public static float getCameraPitch() {
        return cameraPitch;
    }

    /**
     * Позиция камеры СЗАДИ тела на расстоянии freelookDistance.
     *
     * Использует ИНТЕРПОЛИРОВАННУЮ позицию глаз игрока
     * (getEyePosition(partialTick)) — иначе камера дрожит.
     */
    public static Vec3 getCameraPosition(float partialTick) {
        if (!active) return null;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return null;

        Vec3 eye = mc.player.getEyePosition(partialTick);
        double distance = ModConfig.freelookDistance;

        double yawRad = Math.toRadians(cameraYaw);
        double pitchRad = Math.toRadians(cameraPitch);

        double lookX = -Math.sin(yawRad) * Math.cos(pitchRad);
        double lookY = -Math.sin(pitchRad);
        double lookZ = Math.cos(yawRad) * Math.cos(pitchRad);

        Vec3 desired = new Vec3(
                eye.x - lookX * distance,
                eye.y - lookY * distance,
                eye.z - lookZ * distance
        );

        // Коллизия с блоками: рейкаст от глаз до желаемой позиции
        net.minecraft.world.level.ClipContext ctx =
                new net.minecraft.world.level.ClipContext(
                        eye, desired,
                        net.minecraft.world.level.ClipContext.Block.COLLIDER,
                        net.minecraft.world.level.ClipContext.Fluid.NONE,
                        mc.player
                );
        net.minecraft.world.phys.BlockHitResult hit = mc.level.clip(ctx);

        if (hit.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK) {
            // Отступаем от блока на 0.15 в сторону игрока
            Vec3 hitPos = hit.getLocation();
            Vec3 toEye = eye.subtract(hitPos).normalize();
            return hitPos.add(toEye.scale(0.15));
        }

        return desired;
    }

    public static void reset() {
        if (active) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null && savedCameraType != null) {
                mc.options.setCameraType(savedCameraType);
            }
        }
        active = false;
        cameraYaw = 0.0f;
        cameraPitch = 0.0f;
        savedCameraType = null;
    }
}