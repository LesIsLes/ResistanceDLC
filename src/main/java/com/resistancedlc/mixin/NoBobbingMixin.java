package com.resistancedlc.mixin;

import com.resistancedlc.config.ModConfig;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class NoBobbingMixin {

    @Inject(method = "bobView", at = @At("HEAD"), cancellable = true)
    private void onBobView(CameraRenderState cameraState, PoseStack poseStack, CallbackInfo ci) {
        if (ModConfig.noBobbingEnabled) {
            ci.cancel();
        }
    }
}