package com.resistancedlc.mixin;

import com.resistancedlc.ZoomManager;
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
    private void onGetFov(Camera camera, float tickDelta, boolean changingFov,
                          CallbackInfoReturnable<Float> cir) {
        float originalFov = cir.getReturnValue();

        // FOV множитель (растяг)
        if (ModConfig.fovEnabled) {
            originalFov = originalFov * ModConfig.fovMultiplier;
        }

        // Zoom — обновляем на КАЖДОМ кадре (frame-rate independent)
        if (ModConfig.zoomEnabled) {
            // getFov вызывается каждый кадр → интерполируем здесь
            ZoomManager.updatePerFrame(tickDelta);
            originalFov = originalFov * ZoomManager.getCurrentZoom();
        }

        cir.setReturnValue(originalFov);
    }
}