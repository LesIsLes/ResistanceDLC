package com.resistancedlc.mixin;

import com.resistancedlc.FreelookManager;
import com.resistancedlc.config.ModConfig;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * FreelookMixin — перехватывает движение мыши при активном Freelook.
 *
 * В 1.21.11 MouseHandler.turnPlayer(double) принимает один double,
 * поэтому dx/dy ловим в onMove(long, double, double).
 *
 * При активном Freelook:
 *   - не даём ванилле вращать игрока (ci.cancel())
 *   - передаём дельты в FreelookManager для вращения камеры
 */
@Mixin(MouseHandler.class)
public class FreelookMixin {

    @Unique
    private double resistancedlc$lastX = Double.NaN;

    @Unique
    private double resistancedlc$lastY = Double.NaN;

    @Inject(method = "onMove(JDD)V", at = @At("HEAD"), cancellable = true, require = 0)
    private void onMove(long window, double xpos, double ypos, CallbackInfo ci) {
        // Если фича выключена — просто сбрасываем состояние и пропускаем
        if (!ModConfig.freelookEnabled) {
            resistancedlc$lastX = Double.NaN;
            resistancedlc$lastY = Double.NaN;
            return;
        }

        // Если Freelook не активен — сбрасываем состояние и пропускаем
        if (!FreelookManager.isActive()) {
            resistancedlc$lastX = Double.NaN;
            resistancedlc$lastY = Double.NaN;
            return;
        }

        // Первый кадр после активации — запоминаем точку, дельту не считаем
        if (Double.isNaN(resistancedlc$lastX) || Double.isNaN(resistancedlc$lastY)) {
            resistancedlc$lastX = xpos;
            resistancedlc$lastY = ypos;
            return;
        }

        double dx = xpos - resistancedlc$lastX;
        double dy = ypos - resistancedlc$lastY;
        resistancedlc$lastX = xpos;
        resistancedlc$lastY = ypos;

        // Передаём дельты во FreelookManager (вращаем только камеру)
        FreelookManager.onMouseMove(dx, dy);

        // Отменяем ванильный поворот игрока
        ci.cancel();
    }
}