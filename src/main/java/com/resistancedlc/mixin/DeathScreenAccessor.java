package com.resistancedlc.mixin;

import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Аксессор к приватному полю causeOfDeath в DeathScreen (1.21.11).
 */
@Mixin(DeathScreen.class)
public interface DeathScreenAccessor {

    @Accessor("causeOfDeath")
    Component getCauseOfDeath();
}