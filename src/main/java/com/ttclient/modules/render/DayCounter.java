package com.ttclient.modules.render;

import com.ttclient.modules.Category;
import com.ttclient.modules.Module;
import net.minecraft.client.gui.GuiGraphics;

public class DayCounter extends Module {
    public DayCounter() {
        super("DayCounter", "World day number from gameTime", Category.RENDER);
        setEnabled(true);
    }

    @Override
    public void onRender2D(GuiGraphics g, float partialTick) {
        if (mc.level == null || mc.options.hideGui) return;
        long dayTime = mc.level.getGameTime();
        long day = dayTime / 24000L;
        long tod = dayTime % 24000L;
        String phase = tod < 6000 ? "morning" : tod < 12000 ? "day" : tod < 18000 ? "dusk" : "night";
        String text = "Day " + day + " · " + phase;
        int w = mc.getWindow().getGuiScaledWidth();
        g.drawString(mc.font, text, w - mc.font.width(text) - 4, 18, 0xFF00E8A0, true);
    }
}
