package com.ttclient.mixin;

import com.ttclient.modules.player.Freecam;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Freecam camera redirect — reflection only (no @Shadow / @Invoker / ModifyArgs).
 * Soft injects with require=0 so mapping drift cannot brick launch.
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
                    if (n.contains("pos") || n.contains("setposition")) {
                        m.setAccessible(true);
                        try { m.invoke(self, x, y, z); } catch (Throwable ignored) {}
                    }
                }
                if (m.getParameterCount() == 2
                        && m.getParameterTypes()[0] == float.class
                        && m.getParameterTypes()[1] == float.class) {
                    String n = m.getName().toLowerCase();
                    if (n.contains("rot") || n.contains("setrotation")) {
                        m.setAccessible(true);
                        try { m.invoke(self, yaw, pitch); } catch (Throwable ignored) {}
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

    @Inject(method = {"setup", "alignWithEntity"}, at = @At("RETURN"), require = 0)
    private void ttclient$forceReturn(CallbackInfo ci) {
        if (!Freecam.isActive()) return;
        Freecam fc = Freecam.get();
        if (fc == null) return;
        float p = pt();
        reflectSetPosRot((Camera) (Object) this, fc.getX(p), fc.getY(p), fc.getZ(p), fc.getYaw(p), fc.getPitch(p));
    }
}
