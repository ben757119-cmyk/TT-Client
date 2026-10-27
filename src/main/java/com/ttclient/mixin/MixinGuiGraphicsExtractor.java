package com.ttclient.mixin;

import com.ttclient.modules.client.PackTweaks;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Inventory-only animated rainbow overlay for PackTweaks.
 * Targets GuiGraphicsExtractor (26.2 — GuiGraphics does not exist here).
 */
@Mixin(GuiGraphicsExtractor.class)
public abstract class MixinGuiGraphicsExtractor {

    @Inject(
            method = {
                    "renderItem",
                    "renderFakeItem",
                    "renderItemWithoutEntity",
                    "renderItemDecorated"
            },
            at = @At("RETURN"),
            require = 0
    )
    private void ttclient$rainbowAfterItem(ItemStack stack, int x, int y, CallbackInfo ci) {
        paintRainbow(stack, x, y);
    }

    @Inject(
            method = {
                    "renderItem",
                    "renderFakeItem"
            },
            at = @At("RETURN"),
            require = 0
    )
    private void ttclient$rainbowAfterItemSeed(ItemStack stack, int x, int y, int seed, CallbackInfo ci) {
        paintRainbow(stack, x, y);
    }

    @Inject(
            method = {
                    "renderItemDecorations",
                    "renderItemDecorated"
            },
            at = @At("RETURN"),
            require = 0
    )
    private void ttclient$rainbowAfterDeco(Object font, ItemStack stack, int x, int y, CallbackInfo ci) {
        paintRainbow(stack, x, y);
    }

    @Inject(
            method = {
                    "renderItemDecorations"
            },
            at = @At("RETURN"),
            require = 0
    )
    private void ttclient$rainbowAfterDecoText(Object font, ItemStack stack, int x, int y, String text, CallbackInfo ci) {
        paintRainbow(stack, x, y);
    }

    private void paintRainbow(ItemStack stack, int x, int y) {
        try {
            if (!PackTweaks.shouldRainbow(stack)) return;
            GuiGraphicsExtractor g = (GuiGraphicsExtractor) (Object) this;
            int salt = (x * 31 + y * 17) & 0xFF;
            int color = PackTweaks.rainbowArgb(-1f, salt);
            // soft tint over the icon (not opaque — item still readable)
            g.fill(x, y, x + 16, y + 16, color);
            // thin rainbow frame so it reads clearly as rainbow, not purple foil
            int edge = PackTweaks.rainbowArgb(0.85f, salt + 3);
            g.fill(x, y, x + 16, y + 1, edge);
            g.fill(x, y + 15, x + 16, y + 16, edge);
            g.fill(x, y, x + 1, y + 16, edge);
            g.fill(x + 15, y, x + 16, y + 16, edge);
        } catch (Throwable ignored) {}
    }
}
