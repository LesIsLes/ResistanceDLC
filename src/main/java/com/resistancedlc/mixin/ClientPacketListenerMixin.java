package com.resistancedlc.mixin;

import com.resistancedlc.AutoGGManager;
import com.resistancedlc.ConfigManager;
import com.resistancedlc.KillTracker;
import com.resistancedlc.ResistanceDLC;
import com.resistancedlc.TotemTracker;
import com.resistancedlc.config.ModConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * ClientPacketListenerMixin — ловим события entity.
 *
 * Event 35 (TOTEM_USE):
 *   → TotemLog: "attacker снёс тотем victim".
 *
 * Event 3 (ENTITY_DEATH):
 *   → AutoGG: если мы недавно ударили эту entity (KillTracker),
 *     считаем килл нашим и триггерим AutoGG.
 *     Работает БЕЗ чат-сообщений о смерти (для FunTime и т.п.).
 */
@Mixin(ClientPacketListener.class)
public class ClientPacketListenerMixin {

    @Inject(
            method = "handleEntityEvent",
            at = @At("TAIL")
    )
    private void onEntityEvent(ClientboundEntityEventPacket packet, CallbackInfo ci) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null) return;

        Entity entity = packet.getEntity(client.level);
        if (entity == null) return;

        // ============ EVENT 35: TOTEM_USE ============
        if (packet.getEventId() == 35) {
            handleTotemUse(client, entity);
        }

        // ============ EVENT 3: ENTITY_DEATH ============
        if (packet.getEventId() == 3) {
            handleEntityDeath(client, entity);
        }
    }

    // ================= TOTEM LOG =================
    private void handleTotemUse(Minecraft client, Entity entity) {
        if (!ModConfig.totemLogEnabled) return;

        double distSq = entity.distanceToSqr(client.player);
        int radius = ModConfig.totemLogRadius;
        if (distSq > (double) radius * radius) return;

        String victimName = entity.getName().getString();

        String attackerName = TotemTracker.getLastAttacker(entity.getUUID(), 5000L);
        if (attackerName == null || attackerName.isEmpty()) {
            attackerName = "§7Неизвестно";
        }

        String msg = "§c[PvP] §f" + attackerName + " §7снёс тотем §f" + victimName;
        client.player.sendSystemMessage(Component.literal(msg));

        if (ModConfig.totemLogSound) {
            client.level.playLocalSound(
                    client.player.getX(),
                    client.player.getY(),
                    client.player.getZ(),
                    SoundEvents.NOTE_BLOCK_PLING.value(),
                    SoundSource.PLAYERS,
                    1.0f,
                    1.5f,
                    false
            );
        }
    }

    // ================= AUTO GG (event 3) =================
    private void handleEntityDeath(Minecraft client, Entity entity) {
        if (!ModConfig.autoGgEnabled) return;
        if (!ModConfig.statsTrackerEnabled) return;

        // Не считаем свою смерть киллом
        if (entity == client.player) return;

        // Проверяем, была ли entity недавно атакована нами
        if (!KillTracker.consumeHit(entity.getUUID())) return;

        // Считаем килл
        ModConfig.statsKills++;

        // Ник жертвы: сервер мог не написать в чат, но у entity есть имя
        String victimName = entity.getName().getString();
        if (victimName != null && !victimName.isEmpty()) {
            ModConfig.autoGgLastVictim = victimName;
        } else {
            ModConfig.autoGgLastVictim = "";
        }

        ConfigManager.save();
        AutoGGManager.onPlayerKilled();
    }
}