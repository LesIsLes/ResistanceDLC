package com.resistancedlc.mixin;

import com.resistancedlc.FreelookManager;
import com.resistancedlc.config.ModConfig;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class CameraMixin {

    // Доступ к защищённым методам камеры напрямую (Minecraft 26.2)
    @org.spongepowered.asm.mixin.Shadow
    protected abstract void setPosition(double x, double y, double z);

    @org.spongepowered.asm.mixin.Shadow
    protected abstract void setRotation(float yaw, float pitch);

    @Inject(method = "update", at = @At("RETURN"))
    private void resistancedlc$overrideCamera(CallbackInfo ci) {
        if (!ModConfig.freelookEnabled || !FreelookManager.isActive()) return;

        float pt = Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false);
        Vec3 pos = FreelookManager.getCameraPosition(pt);
        if (pos != null) {
            this.setPosition(pos.x, pos.y, pos.z);
        }
        this.setRotation(FreelookManager.getCameraYaw(), FreelookManager.getCameraPitch());
    }
}