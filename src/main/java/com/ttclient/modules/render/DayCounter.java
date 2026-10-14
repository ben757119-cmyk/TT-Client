package com.ttclient.modules.render;

import com.ttclient.modules.Category;
import com.ttclient.modules.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

public class DayCounter extends Module {
    public DayCounter() {
        super("DayCounter", "Show world day count", Category.RENDER);
    }

    @Override
    public void onRender2D(GuiGraphics graphics, float partialTick) {
        Minecraft mc = mc();
        if (mc == null || mc.level == null) return;
        long day = mc.level.getDayTime() / 24000L;
        graphics.drawString(mc.font, "Day " + day, 4, 116, 0xFFE8EAED, false);
    }
}
