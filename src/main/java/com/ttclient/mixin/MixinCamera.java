package com.ttclient.mixin;

import com.ttclient.modules.player.Freecam;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

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

    @Inject(method = "setup", at = @At("HEAD"), require = 0)
    private void ttclient$detach(CallbackInfo ci) {
        if (Freecam.isActive()) {
            try { this.detached = true; } catch (Throwable ignored) {}
        }
    }

    @ModifyArgs(
            method = "setup",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;setPosition(DDD)V"),
            require = 0
    )
    private void ttclient$posSetup(Args args) {
        applyPos(args);
    }

    @ModifyArgs(
            method = "setup",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;setRotation(FF)V"),
            require = 0
    )
    private void ttclient$rotSetup(Args args) {
        applyRot(args);
    }

    @ModifyArgs(
            method = "alignWithEntity",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;setPosition(DDD)V"),
            require = 0
    )
    private void ttclient$posAlign(Args args) {
        applyPos(args);
    }

    @ModifyArgs(
            method = "alignWithEntity",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;setRotation(FF)V"),
            require = 0
    )
    private void ttclient$rotAlign(Args args) {
        applyRot(args);
    }

    @Inject(method = "setup", at = @At("RETURN"), require = 0)
    private void ttclient$setupReturn(CallbackInfo ci) {
        if (!Freecam.isActive()) return;
        Freecam fc = Freecam.get();
        if (fc == null) return;
        float p = pt();
        try {
            setPosition(fc.getX(p), fc.getY(p), fc.getZ(p));
            setRotation(fc.getYaw(p), fc.getPitch(p));
        } catch (Throwable ignored) {}
        try { this.detached = true; } catch (Throwable ignored) {}
    }

    private static void applyPos(Args args) {
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

    private static void applyRot(Args args) {
        if (!Freecam.isActive()) return;
        Freecam fc = Freecam.get();
        if (fc == null) return;
        float p = pt();
        try {
            args.set(0, fc.getYaw(p));
            args.set(1, fc.getPitch(p));
        } catch (Throwable ignored) {}
    }
}
