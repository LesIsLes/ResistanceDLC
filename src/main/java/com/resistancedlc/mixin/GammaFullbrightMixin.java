package com.resistancedlc.mixin;

import com.resistancedlc.config.ModConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.renderer.LightTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Field;

/**
 * GammaFullbrightMixin — обход ограничения gamma в Minecraft [0.0, 1.0].
 *
 * В 1.21.11 LightTexture.updateLightTexture(float) вычисляет:
 *   float p = ((Double)this.minecraft.options.gamma().get()).floatValue();
 * и кладёт в uniform Math.max(0.0F, p - k).
 *
 * Мы подменяем value OptionInstance.gamma() через рефлексию на время вызова
 * метода — визуальный эффект fullbright работает, а в options.txt
 * всегда остаётся безопасное 1.0.
 *
 * Правило 43: gamma в MC ограничена [0.0, 1.0] — обход только через рефлексию.
 * Правило 44: используем поле value, не initialValue.
 */
@Mixin(LightTexture.class)
public class GammaFullbrightMixin {

    @Unique
    private static Field resistancedlc$gammaValueField = null;

    @Inject(method = "updateLightTexture", at = @At("HEAD"))
    private void onUpdateLightTextureHead(float partialTick, CallbackInfo ci) {
        if (!ModConfig.gammaUtilEnabled) return;
        if (ModConfig.gammaValue <= 1.0f) return;

        applyGamma(ModConfig.gammaValue);
    }

    @Inject(method = "updateLightTexture", at = @At("RETURN"))
    private void onUpdateLightTextureReturn(float partialTick, CallbackInfo ci) {
        if (!ModConfig.gammaUtilEnabled) return;
        if (ModConfig.gammaValue <= 1.0f) return;

        applyGamma(1.0);
    }

    @Unique
    private static void applyGamma(double value) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options == null) return;

        try {
            OptionInstance<Double> gammaOption = mc.options.gamma();
            if (resistancedlc$gammaValueField == null) {
                try {
                    resistancedlc$gammaValueField = OptionInstance.class.getDeclaredField("value");
                } catch (NoSuchFieldException e) {
                    for (Field f : OptionInstance.class.getDeclaredFields()) {
                        String name = f.getName().toLowerCase();
                        if (f.getType() == Object.class && !name.contains("initial")) {
                            resistancedlc$gammaValueField = f;
                            break;
                        }
                    }
                }
                if (resistancedlc$gammaValueField == null) return;
                resistancedlc$gammaValueField.setAccessible(true);
            }
            resistancedlc$gammaValueField.set(gammaOption, value);
        } catch (Exception ignored) {
        }
    }
}