package com.ttclient.mixin;

import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Camera.class)
public interface CameraAccessor {
    @Invoker("setPosition")
    void ttclient$setPos(double x, double y, double z);

    /** Mojmap 2-arg (may be deprecated in favor of 3-arg with roll). */
    @Invoker("setRotation")
    void ttclient$setRot(float yRot, float xRot);
}
