package com.resistancedlc.mixin;

import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Camera.class)
public interface CameraInvoker {

    @Invoker("setPosition")
    void resistancedlc$invokeSetPosition(double x, double y, double z);

    @Invoker("setRotation")
    void resistancedlc$invokeSetRotation(float yaw, float pitch);
}