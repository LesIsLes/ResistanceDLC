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
        if (mc.screen != null) {
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
        if (mc.player == null) return null;

        // Интерполированная позиция глаз игрока
        Vec3 eye = mc.player.getEyePosition(partialTick);

        double distance = ModConfig.freelookDistance;

        double yawRad = Math.toRadians(cameraYaw);
        double pitchRad = Math.toRadians(cameraPitch);

        double lookX = -Math.sin(yawRad) * Math.cos(pitchRad);
        double lookY = -Math.sin(pitchRad);
        double lookZ = Math.cos(yawRad) * Math.cos(pitchRad);

        double camX = eye.x - lookX * distance;
        double camY = eye.y - lookY * distance;
        double camZ = eye.z - lookZ * distance;

        return new Vec3(camX, camY, camZ);
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