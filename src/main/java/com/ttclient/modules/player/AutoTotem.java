package com.ttclient.modules.player;

import com.ttclient.modules.Category;
import com.ttclient.modules.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class AutoTotem extends Module {
    public AutoTotem() {
        super("AutoTotem", "Keep a totem in your offhand", Category.PLAYER);
    }

    @Override
    public void onTick() {
        Minecraft mc = mc();
        if (mc == null || mc.player == null) return;
        LocalPlayer p = mc.player;
        if (p.getOffhandItem().is(Items.TOTEM_OF_UNDYING)) return;

        Inventory inv = p.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (stack.is(Items.TOTEM_OF_UNDYING)) {
                // Swap into offhand via selected hotbar when possible
                if (i < 9) {
                    int prev = inv.getSelectedSlot();
                    inv.setSelectedSlot(i);
                    // Offhand swap key is typically F (key category)
                    // Fallback: put stack reference if API allows
                    p.setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND, stack.copy());
                    inv.setItem(i, ItemStack.EMPTY);
                    inv.setSelectedSlot(prev);
                }
                return;
            }
        }
    }
}
