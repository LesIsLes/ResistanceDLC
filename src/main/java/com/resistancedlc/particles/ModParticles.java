package com.resistancedlc.particles;

import com.resistancedlc.ResistanceDLC;
import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;

public final class ModParticles {

    public static final SimpleParticleType HIT_STAR = FabricParticleTypes.simple();
    public static final SimpleParticleType HIT_SNOW = FabricParticleTypes.simple();
    public static final SimpleParticleType HIT_TRIANGLE = FabricParticleTypes.simple();

    private ModParticles() {}

    public static void register() {
        Registry.register(BuiltInRegistries.PARTICLE_TYPE,
                Identifier.fromNamespaceAndPath(ResistanceDLC.MOD_ID, "hit_star"), HIT_STAR);
        Registry.register(BuiltInRegistries.PARTICLE_TYPE,
                Identifier.fromNamespaceAndPath(ResistanceDLC.MOD_ID, "hit_snow"), HIT_SNOW);
        Registry.register(BuiltInRegistries.PARTICLE_TYPE,
                Identifier.fromNamespaceAndPath(ResistanceDLC.MOD_ID, "hit_triangle"), HIT_TRIANGLE);
    }
}