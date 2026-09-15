package com.resistancedlc.mixin;

import com.resistancedlc.config.ModConfig;

import com.mojang.blaze3d.vertex.PoseStack;
import com.resistancedlc.MyCustomScreen;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class NoHurtCamMixin {

    @Inject(method = "bobHurt", at = @At("HEAD"), cancellable = true)
    private void onTiltViewWhenHurt(PoseStack poseStack, float f, CallbackInfo ci) {
        if (ModConfig.noHurtCamEnabled) {
            ci.cancel();
        }
    }
}