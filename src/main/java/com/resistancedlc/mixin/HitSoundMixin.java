package com.resistancedlc.mixin;

import com.resistancedlc.MyCustomScreen;
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
        if (!MyCustomScreen.customHitSoundsEnabled) return;

        Player self = (Player) (Object) this;
        Minecraft client = Minecraft.getInstance();

        if (client.player == null || self != client.player) return;
        if (client.level == null) return;

        // Используем выбранный пресет (1–7)
        int preset = MyCustomScreen.customHitSoundPreset;
        if (preset < 1 || preset > 7) preset = 1;

        SoundEvent customSound = SoundEvent.createVariableRangeEvent(
                Identifier.fromNamespaceAndPath("resistancedlc", "custom_hit_" + preset)
        );

        client.level.playLocalSound(
                self.getX(), self.getY(), self.getZ(),
                customSound,
                SoundSource.PLAYERS,
                MyCustomScreen.customHitSoundVolume,
                MyCustomScreen.customHitSoundPitch,
                false
        );
    }
}