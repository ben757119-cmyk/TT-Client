package com.ttclient.modules.player;

import com.ttclient.modules.Category;
import com.ttclient.modules.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

public class HungerInfo extends Module {
    public HungerInfo() {
        super("HungerInfo", "Show hunger / saturation", Category.PLAYER);
    }

    @Override
    public void onRender2D(GuiGraphics graphics, float partialTick) {
        Minecraft mc = mc();
        if (mc == null || mc.player == null) return;
        int food = mc.player.getFoodData().getFoodLevel();
        float sat = mc.player.getFoodData().getSaturationLevel();
        graphics.drawString(mc.font, String.format("Food %d (%.1f)", food, sat), 4, 164, 0xFFE8EAED, false);
    }
}
