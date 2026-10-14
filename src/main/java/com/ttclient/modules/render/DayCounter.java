package com.ttclient.modules.render;

import com.ttclient.modules.Category;
import com.ttclient.modules.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public class DayCounter extends Module {
    public DayCounter() {
        super("DayCounter", "Show world day count", Category.RENDER);
    }

    @Override
    public void onRender2D(GuiGraphicsExtractor graphics, float partialTick) {
        Minecraft minecraft = mc();
        if (minecraft == null || minecraft.level == null) return;
        long day = minecraft.level.getGameTime() / 24000L;
        graphics.text(minecraft.font, "Day " + day, 4, 116, 0xFFE8EAED, false);
    }
}
