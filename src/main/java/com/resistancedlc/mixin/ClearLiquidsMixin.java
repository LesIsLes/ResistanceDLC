package com.resistancedlc.mixin;

import com.resistancedlc.config.ModConfig;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.client.renderer.fog.FogRenderer;
import net.minecraft.world.level.material.FogType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(FogRenderer.class)
public class ClearLiquidsMixin {

    @Inject(
            method = "setupFog",
            at = @At("RETURN"),
            locals = LocalCapture.CAPTURE_FAILHARD
    )
    private void resistancedlc$clearLiquidsFog(
            Camera camera,
            int renderDistanceInChunks,
            net.minecraft.client.DeltaTracker deltaTracker,
            float darkenWorldAmount,
            net.minecraft.client.multiplayer.ClientLevel level,
            CallbackInfoReturnable<FogData> cir
    ) {
        if (!ModConfig.clearLiquidsEnabled) return;

        FogType fogType = camera.getFluidInCamera();
        if (fogType == null) return;

        FogData fog = cir.getReturnValue();
        if (fog == null) return;

        boolean shouldClear = false;
        if (fogType == FogType.WATER && ModConfig.clearWaterEnabled) {
            shouldClear = true;
        } else if (fogType == FogType.LAVA && ModConfig.clearLavaEnabled) {
            shouldClear = true;
        }

        if (shouldClear) {
            // Ставим огромные значения — туман становится "за горизонтом"
            fog.environmentalStart = Float.MAX_VALUE / 2.0f;
            fog.environmentalEnd   = Float.MAX_VALUE / 2.0f;
            fog.renderDistanceStart = Float.MAX_VALUE / 2.0f;
            fog.renderDistanceEnd   = Float.MAX_VALUE / 2.0f;
            // Цвет тоже можно занулить, но обычно достаточно дистанций
            // fog.color.set(0f, 0f, 0f, 1f);
        }
    }
}