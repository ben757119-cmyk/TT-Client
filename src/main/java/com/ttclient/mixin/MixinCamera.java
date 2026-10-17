package com.ttclient.mixin;

import com.ttclient.modules.player.Freecam;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * After vanilla Camera.setup, force freecam position/rotation.
 * Uses reflection so 26.2 method-name drift does not break compile.
 */
@Mixin(Camera.class)
public abstract class MixinCamera {

    @Inject(method = "setup", at = @At("TAIL"), require = 0)
    private void ttclient$freecamSetup(BlockGetter level, Entity entity, boolean thirdPerson,
                                       boolean thirdPersonReverse, float partialTick, CallbackInfo ci) {
        applyFreecam((Camera) (Object) this);
    }

    private static void applyFreecam(Camera cam) {
        try {
            Freecam fc = Freecam.get();
            if (fc == null || !fc.active()) return;

            boolean posSet = false;
            try {
                var m = Camera.class.getDeclaredMethod("setPosition", double.class, double.class, double.class);
                m.setAccessible(true);
                m.invoke(cam, fc.getCamX(), fc.getCamY(), fc.getCamZ());
                posSet = true;
            } catch (Throwable ignored) {}
            if (!posSet) {
                try {
                    var m = Camera.class.getDeclaredMethod("setPosition", Vec3.class);
                    m.setAccessible(true);
                    m.invoke(cam, new Vec3(fc.getCamX(), fc.getCamY(), fc.getCamZ()));
                    posSet = true;
                } catch (Throwable ignored) {}
            }
            if (!posSet) {
                for (var f : Camera.class.getDeclaredFields()) {
                    if (f.getType() == Vec3.class) {
                        f.setAccessible(true);
                        f.set(cam, new Vec3(fc.getCamX(), fc.getCamY(), fc.getCamZ()));
                        break;
                    }
                }
            }

            try {
                var m = Camera.class.getDeclaredMethod("setRotation", float.class, float.class);
                m.setAccessible(true);
                m.invoke(cam, fc.getCamYaw(), fc.getCamPitch());
            } catch (Throwable t) {
                try {
                    var m = Camera.class.getDeclaredMethod("setRotation", float.class, float.class, float.class);
                    m.setAccessible(true);
                    m.invoke(cam, fc.getCamYaw(), fc.getCamPitch(), 0f);
                } catch (Throwable ignored) {}
            }
        } catch (Throwable ignored) {}
    }
}
