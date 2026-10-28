package com.ttclient.mixin;

import com.ttclient.modules.client.ToolStyles;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Dual-axe visual: when mainhand is an axe and offhand is empty, feed the
 * offhand render path a copy of the mainhand axe (render-only via item arg).
 */
@Mixin(ItemInHandRenderer.class)
public abstract class MixinItemInHandRenderer {

    @ModifyVariable(
            method = {
                    "renderArmWithItem",
                    "renderItem"
            },
            at = @At("HEAD"),
            argsOnly = true,
            ordinal = 0,
            require = 0
    )
    private ItemStack ttclient$dualAxeStack(ItemStack original) {
        try {
            if (!ToolStyles.dualAxeActive()) return original;
            if (original != null && !original.isEmpty()) return original;
            Minecraft mc = Minecraft.getInstance();
            if (mc == null || mc.player == null) return original;
            LocalPlayer p = mc.player;
            if (!ToolStyles.shouldDualAxeRender(p)) return original;
            ItemStack main = p.getItemInHand(InteractionHand.MAIN_HAND);
            if (ToolStyles.isAxe(main)) {
                return main.copy();
            }
        } catch (Throwable ignored) {}
        return original;
    }
}
