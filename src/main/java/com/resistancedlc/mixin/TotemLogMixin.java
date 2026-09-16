package com.resistancedlc.mixin;

import com.resistancedlc.config.ModConfig;

import com.resistancedlc.TotemTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public class TotemLogMixin {

    /**
     * Ловим момент, когда у сущности срабатывает тотем.
     * В 1.21.11 метод: checkTotemDeathProtection(DamageSource) -> boolean
     */
    @Inject(method = "checkTotemDeathProtection", at = @At("RETURN"))
    private void onTotemPop(DamageSource source, CallbackInfoReturnable<Boolean> cir) {
        // Сработал тотем?
        if (!cir.getReturnValue()) return;
        if (!ModConfig.totemLogEnabled) return;

        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null) return;

        LivingEntity victim = (LivingEntity) (Object) this;

        // Радиус проверяем от жертвы до игрока-клиента
        double distSq = victim.distanceToSqr(client.player);
        int radius = ModConfig.totemLogRadius;
        if (distSq > (double) radius * radius) return;

        // === Кто снёс ===
        String attackerName = null;

        // 1) Пытаемся достать из DamageSource
        Entity attacker = source.getEntity();
        if (attacker instanceof Player p) {
            attackerName = p.getName().getString();
        } else if (attacker != null) {
            attackerName = attacker.getName().getString();
        }

        // 2) Fallback — из трекера (если DamageSource пустой)
        if (attackerName == null || attackerName.isEmpty()) {
            attackerName = TotemTracker.getLastAttacker(victim.getUUID(), 5000L);
        }

        if (attackerName == null || attackerName.isEmpty()) {
            attackerName = "Неизвестно";
        }

        // === Кто жертва ===
        String victimName;
        if (victim instanceof Player vp) {
            victimName = vp.getName().getString();
        } else {
            victimName = victim.getName().getString();
        }

        // === Сообщение в чат ===
        String msg = "§c[PvP] §f" + attackerName + " §7снёс тотем §f" + victimName;
        client.player.displayClientMessage(Component.literal(msg), false);

        // === Звук-уведомление (клиентский) ===
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
}