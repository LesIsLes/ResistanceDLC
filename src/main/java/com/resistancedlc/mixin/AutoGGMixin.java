package com.resistancedlc.mixin;

import com.resistancedlc.AutoGGManager;
import com.resistancedlc.TotemTracker;
import com.resistancedlc.config.ModConfig;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundPlayerCombatKillPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * AutoGGMixin — ловим пакет ClientboundPlayerCombatKillPacket.
 * Этот пакет приходит, когда игрок умирает в бою (с указанием убийцы).
 *
 * В 1.21.11 пакет — record: ClientboundPlayerCombatKillPacket(int playerId, Component message)
 * Методы: playerId() и message().
 */
@Mixin(ClientPacketListener.class)
public class AutoGGMixin {

    @Inject(method = "handlePlayerCombatKill", at = @At("HEAD"), require = 0)
    private void onPlayerCombatKill(ClientboundPlayerCombatKillPacket packet, CallbackInfo ci) {
        if (!ModConfig.autoGgEnabled) return;

        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null) return;

        int victimId = packet.playerId();
        Entity victim = client.level.getEntity(victimId);

        if (!(victim instanceof Player victimPlayer)) return;
        if (victimPlayer == client.player) return;

        String lastAttacker = TotemTracker.getLastAttacker(victimPlayer.getUUID(), 10000L);
        String myName = client.player.getName().getString();

        if (lastAttacker == null || !lastAttacker.equals(myName)) {
            String raw = packet.message().getString();
            if (raw == null) return;
            String lower = raw.toLowerCase();
            if (!lower.contains(myName.toLowerCase())) return;
        }

        ModConfig.autoGgLastVictim = victimPlayer.getName().getString();
        AutoGGManager.onPlayerKilled();
    }
}