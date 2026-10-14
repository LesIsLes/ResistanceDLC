package com.resistancedlc.mixin;

import com.resistancedlc.environment.CloudControlManager;
import net.minecraft.client.renderer.CloudRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(CloudRenderer.class)
public class CloudControlMixin {

    @ModifyVariable(
            method = "render",
            at = @At("HEAD"),
            argsOnly = true,
            index = 1
    )
    private int resistancedlc$modifyCloudColor(int color) {
        return CloudControlManager.modifyColor(color);
    }

    @ModifyVariable(
            method = "render",
            at = @At("HEAD"),
            argsOnly = true,
            index = 3
    )
    private float resistancedlc$modifyCloudHeight(float bottomY) {
        return CloudControlManager.modifyHeight(bottomY);
    }

    @ModifyVariable(
            method = "render",
            at = @At("HEAD"),
            argsOnly = true,
            index = 6
    )
    private long resistancedlc$modifyCloudSpeed(long gameTime) {
        return CloudControlManager.modifyGameTime(gameTime);
    }
}