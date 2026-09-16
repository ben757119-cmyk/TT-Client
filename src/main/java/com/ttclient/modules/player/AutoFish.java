package com.ttclient.modules.player;

import com.ttclient.modules.Category;
import com.ttclient.modules.Module;
import com.ttclient.settings.BoolSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.Items;

public class AutoFish extends Module {
    public final BoolSetting openWater = addSetting(new BoolSetting("Open Water", "Only open water", false));
    private int castCooldown;

    public AutoFish() {
        super("AutoFish", "Automatically fish", Category.PLAYER);
    }

    @Override
    public void onTick() {
        Minecraft mc = mc();
        if (mc == null || mc.player == null || mc.gameMode == null) return;
        if (!mc.player.getMainHandItem().is(Items.FISHING_ROD)) return;
        if (castCooldown > 0) {
            castCooldown--;
            return;
        }
        FishingHook hook = mc.player.fishing;
        if (hook == null) {
            mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
            castCooldown = 10;
        }
    }
}
