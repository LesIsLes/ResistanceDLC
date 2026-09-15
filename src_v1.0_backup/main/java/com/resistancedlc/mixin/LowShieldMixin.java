package com.resistancedlc.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.resistancedlc.MyCustomScreen;
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

    @Inject(method = "renderArmWithItem", at = @At("HEAD"))
    private void onRenderArmWithItem(AbstractClientPlayer abstractClientPlayer,
                                     float f, float g,
                                     InteractionHand interactionHand,
                                     float h, ItemStack itemStack, float i,
                                     PoseStack poseStack,
                                     SubmitNodeCollector submitNodeCollector,
                                     int j,
                                     CallbackInfo ci) {
        if (MyCustomScreen.lowShieldEnabled) {
            poseStack.translate(0.0, -MyCustomScreen.lowShieldOffset, 0.0);
        }
    }
}