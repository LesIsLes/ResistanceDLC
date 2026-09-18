package com.resistancedlc.targetesp;

import net.minecraft.world.phys.Vec3;

public record RenderTarget(
        int entityId,
        Vec3 position,
        float width,
        float height,
        float hurtProgress
) {
    public RenderTarget(int entityId, Vec3 position, float width, float height) {
        this(entityId, position, width, height, 0f);
    }
}