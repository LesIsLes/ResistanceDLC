package com.resistancedlc.mixin;

import com.resistancedlc.config.ModConfig;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.ItemEntityRenderer;
import net.minecraft.client.renderer.entity.state.ItemEntityRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemEntityRenderer.class)
public class ItemEntityRendererMixin {

    @Inject(method = "submit", at = @At("HEAD"), require = 0)
    private void onRender(ItemEntityRenderState state, PoseStack poseStack,
                          SubmitNodeCollector collector,
                          CameraRenderState cameraState,
                          CallbackInfo ci) {
        if (!ModConfig.itemPhysicsEnabled) return;

        // Обнуляем анимацию — предмет не крутится и не качается
        state.ageInTicks = 0.0f;
        state.bobOffset = 0.0f;

        // Наклоняем на 90° — предмет ложится плашмя
        poseStack.mulPose(Axis.XP.rotationDegrees(90.0f));

        // Опускаем на землю — компенсируем наклон
        poseStack.translate(0.0f, -0.2f, 0.0f);
    }
}