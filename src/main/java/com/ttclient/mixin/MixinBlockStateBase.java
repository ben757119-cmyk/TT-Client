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
 * Opacity XRay mixins. First check is a static boolean — free when XRay is off
 * so chunk meshing does not lag with modules disabled.
 */
@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class MixinBlockStateBase {

    @Inject(method = "canOcclude", at = @At("HEAD"), cancellable = true, require = 0)
    private void ttclient$canOcclude(CallbackInfoReturnable<Boolean> cir) {
        if (!XRay.opacityActiveCache) return;
        try {
            BlockState self = (BlockState) (Object) this;
            if (!XRay.isOre(self)) cir.setReturnValue(false);
        } catch (Throwable ignored) {}
    }

    @Inject(method = "getRenderShape", at = @At("HEAD"), cancellable = true, require = 0)
    private void ttclient$getRenderShape(CallbackInfoReturnable<RenderShape> cir) {
        if (!XRay.opacityActiveCache) return;
        try {
            BlockState self = (BlockState) (Object) this;
            if (!XRay.isOre(self) && XRay.shouldHideNonOres()) {
                cir.setReturnValue(RenderShape.INVISIBLE);
            }
        } catch (Throwable ignored) {}
    }

    @Inject(method = "isSolidRender", at = @At("HEAD"), cancellable = true, require = 0)
    private void ttclient$isSolidRender(CallbackInfoReturnable<Boolean> cir) {
        if (!XRay.opacityActiveCache) return;
        try {
            BlockState self = (BlockState) (Object) this;
            if (!XRay.isOre(self)) cir.setReturnValue(false);
        } catch (Throwable ignored) {}
    }

    @Inject(method = "propagatesSkylightDown", at = @At("HEAD"), cancellable = true, require = 0)
    private void ttclient$skylight(CallbackInfoReturnable<Boolean> cir) {
        if (!XRay.opacityActiveCache) return;
        try {
            BlockState self = (BlockState) (Object) this;
            if (!XRay.isOre(self)) cir.setReturnValue(true);
        } catch (Throwable ignored) {}
    }
}
