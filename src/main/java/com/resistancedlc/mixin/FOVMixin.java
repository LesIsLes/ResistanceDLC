package com.resistancedlc.mixin;

import com.resistancedlc.ZoomManager;
import com.resistancedlc.config.ModConfig;
import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * FOVMixin — модификация FOV (растяг + Zoom).
 * В 26.1 FOV считается в Camera#calculateFov, а не в GameRenderer#getFov.
 */
@Mixin(Camera.class)
public class FOVMixin {

    @Inject(method = "calculateFov", at = @At("RETURN"), cancellable = true)
    private void onCalculateFov(CallbackInfoReturnable<Float> cir) {
        float originalFov = cir.getReturnValue();

        // FOV множитель (растяг)
        if (ModConfig.fovEnabled) {
            originalFov = originalFov * ModConfig.fovMultiplier;
        }

        // Zoom — обновляем на КАЖДОМ кадре (frame-rate independent)
        if (ModConfig.zoomEnabled) {
            ZoomManager.updatePerFrame(1.0f); // tickDelta может быть недоступен
            originalFov = originalFov * ZoomManager.getCurrentZoom();
        }

        cir.setReturnValue(originalFov);
    }
}