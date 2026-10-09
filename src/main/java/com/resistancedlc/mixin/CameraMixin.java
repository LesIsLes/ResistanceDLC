package com.resistancedlc.mixin;

import com.resistancedlc.FreelookManager;
import com.resistancedlc.config.ModConfig;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * CameraMixin — override rotation и позиции камеры при активном Freelook.
 *
 * Сигнатура setup (official Mojang mappings, 1.21.11):
 *   setup(Level, Entity, boolean, boolean, float, CallbackInfo)
 *
 * Используем raw значения yaw/pitch из мыши (без интерполяции),
 * а позицию берём от ИНТЕРПОЛИРОВАННОЙ позиции глаз игрока.
 */
@Mixin(Camera.class)
public abstract class CameraMixin implements CameraInvoker {

    @Inject(method = "setup", at = @At("RETURN"))
    private void onSetupReturn(
            Level level,
            Entity entity,
            boolean bl,
            boolean bl2,
            float partialTick,
            CallbackInfo ci
    ) {
        if (!ModConfig.freelookEnabled) return;
        if (!FreelookManager.isActive()) return;

        // Rotation — raw yaw/pitch (мышь даёт малые дельты)
        float yaw = FreelookManager.getCameraYaw();
        float pitch = FreelookManager.getCameraPitch();

        resistancedlc$invokeSetRotation(yaw, pitch);

        // Position — от интерполированной позиции глаз
        Vec3 camPos = FreelookManager.getCameraPosition(partialTick);
        if (camPos != null) {
            resistancedlc$invokeSetPosition(camPos.x, camPos.y, camPos.z);
        }
    }
}