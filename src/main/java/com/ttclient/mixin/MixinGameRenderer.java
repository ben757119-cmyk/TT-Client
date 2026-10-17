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
    @Inject(method = "renderLevel", at = @At("RETURN"), require = 0)
    private void ttclient$fcReturn(CallbackInfo ci) {
        if (!Freecam.isActive()) return;
        try {
            Camera cam = Minecraft.getInstance().gameRenderer.mainCamera();
            Freecam.forceCamera(cam);
        } catch (Throwable ignored) {}
    }
}
