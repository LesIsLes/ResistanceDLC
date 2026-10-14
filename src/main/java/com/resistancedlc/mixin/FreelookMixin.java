package com.resistancedlc.mixin;

import com.resistancedlc.FreelookManager;
import com.resistancedlc.config.ModConfig;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public class FreelookMixin {

    @Unique
    private double resistancedlc$lastX = Double.NaN;

    @Unique
    private double resistancedlc$lastY = Double.NaN;

    @Inject(method = "onMove(JDD)V", at = @At("HEAD"), cancellable = true, require = 0)
    private void onMove(long window, double xpos, double ypos, CallbackInfo ci) {
        if (!ModConfig.freelookEnabled) {
            resistancedlc$lastX = Double.NaN;
            resistancedlc$lastY = Double.NaN;
            return;
        }

        if (!FreelookManager.isActive()) {
            resistancedlc$lastX = Double.NaN;
            resistancedlc$lastY = Double.NaN;
            return;
        }

        if (Double.isNaN(resistancedlc$lastX) || Double.isNaN(resistancedlc$lastY)) {
            resistancedlc$lastX = xpos;
            resistancedlc$lastY = ypos;
            return;
        }

        double dx = xpos - resistancedlc$lastX;
        double dy = ypos - resistancedlc$lastY;
        resistancedlc$lastX = xpos;
        resistancedlc$lastY = ypos;

        FreelookManager.onMouseMove(dx, dy);

        ci.cancel();
    }
}