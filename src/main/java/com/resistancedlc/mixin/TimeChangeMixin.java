package com.resistancedlc.mixin;

import com.resistancedlc.TimeChangeManager;
import net.minecraft.client.ClientClockManager;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.clock.WorldClock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * TimeChangeMixin — подменяет время суток на клиенте (26.1).
 *
 * В 26.1 время хранится в ClientClockManager.
 * Миксин на getTotalTicks(Holder<WorldClock>) — подменяет только
 * для overworld-часов, если включён ModConfig.timeChangeEnabled.
 */
@Mixin(ClientClockManager.class)
public class TimeChangeMixin {

    private static final Identifier OVERWORLD_CLOCK_ID =
            Identifier.fromNamespaceAndPath("minecraft", "overworld");

    @Inject(method = "getTotalTicks", at = @At("HEAD"), cancellable = true)
    private void onGetTotalTicks(Holder<WorldClock> definition,
                                 CallbackInfoReturnable<Long> cir) {
        long fake = TimeChangeManager.getFakeDayTime();
        if (fake < 0) return;

        if (definition == null || definition.unwrapKey().isEmpty()) return;
        ResourceKey<WorldClock> key = definition.unwrapKey().get();

        if (!key.identifier().equals(OVERWORLD_CLOCK_ID)) return;

        cir.setReturnValue(fake);
    }
}