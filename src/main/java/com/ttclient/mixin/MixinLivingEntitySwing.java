package com.ttclient.mixin;

import com.ttclient.modules.client.ToolStyles;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class MixinLivingEntitySwing {

    @Shadow public float attackAnim;

    @Inject(method = {"tick", "baseTick", "aiStep"}, at = @At("TAIL"), require = 0)
    private void ttclient$toolSwingFeel(CallbackInfo ci) {
        try {
            if (!ToolStyles.swingsActive()) return;
            LivingEntity self = (LivingEntity) (Object) this;
            if (!(self instanceof Player)) return;
            if (!(self.level().isClientSide())) return;
            ItemStack stack = ToolStyles.mainHand((Player) self);
            float mul = ToolStyles.swingMultiplier(stack);
            if (mul == 1f) return;
            if (this.attackAnim > 0f) {
                float residual = this.attackAnim;
                float step = 0.16f * (mul - 1f);
                this.attackAnim = Math.max(0f, residual - step);
            }
        } catch (Throwable ignored) {}
    }
}
