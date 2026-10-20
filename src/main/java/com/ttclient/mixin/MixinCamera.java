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
 * Freecam camera redirect without @Shadow / @Invoker (those hard-crash on mapping drift).
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

    private static void reflectSetPosRot(Camera self, double x, double y, double z, float yaw, float pitch) {
        try {
            for (var m : Camera.class.getDeclaredMethods()) {
                if (m.getParameterCount() == 3
                        && m.getParameterTypes()[0] == double.class
                        && m.getParameterTypes()[1] == double.class
                        && m.getParameterTypes()[2] == double.class) {
                    String n = m.getName().toLowerCase();
                    if (n.contains("pos") || n.contains("setposition") || n.equals("setposition")) {
                        m.setAccessible(true);
                        m.invoke(self, x, y, z);
                    }
                }
                if (m.getParameterCount() == 2
                        && m.getParameterTypes()[0] == float.class
                        && m.getParameterTypes()[1] == float.class) {
                    String n = m.getName().toLowerCase();
                    if (n.contains("rot") || n.contains("setrotation")) {
                        m.setAccessible(true);
                        m.invoke(self, yaw, pitch);
                    }
                }
            }
        } catch (Throwable ignored) {}
        try {
            for (var f : Camera.class.getDeclaredFields()) {
                if (f.getType() == boolean.class && f.getName().toLowerCase().contains("detach")) {
                    f.setAccessible(true);
                    f.setBoolean(self, true);
                }
            }
        } catch (Throwable ignored) {}
    }

    @Inject(method = {"setup", "alignWithEntity"}, at = @At("HEAD"), require = 0)
    private void ttclient$detachHead(CallbackInfo ci) {
        // no-op head; RETURN handler applies pos
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
        reflectSetPosRot((Camera) (Object) this, fc.getX(p), fc.getY(p), fc.getZ(p), fc.getYaw(p), fc.getPitch(p));
    }
}
