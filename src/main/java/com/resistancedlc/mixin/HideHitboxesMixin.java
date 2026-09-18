package com.resistancedlc.mixin;

import com.resistancedlc.config.ModConfig;
import net.minecraft.client.renderer.debug.EntityHitboxDebugRenderer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * HideHitboxesMixin — скрывает ванильные debug-хитбоксы (F3+B),
 * когда TargetEsp активен и включена настройка targetEspHideHitboxes.
 */
@Mixin(EntityHitboxDebugRenderer.class)
public class HideHitboxesMixin {

    @Inject(method = "showHitboxes", at = @At("HEAD"), cancellable = true, require = 0)
    private void onShowHitboxes(Entity entity, float partialTick, boolean isServerEntity, CallbackInfo ci) {
        if (ModConfig.targetEspEnabled && ModConfig.targetEspHideHitboxes) {
            ci.cancel();
        }
    }
}