package com.resistancedlc.mixin;

import com.resistancedlc.config.ModConfig;

import com.mojang.blaze3d.vertex.PoseStack;
import com.resistancedlc.MyCustomScreen;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.ScreenEffectRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ScreenEffectRenderer.class)
public class LowFireMixin {

    @Inject(method = "renderFire", at = @At("HEAD"))
    private static void onRenderFire(PoseStack poseStack,
                                     MultiBufferSource multiBufferSource,
                                     TextureAtlasSprite textureAtlasSprite,
                                     CallbackInfo ci) {
        if (ModConfig.lowFireEnabled) {
            poseStack.translate(0.0, -ModConfig.lowFireOffset, 0.0);
        }
    }
}
