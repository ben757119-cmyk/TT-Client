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
 * Wurst-style: fall-spoof packets must leave BEFORE the attack interact packet.
 * Inject at HEAD of MultiPlayerGameMode.attack(Player, Entity).
 */
@Mixin(MultiPlayerGameMode.class)
public abstract class MixinMultiPlayerGameMode {

    @Inject(method = "attack", at = @At("HEAD"), require = 0)
    private void ttclient$maceSpoofBeforeAttack(Player player, Entity target, CallbackInfo ci) {
        try {
            MaceHack.onPlayerAttack(player, target);
        } catch (Throwable ignored) {}
    }
}
