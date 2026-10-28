package com.ttclient.mixin;

import com.ttclient.util.TimerHooks;
import net.minecraft.client.DeltaTracker;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Scales client tick accumulation so Timer / ImStorkingIt actually change tick rate.
 * Meteor-style inject on DeltaTracker.Timer.advanceGameTime.
 */
@Mixin(DeltaTracker.Timer.class)
public abstract class MixinDeltaTrackerTimer {
    @Shadow
    private float deltaTicks;

    @Inject(
            method = "advanceGameTime(J)I",
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/client/DeltaTracker$Timer;lastMs:J",
                    opcode = Opcodes.PUTFIELD
            ),
            require = 0
    )
    private void ttclient$scaleDeltaTicks(long timeMillis, CallbackInfoReturnable<Integer> cir) {
        float s = TimerHooks.speed;
        if (s != 1.0f && s > 0.0f) {
            this.deltaTicks *= s;
        }
    }
}
