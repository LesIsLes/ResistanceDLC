package com.resistancedlc.mixin;

import com.resistancedlc.config.ModConfig;

import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import com.resistancedlc.MyCustomScreen;

@Mixin(GameRenderer.class)
public class AspectRatioMixin {

    @Inject(method = "getFov", at = @At("RETURN"), cancellable = true)
    private void onGetFov(Camera camera, float tickDelta, boolean changingFov, CallbackInfoReturnable<Float> cir) {
        float originalFov = cir.getReturnValue();

        // Aspect Ratio (растяг)
        if (ModConfig.aspectRatioEnabled) {
            originalFov = originalFov * ModConfig.aspectRatio;
        }

        // Zoom
        if (ModConfig.zoomEnabled) {
            originalFov = originalFov * ModConfig.currentZoom;
        }

        cir.setReturnValue(originalFov);
    }
}