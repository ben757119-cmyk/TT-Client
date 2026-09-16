package com.ttclient.modules.player;

import com.ttclient.modules.Category;
import com.ttclient.modules.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class InventoryCleaner extends Module {
    private int timer;

    public InventoryCleaner() {
        super("InventoryCleaner", "Drop junk items from hotbar", Category.PLAYER);
    }

    @Override
    public void onTick() {
        Minecraft mc = mc();
        if (mc == null || mc.player == null) return;
        if (timer > 0) { timer--; return; }
        LocalPlayer p = mc.player;
        for (int i = 0; i < 9; i++) {
            ItemStack s = p.getInventory().getItem(i);
            if (s.isEmpty()) continue;
            if (s.is(Items.ROTTEN_FLESH) || s.is(Items.POISONOUS_POTATO)
                    || s.is(Items.SPIDER_EYE) || s.is(Items.DIRT) || s.is(Items.GRAVEL)) {
                p.drop(s.copy(), true);
                p.getInventory().setItem(i, ItemStack.EMPTY);
                timer = 3;
                return;
            }
        }
    }
}
