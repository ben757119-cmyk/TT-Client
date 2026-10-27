package com.ttclient.mixin;

import com.ttclient.modules.client.PackTweaks;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Force enchanted foil for PackTweaks rainbow items (GUI context only). */
@Mixin(ItemStack.class)
public abstract class MixinItemStack {

    @Inject(method = {"hasFoil", "isFoil", "hasGlint", "hasEnchantmentGlint"}, at = @At("HEAD"), cancellable = true, require = 0)
    private void ttclient$forceRainbowFoil(CallbackInfoReturnable<Boolean> cir) {
        try {
            if (PackTweaks.shouldForceFoil((ItemStack) (Object) this)) {
                cir.setReturnValue(true);
            }
        } catch (Throwable ignored) {}
    }
}
