package com.ttclient.mixin;

import com.ttclient.modules.player.Freecam;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class MixinCamera {

    @Inject(method = "setup", at = @At("TAIL"), require = 0)
    private void ttclient$setup5(BlockGetter level, Entity entity, boolean thirdPerson,
                                 boolean reverse, float partialTick, CallbackInfo ci) {
        Freecam.applyTo((Camera) (Object) this);
    }

    @Inject(method = "setup", at = @At("TAIL"), require = 0)
    private void ttclient$setup6(BlockGetter level, Entity entity, boolean thirdPerson,
                                 boolean reverse, boolean something, float partialTick, CallbackInfo ci) {
        Freecam.applyTo((Camera) (Object) this);
    }
}
