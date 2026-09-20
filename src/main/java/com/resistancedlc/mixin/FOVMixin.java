package com.resistancedlc.mixin;

import com.resistancedlc.config.ModConfig;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
public class FOVMixin {

    @Inject(method = "getFov", at = @At("RETURN"), cancellable = true)
    private void onGetFov(Camera camera, float tickDelta, boolean changingFov, CallbackInfoReturnable<Float> cir) {
        float originalFov = cir.getReturnValue();

        // FOV множитель (растяг)
        if (ModConfig.fovEnabled) {
            originalFov = originalFov * ModConfig.fovMultiplier;
        }

        // Zoom
        if (ModConfig.zoomEnabled) {
            originalFov = originalFov * ModConfig.currentZoom;
        }

        cir.setReturnValue(originalFov);
    }
}