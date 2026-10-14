package com.ttclient.modules.player;

import com.ttclient.modules.Category;
import com.ttclient.modules.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public class HungerInfo extends Module {
    public HungerInfo() {
        super("HungerInfo", "Show hunger / saturation", Category.PLAYER);
    }

    @Override
    public void onRender2D(GuiGraphicsExtractor graphics, float partialTick) {
        Minecraft minecraft = mc();
        if (minecraft == null || minecraft.player == null) return;
        int food = minecraft.player.getFoodData().getFoodLevel();
        float sat = minecraft.player.getFoodData().getSaturationLevel();
        graphics.text(minecraft.font, String.format("Food %d (%.1f)", food, sat), 4, 164, 0xFFE8EAED, false);
    }
}
