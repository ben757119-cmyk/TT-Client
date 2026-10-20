package com.ttclient.mixin;

import com.ttclient.modules.player.Freecam;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

/**
 * Freecam camera redirect — no @Shadow fields (hard-fail on mapping drift).
 * Uses ModifyArgs + CameraAccessor invokers only.
 */
@Mixin(Camera.class)
public abstract class MixinCamera {

    private static float pt() {
        try {
            return Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false);
        } catch (Throwable t) {
            return 1.0f;
        }
    }

    private static void forceDetached(Camera self) {
        try {
            for (var f : Camera.class.getDeclaredFields()) {
                if (f.getType() == boolean.class) {
                    f.setAccessible(true);
                    // only flip the detached-like flag if currently false — best-effort
                    // Prefer field named detached when present
                    if (f.getName().toLowerCase().contains("detach")) {
                        f.setBoolean(self, true);
                        return;
                    }
                }
            }
        } catch (Throwable ignored) {}
    }

    @Inject(method = {"setup", "alignWithEntity"}, at = @At("HEAD"), require = 0)
    private void ttclient$detachHead(CallbackInfo ci) {
        if (Freecam.isActive()) forceDetached((Camera) (Object) this);
    }

    @ModifyArgs(
            method = {"setup", "alignWithEntity"},
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;setPosition(DDD)V"),
            require = 0
    )
    private void ttclient$pos(Args args) {
        if (!Freecam.isActive()) return;
        Freecam fc = Freecam.get();
        if (fc == null) return;
        float p = pt();
        try {
            args.set(0, fc.getX(p));
            args.set(1, fc.getY(p));
            args.set(2, fc.getZ(p));
        } catch (Throwable ignored) {}
    }

    @ModifyArgs(
            method = {"setup", "alignWithEntity"},
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;setRotation(FF)V"),
            require = 0
    )
    private void ttclient$rot(Args args) {
        if (!Freecam.isActive()) return;
        Freecam fc = Freecam.get();
        if (fc == null) return;
        float p = pt();
        try {
            args.set(0, fc.getYaw(p));
            args.set(1, fc.getPitch(p));
        } catch (Throwable ignored) {}
    }

    @Inject(method = {"setup", "alignWithEntity"}, at = @At("RETURN"), require = 0)
    private void ttclient$forceReturn(CallbackInfo ci) {
        if (!Freecam.isActive()) return;
        Freecam fc = Freecam.get();
        if (fc == null) return;
        float p = pt();
        try {
            Camera self = (Camera) (Object) this;
            CameraAccessor a = (CameraAccessor) self;
            a.ttclient$setPos(fc.getX(p), fc.getY(p), fc.getZ(p));
            a.ttclient$setRot(fc.getYaw(p), fc.getPitch(p));
            forceDetached(self);
        } catch (Throwable ignored) {}
    }
}
