package com.ttclient.modules.player;

import com.ttclient.modules.Category;
import com.ttclient.modules.Module;
import com.ttclient.settings.NumberSetting;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;

public class AutoEat extends Module {
    public final NumberSetting hunger = addSetting(new NumberSetting("Hunger", "Eat below this hunger", 14, 1, 19, 1));

    public AutoEat() {
        super("AutoEat", "Eat held food when hunger is low", Category.PLAYER);
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.gameMode == null) return;
        if (mc.player.getFoodData().getFoodLevel() > hunger.getInt()) return;
        if (mc.player.isUsingItem()) return;
        ItemStack stack = mc.player.getMainHandItem();
        FoodProperties food = stack.getFoodProperties(mc.player);
        if (food == null) return;
        mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
    }
}
