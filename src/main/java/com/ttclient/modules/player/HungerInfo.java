package com.ttclient.modules.player;

import com.ttclient.modules.Category;
import com.ttclient.modules.Module;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.food.FoodData;

public class HungerInfo extends Module {
    public HungerInfo() {
        super("HungerInfo", "Food and saturation numbers next to the hotbar", Category.PLAYER);
        setEnabled(true);
    }

    @Override
    public void onRender2D(GuiGraphics g, float partialTick) {
        if (mc.player == null || mc.options.hideGui) return;
        FoodData food = mc.player.getFoodData();
        String text = String.format("Food %d  Sat %.1f", food.getFoodLevel(), food.getSaturationLevel());
        int w = mc.getWindow().getGuiScaledWidth();
        int h = mc.getWindow().getGuiScaledHeight();
        g.drawString(mc.font, text, w / 2 + 95, h - 39, 0xFFFFAA55, false);
    }
}
