package com.ttclient.mixin;

import com.ttclient.modules.world.XRay;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Opacity-style XRay: non-ore blocks stop occluding and can be made invisible.
 * Only affects client rendering of blocks the server already sent.
 */
@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class MixinBlockStateBase {

    @Inject(method = "canOcclude", at = @At("HEAD"), cancellable = true, require = 0)
    private void ttclient$canOcclude(CallbackInfoReturnable<Boolean> cir) {
        if (!XRay.isOpacityActive()) return;
        BlockState self = (BlockState) (Object) this;
        if (!XRay.isOre(self)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "getRenderShape", at = @At("HEAD"), cancellable = true, require = 0)
    private void ttclient$getRenderShape(CallbackInfoReturnable<RenderShape> cir) {
        if (!XRay.isOpacityActive()) return;
        BlockState self = (BlockState) (Object) this;
        if (!XRay.isOre(self) && XRay.shouldHideNonOres()) {
            cir.setReturnValue(RenderShape.INVISIBLE);
        }
    }
}
