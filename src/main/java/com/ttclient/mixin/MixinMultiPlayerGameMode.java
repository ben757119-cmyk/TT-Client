package com.ttclient.mixin;

import com.ttclient.modules.combat.MaceHack;
import com.ttclient.modules.player.InstantRebreak;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * MaceHack fall-spoof before attack; InstantRebreak target capture on startDestroyBlock.
 */
@Mixin(MultiPlayerGameMode.class)
public abstract class MixinMultiPlayerGameMode {

    @Inject(method = "attack", at = @At("HEAD"), require = 0)
    private void ttclient$maceSpoofBeforeAttack(Player player, Entity target, CallbackInfo ci) {
        try {
            MaceHack.onPlayerAttack(player, target);
        } catch (Throwable ignored) {}
    }

    @Inject(method = "startDestroyBlock", at = @At("HEAD"), require = 0)
    private void ttclient$instantRebreakStart(BlockPos pos, Direction direction, CallbackInfoReturnable<Boolean> cir) {
        try {
            InstantRebreak.onStartBreak(pos, direction);
        } catch (Throwable ignored) {}
    }
}
