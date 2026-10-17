package com.ttclient.mixin;

import com.ttclient.modules.player.Freecam;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class MixinCamera {

    @Shadow
    protected abstract void setPosition(double x, double y, double z);

    @Shadow
    protected abstract void setRotation(float yRot, float xRot);

    @Inject(
            method = "setup(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/world/entity/Entity;ZZF)V",
            at = @At("TAIL"),
            require = 0
    )
    private void ttclient$freecamSetupBlockGetter(BlockGetter level, Entity entity, boolean thirdPerson,
                                                   boolean inverseView, float partialTick, CallbackInfo ci) {
        apply();
    }

    @Inject(
            method = "setup(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/Entity;ZZF)V",
            at = @At("TAIL"),
            require = 0
    )
    private void ttclient$freecamSetupLevel(CallbackInfo ci) {
        apply();
    }

    @Inject(method = "setup", at = @At("TAIL"), require = 0)
    private void ttclient$freecamSetupAny(CallbackInfo ci) {
        apply();
    }

    private void apply() {
        Freecam fc = Freecam.get();
        if (fc == null || !fc.active()) return;
        try {
            setPosition(fc.getCamX(), fc.getCamY(), fc.getCamZ());
        } catch (Throwable ignored) {}
        try {
            setRotation(fc.getCamYaw(), fc.getCamPitch());
        } catch (Throwable ignored) {}
        try {
            Camera self = (Camera) (Object) this;
            for (var f : Camera.class.getDeclaredFields()) {
                if (f.getType() == Vec3.class) {
                    f.setAccessible(true);
                    f.set(self, new Vec3(fc.getCamX(), fc.getCamY(), fc.getCamZ()));
                }
            }
        } catch (Throwable ignored) {}
    }
}
