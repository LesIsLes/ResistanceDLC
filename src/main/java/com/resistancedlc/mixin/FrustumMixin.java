package com.resistancedlc.mixin;

import com.resistancedlc.FreelookManager;
import com.resistancedlc.config.ModConfig;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Frustum.class)
public class FrustumMixin {
    @Inject(method = "isVisible", at = @At("HEAD"), cancellable = true)
    private void resistancedlc$alwaysVisibleInFreelook(AABB box, CallbackInfoReturnable<Boolean> cir) {
        if (ModConfig.freelookEnabled && FreelookManager.isActive()) {
            cir.setReturnValue(true);
        }
    }
}