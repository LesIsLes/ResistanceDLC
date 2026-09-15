package com.resistancedlc.mixin;

import com.resistancedlc.config.ModConfig;

import com.resistancedlc.PvPSafeManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public class PvPSafeCommandMixin {

    /**
     * Ловим отправку команды на сервер.
     * Если PvP-бой активен и команда опасная — блокируем.
     */
    @Inject(method = "sendCommand", at = @At("HEAD"), cancellable = true)
    private void onSendCommand(String command, CallbackInfo ci) {
        if (!PvPSafeManager.isInCombat()) return;
        if (!com.resistancedlc.config.ModConfig.pvpSafeBlockCommands) return;
        if (command == null) return;

        // Убираем ведущий слэш
        String cmd = command.startsWith("/") ? command.substring(1) : command;

        if (PvPSafeManager.isDangerousCommand(cmd)) {
            PvPSafeManager.sendBlockedMessage(cmd);
            ci.cancel();
        }
    }
}