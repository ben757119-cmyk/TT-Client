package com.ttclient.mixin;

import com.ttclient.modules.client.PackTweaks;
import net.minecraft.client.gui.Gui;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * PackTweaks overlays: NoPumpkin, LowFire (soft targets for 26.2).
 */
@Mixin(Gui.class)
public abstract class MixinGui {

    /** Hide / cancel pumpkin blur (and similar full-screen texture overlays). */
    @Inject(
            method = {
                    "renderTextureOverlay",
                    "renderTextureOverlay(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/resources/ResourceLocation;F)V",
                    "renderTextureOverlay(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/resources/ResourceLocation;F)V"
            },
            at = @At("HEAD"),
            cancellable = true,
            require = 0
    )
    private void ttclient$textureOverlay(Object graphics, Object texture, float opacity, CallbackInfo ci) {
        try {
            if (PackTweaks.noPumpkinEnabled() && PackTweaks.isPumpkinOverlay(texture)) {
                ci.cancel();
                return;
            }
            if (PackTweaks.lowFireEnabled() && PackTweaks.isFireOverlay(texture) && opacity > 0.35f) {
                ci.cancel();
            }
        } catch (Throwable ignored) {}
    }

    /** Some mappings pass only texture + opacity. */
    @Inject(
            method = {
                    "renderTextureOverlay"
            },
            at = @At("HEAD"),
            cancellable = true,
            require = 0
    )
    private void ttclient$textureOverlay2(Object texture, float opacity, CallbackInfo ci) {
        try {
            if (PackTweaks.noPumpkinEnabled() && PackTweaks.isPumpkinOverlay(texture)) {
                ci.cancel();
            }
        } catch (Throwable ignored) {}
    }

    /** Keep scoreboard method referenced for soft mapping; no-op transform. */
    @ModifyVariable(
            method = {
                    "displayScoreboardSidebar",
                    "renderScoreboardSidebar",
                    "renderScoreboard"
            },
            at = @At("HEAD"),
            argsOnly = true,
            ordinal = 0,
            require = 0
    )
    private Object ttclient$scoreboardPass(Object objective) {
        return objective;
    }
}
