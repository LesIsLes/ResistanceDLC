package com.resistancedlc.mixin;

import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import com.resistancedlc.MyCustomScreen;

@Mixin(GameRenderer.class)
public class AspectRatioMixin {

    // getFov возвращает Float в 1.21.11, а не Double
    @Inject(method = "getFov", at = @At("RETURN"), cancellable = true)
    private void onGetFov(Camera camera, float tickDelta, boolean changingFov, CallbackInfoReturnable<Float> cir) {
        if (MyCustomScreen.aspectRatioEnabled) {
            float originalFov = cir.getReturnValue();
            cir.setReturnValue(originalFov * MyCustomScreen.aspectRatio);
        }
    }
}