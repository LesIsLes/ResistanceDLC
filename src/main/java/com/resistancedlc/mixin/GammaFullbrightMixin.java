package com.resistancedlc.mixin;

import com.resistancedlc.config.ModConfig;
import net.minecraft.client.renderer.LightmapRenderStateExtractor;
import net.minecraft.client.renderer.state.LightmapRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * GammaFullbrightMixin — fullbright в 26.1.
 *
 * В 26.1 гамма читается в LightmapRenderStateExtractor.extract() и кладётся
 * в LightmapRenderState.brightness. Шейдер lightmap использует:
 *   - brightness
 *   - nightVisionEffectIntensity
 *
 * Мы подписываемся на RETURN и подменяем оба поля, если включён GammaUtil.
 */
@Mixin(LightmapRenderStateExtractor.class)
public class GammaFullbrightMixin {

    @Inject(method = "extract", at = @At("RETURN"))
    private void onExtractReturn(LightmapRenderState renderState,
                                 float partialTicks,
                                 CallbackInfo ci) {
        if (!ModConfig.gammaUtilEnabled) return;
        if (ModConfig.gammaValue <= 1.0f) return;

        // Fullbright: максимальная яркость lightmap.
        // brightness — гамма (0..1), nightVisionEffectIntensity — night vision.
        // Ставим оба в 1.0 — это даёт максимально яркий lightmap.
        renderState.brightness = 1.0f;
        renderState.nightVisionEffectIntensity = 1.0f;
    }
}