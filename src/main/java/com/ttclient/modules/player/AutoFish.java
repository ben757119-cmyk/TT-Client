package com.ttclient.modules.player;

import com.ttclient.modules.Category;
import com.ttclient.modules.Module;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.Items;

public class AutoFish extends Module {
    private int recastDelay;

    public AutoFish() {
        super("AutoFish", "Recast the rod after a catch", Category.PLAYER);
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.gameMode == null) return;
        if (!mc.player.getMainHandItem().is(Items.FISHING_ROD)) return;

        if (recastDelay > 0) {
            recastDelay--;
            if (recastDelay == 0) {
                mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
            }
            return;
        }

        FishingHook hook = mc.player.fishing;
        if (hook == null) {
            recastDelay = 10;
        }
    }
}
