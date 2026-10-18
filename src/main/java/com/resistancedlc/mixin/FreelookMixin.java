package com.resistancedlc.mixin;

import com.resistancedlc.FreelookManager;
import com.resistancedlc.config.ModConfig;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public class FreelookMixin {

    // Ванильные поля "последней позиции мыши". При канесле onMove они не обновляются,
    // что накапливает огромную дельту при отпускании freelook → рывок камеры.
    @Shadow private double xpos;
    @Shadow private double ypos;

    @Unique private double resistancedlc$lastX = Double.NaN;
    @Unique private double resistancedlc$lastY = Double.NaN;
    @Unique private boolean resistancedlc$wasActive = false;

    @Inject(method = "onMove(JDD)V", at = @At("HEAD"), cancellable = true)
    private void onMove(long window, double xposArg, double yposArg, CallbackInfo ci) {
        boolean isActive = ModConfig.freelookEnabled && FreelookManager.isActive();

        if (!isActive) {
            if (resistancedlc$wasActive) {
                // Только что отпустили freelook — синхронизируем позицию,
                // чтобы ванильный расчёт дельты на следующем событии был ~0.
                this.xpos = xposArg;
                this.ypos = yposArg;
                resistancedlc$wasActive = false;
                resistancedlc$lastX = Double.NaN;
                resistancedlc$lastY = Double.NaN;
            }
            return;
        }

        resistancedlc$wasActive = true;

        if (Double.isNaN(resistancedlc$lastX) || Double.isNaN(resistancedlc$lastY)) {
            resistancedlc$lastX = xposArg;
            resistancedlc$lastY = yposArg;
            this.xpos = xposArg;
            this.ypos = yposArg;
            ci.cancel();
            return;
        }

        double dx = xposArg - resistancedlc$lastX;
        double dy = yposArg - resistancedlc$lastY;
        resistancedlc$lastX = xposArg;
        resistancedlc$lastY = yposArg;

        FreelookManager.onMouseMove(dx, dy);

        // КРИТИЧНО: держим ванильные поля в актуальном состоянии
        this.xpos = xposArg;
        this.ypos = yposArg;

        ci.cancel();
    }
}