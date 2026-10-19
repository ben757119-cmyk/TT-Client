package com.ttclient.mixin;

import com.ttclient.modules.player.Freecam;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Meteor-style: while freecam is on, mouse look goes to freecam, not the player body.
 * Targets Entity.turn (1.21+ name for changeLookDirection).
 */
@Mixin(Entity.class)
public abstract class MixinEntity {

    @Inject(method = "turn", at = @At("HEAD"), cancellable = true, require = 0)
    private void ttclient$freecamTurn(double xo, double yo, CallbackInfo ci) {
        if (!Freecam.isActive()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.player == null) return;
        if ((Object) this != mc.player) return;

        Freecam fc = Freecam.get();
        if (fc == null) return;
        // Meteor: xo/yo * 0.15 applied inside changeLookDirection
        fc.changeLookDirection(xo * 0.15, yo * 0.15);
        ci.cancel();
    }

    /** Fallback if mappings still use changeLookDirection */
    @Inject(method = "changeLookDirection", at = @At("HEAD"), cancellable = true, require = 0)
    private void ttclient$freecamLook(double xo, double yo, CallbackInfo ci) {
        if (!Freecam.isActive()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.player == null) return;
        if ((Object) this != mc.player) return;

        Freecam fc = Freecam.get();
        if (fc == null) return;
        fc.changeLookDirection(xo * 0.15, yo * 0.15);
        ci.cancel();
    }
}
