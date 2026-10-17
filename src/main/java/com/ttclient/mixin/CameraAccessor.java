package com.ttclient.mixin;

import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Camera.class)
public interface CameraAccessor {
    @Invoker("setPosition")
    void ttclient$invokeSetPosition(double x, double y, double z);

    @Invoker("setRotation")
    void ttclient$invokeSetRotation(float yRot, float xRot);
}
