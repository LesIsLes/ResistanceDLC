package com.resistancedlc.mixin;

import com.resistancedlc.config.ModConfig;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandRenderer.class)
public class LowShieldMixin {

    @Inject(method = "submitArmWithItem", at = @At("HEAD"))
    private void onRenderArmWithItem(AbstractClientPlayer player,
                                     float frameInterp, float xRot,
                                     InteractionHand hand,
                                     float attack, ItemStack itemStack, float inverseArmHeight,
                                     PoseStack poseStack,
                                     SubmitNodeCollector submitNodeCollector,
                                     int lightCoords,
                                     CallbackInfo ci) {
        if (ModConfig.lowShieldEnabled) {
            poseStack.translate(0.0, -ModConfig.lowShieldOffset, 0.0);
        }
    }
}