package com.resistancedlc.mixin;

import com.resistancedlc.WeatherChangeManager;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * WeatherChangeMixin — override rain/thunder level на клиенте.
 *
 * getRainLevel(float) и getThunderLevel(float) объявлены в Level.
 * Если override включён — возвращаем fake, иначе ваниль.
 */
@Mixin(Level.class)
public class WeatherChangeMixin {

    @Inject(method = "getRainLevel", at = @At("HEAD"), cancellable = true)
    private void onGetRainLevel(float partialTick, CallbackInfoReturnable<Float> cir) {
        float fake = WeatherChangeManager.getFakeRainLevel();
        if (fake >= 0.0f) {
            cir.setReturnValue(fake);
        }
    }

    @Inject(method = "getThunderLevel", at = @At("HEAD"), cancellable = true)
    private void onGetThunderLevel(float partialTick, CallbackInfoReturnable<Float> cir) {
        float fake = WeatherChangeManager.getFakeThunderLevel();
        if (fake >= 0.0f) {
            cir.setReturnValue(fake);
        }
    }
}