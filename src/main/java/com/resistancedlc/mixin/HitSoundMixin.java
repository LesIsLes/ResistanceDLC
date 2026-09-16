package com.resistancedlc.mixin;

import com.resistancedlc.config.ModConfig;

import com.resistancedlc.TotemTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public class HitSoundMixin {

    @Inject(method = "attack", at = @At("HEAD"))
    private void onAttack(Entity target, CallbackInfo ci) {
        Player self = (Player) (Object) this;
        Minecraft client = Minecraft.getInstance();

        if (client.player == null || self != client.player) return;
        if (client.level == null) return;

        // ===== ЗАПИСЬ В ТРЕКЕР ПОСЛЕДНЕГО УДАРА =====
        if (target instanceof Player victim) {
            TotemTracker.record(victim.getUUID(), self.getName().getString());

            // ===== PVP SAFE: мы ударили игрока — начинаем бой =====
            com.resistancedlc.PvPSafeManager.recordHit();
        }

        // ===== COMBO COUNTER =====
        if (ModConfig.comboEnabled) {
            if (client.player.getAttackStrengthScale(0.0f) >= 1.0f) {
                ModConfig.currentCombo++;
                ModConfig.lastComboTime = System.currentTimeMillis();
            }
        }

        // ===== CUSTOM HIT SOUNDS =====
        if (!ModConfig.customHitSoundsEnabled) return;

        int preset = ModConfig.customHitSoundPreset;
        if (preset < 1 || preset > 7) preset = 1;

        SoundEvent customSound = SoundEvent.createVariableRangeEvent(
                Identifier.fromNamespaceAndPath("resistancedlc", "custom_hit_" + preset)
        );

        client.level.playLocalSound(
                self.getX(), self.getY(), self.getZ(),
                customSound,
                SoundSource.PLAYERS,
                ModConfig.customHitSoundVolume,
                ModConfig.customHitSoundPitch,
                false
        );
    }
}