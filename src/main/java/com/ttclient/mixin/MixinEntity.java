package com.ttclient.mixin;

import com.ttclient.modules.player.Freecam;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Freecam look: cancel player body turn while active.
 * Only injects into {@code turn} (26.2). No dual-target that confuses the transformer.
 */
@Mixin(Entity.class)
public abstract class MixinEntity {

    @Inject(method = "turn(DD)V", at = @At("HEAD"), cancellable = true, require = 0)
    private void ttclient$freecamTurn(double xo, double yo, CallbackInfo ci) {
        try {
            if (!Freecam.isActive()) return;
            Minecraft mc = Minecraft.getInstance();
            if (mc == null || mc.player == null) return;
            if ((Object) this != mc.player) return;
            Freecam fc = Freecam.get();
            if (fc == null) return;
            fc.changeLookDirection(xo * 0.15, yo * 0.15);
            ci.cancel();
        } catch (Throwable ignored) {}
    }
}
