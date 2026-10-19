package com.ttclient.mixin;

import com.ttclient.modules.world.XRay;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Meteor BlockMixin: force faces of selected xray blocks to render. */
@Mixin(Block.class)
public abstract class MixinBlock {

    @Inject(method = "shouldRenderFace", at = @At("HEAD"), cancellable = true, require = 0)
    private static void ttclient$xrayFace(BlockState state, BlockState neighborState, Direction direction,
                                          CallbackInfoReturnable<Boolean> cir) {
        if (!XRay.opacityActiveCache) return;
        try {
            if (XRay.isOre(state)) {
                cir.setReturnValue(true);
            }
        } catch (Throwable ignored) {}
    }
}
