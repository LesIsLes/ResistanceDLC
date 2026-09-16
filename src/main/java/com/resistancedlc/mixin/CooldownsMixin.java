package com.resistancedlc.mixin;

import com.resistancedlc.config.ModConfig;

import net.minecraft.world.item.ItemCooldowns;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * CooldownsMixin — перехватываем тик ItemCooldowns.
 *
 * Зачем: ванильный tick() вызывается каждый тик и "прокручивает"
 * все активные кулдауны. Мы ничего не делаем в инъекции — просто
 * подтверждаем, что класс загружен и работает.
 *
 * Основная логика — в CooldownsManager (pull-based через HUD-рендер).
 * Миксин нужен на будущее: если захотим логировать/события при старте/конце кулдауна.
 */
@Mixin(ItemCooldowns.class)
public class CooldownsMixin {

    @Inject(method = "tick", at = @At("HEAD"), require = 0)
    private void onTick(CallbackInfo ci) {
        // Заглушка: логика в CooldownsManager.
        // Если ModConfig.cooldownsEnabled == false — можно было бы очищать кэш,
        // но у нас pull-based, так что ничего не делаем.
    }
}