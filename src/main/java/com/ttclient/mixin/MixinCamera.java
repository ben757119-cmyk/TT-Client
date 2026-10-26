package com.ttclient.mixin;

import com.ttclient.modules.player.Freecam;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Force freecam camera pose after vanilla setup.
 * Soft injects (require=0) so mapping drift cannot brick launch.
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

    @Inject(method = {"setup", "alignWithEntity"}, at = @At("RETURN"), require = 0)
    private void ttclient$forceReturn(CallbackInfo ci) {
        if (!Freecam.isActive()) return;
        Freecam fc = Freecam.get();
        if (fc == null) return;
        float p = pt();
        Freecam.applyCamera(
                (Camera) (Object) this,
                fc.getX(p), fc.getY(p), fc.getZ(p),
                fc.getYaw(p), fc.getPitch(p));
    }
}
