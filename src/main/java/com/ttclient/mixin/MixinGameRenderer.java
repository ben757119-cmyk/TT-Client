package com.ttclient.mixin;

import com.ttclient.modules.player.Freecam;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class MixinGameRenderer {

    @Inject(method = "renderLevel", at = @At("HEAD"), require = 0)
    private void ttclient$freecamBeforeLevel(CallbackInfo ci) {
        Freecam fc = Freecam.get();
        if (fc == null || !fc.active()) return;
        try {
            Minecraft mc = Minecraft.getInstance();
            Camera cam = null;
            try { cam = mc.gameRenderer.mainCamera(); } catch (Throwable ignored) {}
            if (cam == null) return;
            Freecam.forceCamera(cam);
        } catch (Throwable ignored) {}
    }
}
