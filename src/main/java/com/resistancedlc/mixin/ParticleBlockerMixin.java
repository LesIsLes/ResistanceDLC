package com.resistancedlc.mixin;

import com.resistancedlc.config.ModConfig;

import com.resistancedlc.MyCustomScreen;
import com.resistancedlc.ParticleBlockerManager;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.registries.BuiltInRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ParticleEngine.class)
public class ParticleBlockerMixin {

    /**
     * Ловим СОЗДАНИЕ частицы. Тут есть ParticleOptions, у которого есть getType().
     */
    @Inject(method = "createParticle", at = @At("HEAD"), cancellable = true, require = 0)
    private void onCreateParticle(ParticleOptions options, double x, double y, double z,
                                  double xSpeed, double ySpeed, double zSpeed,
                                  CallbackInfoReturnable<Particle> cir) {
        if (!ModConfig.particleBlockerEnabled) return;
        if (options == null) return;

        try {
            var type = options.getType();
            var key = BuiltInRegistries.PARTICLE_TYPE.getKey(type);
            if (key == null) return;

            String particleId = key.getPath();

            if (ParticleBlockerManager.shouldBlock(particleId)) {
                cir.setReturnValue(null);
            }
        } catch (Exception ignored) {}
    }
}