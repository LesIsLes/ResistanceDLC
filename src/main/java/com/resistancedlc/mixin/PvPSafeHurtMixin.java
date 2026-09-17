package com.resistancedlc.mixin;

import com.resistancedlc.PvPSafeManager;
import com.resistancedlc.config.ModConfig;

import net.minecraft.client.Minecraft;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * PvPSafeHurtMixin — запускает таймер боя ТОЛЬКО когда:
 *   1. Нас ударил игрок.
 *   2. Мы ударили игрока (это уже есть в HitSoundMixin, оставляем).
 *
 * В 1.21.11 метод урона: LivingEntity#hurtServer(ServerLevel, DamageSource, float)
 * Он вызывается только на сервере. В одиночной игре сервер встроенный — работает.
 * В мультиплеере — используем парсинг чата.
 */
@Mixin(LivingEntity.class)
public class PvPSafeHurtMixin {

    @Inject(method = "hurtServer", at = @At("HEAD"), require = 0)
    private void onLivingHurtServer(ServerLevel level, DamageSource source, float amount,
                                    CallbackInfoReturnable<Boolean> cir) {
        if (!ModConfig.pvpSafeEnabled) return;
        if (source == null) return;

        LivingEntity self = (LivingEntity) (Object) this;

        // Реагируем только на нашего клиентского игрока
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;
        if (self != client.player) return;

        // Проверяем, что источник урона — игрок
        Entity attacker = source.getEntity();
        if (!(attacker instanceof Player)) return;
        // Не считаем самоурон
        if (attacker == self) return;

        // Запускаем таймер боя
        PvPSafeManager.recordHit();
    }
}