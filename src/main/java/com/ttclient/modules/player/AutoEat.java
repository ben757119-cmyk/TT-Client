package com.ttclient.modules.player;

import com.ttclient.modules.Category;
import com.ttclient.modules.Module;
import com.ttclient.settings.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

public class AutoEat extends Module {
    public final NumberSetting hunger = addSetting(new NumberSetting("Hunger", "Eat below hunger level", 14, 1, 19, 1));

    public AutoEat() {
        super("AutoEat", "Eat food when hungry", Category.PLAYER);
    }

    @Override
    public void onTick() {
        Minecraft mc = mc();
        if (mc == null || mc.player == null || mc.gameMode == null) return;
        LocalPlayer p = mc.player;
        if (p.getFoodData().getFoodLevel() > hunger.get()) return;
        if (p.isUsingItem()) return;

        for (int i = 0; i < 9; i++) {
            ItemStack stack = p.getInventory().getItem(i);
            if (stack.isEmpty()) continue;
            if (!stack.getItem().components().has(net.minecraft.core.component.DataComponents.FOOD)) continue;
            p.getInventory().setSelectedSlot(i);
            mc.gameMode.useItem(p, InteractionHand.MAIN_HAND);
            return;
        }
    }
}
