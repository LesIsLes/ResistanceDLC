package com.resistancedlc.mixin;

import com.resistancedlc.config.ModConfig;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.client.renderer.fog.FogRenderer;
import net.minecraft.world.level.material.FogType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FogRenderer.class)
public class CustomFogMixin {

    @Inject(method = "setupFog", at = @At("RETURN"), cancellable = true)
    private void resistancedlc$customFog(
            Camera camera,
            int renderDistanceInChunks,
            DeltaTracker deltaTracker,
            float darkenWorldAmount,
            ClientLevel level,
            CallbackInfoReturnable<FogData> cir
    ) {
        if (!ModConfig.customFogEnabled) return;

        FogData fog = cir.getReturnValue();
        if (fog == null) return;

        FogType fogType = camera.getFluidInCamera();
        boolean isAtmospheric = (fogType == FogType.NONE || fogType == null);
        boolean isLiquid = (fogType == FogType.WATER || fogType == FogType.LAVA);

        // Режим применения
        switch (ModConfig.customFogMode) {
            case 1: // только атмосфера
                if (!isAtmospheric) return;
                break;
            case 2: // только жидкости
                if (!isLiquid) return;
                break;
            default: // always — применяем всегда
                break;
        }

        // Цвет тумана
        int r = (ModConfig.customFogColor >> 16) & 0xFF;
        int g = (ModConfig.customFogColor >> 8) & 0xFF;
        int b = ModConfig.customFogColor & 0xFF;
        fog.color.set(r / 255.0f, g / 255.0f, b / 255.0f, 1.0f);

        // Дистанции. Ставим "сырые" значения, они уже в блоках?
        // В 26.1 setupFog задаёт renderDistanceInBlocks = renderDistanceInChunks * 16.
        // environmental* задаётся FogEnvironment (например, AtmosphericFogEnvironment).
        // Мы просто перезапишем их, используя наши относительные множители.
        float renderBlocks = renderDistanceInChunks * 16.0f;

        // environmentalStart: от 0 (у глаз) до renderBlocks
        // environmentalEnd: от environmentalStart до renderBlocks * customFogEnd
        float start = ModConfig.customFogStart * renderBlocks;
        float end = ModConfig.customFogEnd * renderBlocks;

        fog.environmentalStart = start;
        fog.environmentalEnd = end;

        // Render-distance туман
        fog.renderDistanceStart = end * 0.9f;
        fog.renderDistanceEnd = renderBlocks;

        // Sky / clouds
        if (ModConfig.customFogAffectSky) {
            fog.skyEnd = end;
        }
        if (ModConfig.customFogAffectClouds) {
            fog.cloudEnd = end * 1.2f;
        }
    }
}