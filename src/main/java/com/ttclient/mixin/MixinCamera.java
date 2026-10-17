package com.ttclient.mixin;

import com.ttclient.modules.player.Freecam;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class MixinCamera {

    @Shadow private boolean detached;

    @Shadow
    protected abstract void setPosition(double x, double y, double z);

    @Shadow
    protected abstract void setRotation(float yRot, float xRot);

    private static float pt() {
        try {
            return Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false);
        } catch (Throwable t) {
            return 1.0f;
        }
    }

    @Inject(method = "setup", at = @At("RETURN"), require = 0)
    private void ttclient$freecam(CallbackInfo ci) {
        if (!Freecam.isActive()) return;
        Freecam fc = Freecam.get();
        if (fc == null) return;
        float p = pt();
        try {
            setPosition(fc.getX(p), fc.getY(p), fc.getZ(p));
            setRotation(fc.getYaw(p), fc.getPitch(p));
        } catch (Throwable ignored) {}
        try {
            this.detached = true;
        } catch (Throwable ignored) {}
        try {
            CameraAccessor a = (CameraAccessor) (Object) this;
            a.ttclient$setPos(fc.getX(p), fc.getY(p), fc.getZ(p));
            a.ttclient$setRot(fc.getYaw(p), fc.getPitch(p));
        } catch (Throwable ignored) {}
    }
}
