package com.resistancedlc.mixin;

import com.resistancedlc.FreelookManager;
import com.resistancedlc.config.ModConfig;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * CameraMixin — override rotation и позиции камеры при активном Freelook.
 *
 * Сигнатура update (official Mojang mappings, 26.1):
 *   update(DeltaTracker, CallbackInfo)
 *
 * partialTick берём из DeltaTracker.
 */
@Mixin(Camera.class)
public abstract class CameraMixin implements CameraInvoker {

    @Inject(method = "update", at = @At("RETURN"))
    private void onUpdateReturn(
            DeltaTracker deltaTracker,
            CallbackInfo ci
    ) {
        if (!ModConfig.freelookEnabled) return;
        if (!FreelookManager.isActive()) return;

        float partialTick = deltaTracker.getGameTimeDeltaPartialTick(true);

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