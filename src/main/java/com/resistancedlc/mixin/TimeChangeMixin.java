package com.resistancedlc.mixin;

import com.resistancedlc.TimeChangeManager;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * TimeChangeMixin — подменяет время суток на клиенте.
 *
 * getDayTime() объявлен в Level (не в ClientLevel).
 * getTimeOfDay(float) в 1.21.11 ОТСУТСТВУЕТ — но нам он не нужен,
 * потому что всё время в игре считается через getDayTime().
 */
@Mixin(Level.class)
public class TimeChangeMixin {

    @Inject(method = "getDayTime", at = @At("HEAD"), cancellable = true)
    private void onGetDayTime(CallbackInfoReturnable<Long> cir) {
        long fake = TimeChangeManager.getFakeDayTime();
        if (fake >= 0) {
            cir.setReturnValue(fake);
        }
    }
}