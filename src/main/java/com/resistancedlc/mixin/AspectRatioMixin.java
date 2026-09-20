package com.resistancedlc.mixin;

import com.resistancedlc.config.ModConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
public class AspectRatioMixin {

    @Inject(method = "getProjectionMatrix", at = @At("RETURN"), cancellable = true)
    private void onGetProjectionMatrix(float fov, CallbackInfoReturnable<Matrix4f> cir) {
        if (!ModConfig.aspectRatioEnabled) return;

        float aspect = ModConfig.aspectRatioUsePreset
                ? ModConfig.aspectRatioPreset.getRatio()
                : ModConfig.aspectRatioFactor;

        if (Math.abs(aspect - 1.0f) < 0.001f) return;

        Minecraft mc = Minecraft.getInstance();
        float windowAspect = (float) mc.getWindow().getWidth() / mc.getWindow().getHeight();
        float modifiedAspect = windowAspect * aspect;

        Matrix4f modified = new Matrix4f().perspective(
                fov * ((float) Math.PI / 180F),
                modifiedAspect,
                0.05f,
                mc.gameRenderer.getDepthFar()
        );

        cir.setReturnValue(modified);
    }
}