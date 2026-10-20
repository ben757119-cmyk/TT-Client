package com.ttclient.mixin;

import com.ttclient.modules.combat.MaceHack;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Inject at HEAD of client attack so fall-spoof packets leave BEFORE
 * ServerboundInteractPacket — same timing as Wurst PlayerAttacksEntityListener.
 */
@Mixin(value = MultiPlayerGameMode.class, remap = true)
public abstract class MixinMultiPlayerGameMode {

    @Inject(method = {"attack", "attack(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/entity/Entity;)V"}, at = @At("HEAD"), require = 0)
    private void ttclient$maceBeforeAttack(Player player, Entity target, CallbackInfo ci) {
        try {
            MaceHack.onPlayerAttack(player, target);
        } catch (Throwable ignored) {}
    }
}
