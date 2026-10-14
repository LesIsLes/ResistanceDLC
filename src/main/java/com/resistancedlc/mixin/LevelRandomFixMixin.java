package com.resistancedlc.mixin;

import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.ThreadSafeLegacyRandomSource;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Level.class)
public class LevelRandomFixMixin {

    @Shadow @Final @Mutable
    protected RandomSource random;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void resistancedlc$replaceRandomWithThreadSafe(CallbackInfo ci) {
        this.random = new ThreadSafeLegacyRandomSource(
                net.minecraft.world.level.levelgen.RandomSupport.generateUniqueSeed()
        );
    }
}