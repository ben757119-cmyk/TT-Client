package com.ttclient.mixin;

import com.ttclient.modules.client.PackTweaks;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Soft GUI item overlay for PackTweaks rainbow frame.
 * require=0 so missing overloads never brick the client.
 */
@Mixin(GuiGraphics.class)
public abstract class MixinGuiGraphics {

    @Inject(method = "renderItem(Lnet/minecraft/world/item/ItemStack;II)V", at = @At("RETURN"), require = 0)
    private void ttclient$rainbowSimple(ItemStack stack, int x, int y, CallbackInfo ci) {
        drawFrame(stack, x, y);
    }

    private void drawFrame(ItemStack stack, int x, int y) {
        try {
            if (!PackTweaks.shouldRainbow(stack)) return;
            GuiGraphics g = (GuiGraphics) (Object) this;
            int c = PackTweaks.rainbowArgb(0.9f);
            g.fill(x, y, x + 16, y + 1, c);
            g.fill(x, y + 15, x + 16, y + 16, c);
            g.fill(x, y, x + 1, y + 16, c);
            g.fill(x + 15, y, x + 16, y + 16, c);
            int c2 = PackTweaks.rainbowArgb(0.22f);
            g.fill(x + 1, y + 1, x + 15, y + 15, c2);
        } catch (Throwable ignored) {}
    }
}
