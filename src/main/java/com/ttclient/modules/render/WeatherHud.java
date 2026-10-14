package com.ttclient.modules.render;

import com.ttclient.modules.Category;
import com.ttclient.modules.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

public class WeatherHud extends Module {
    public WeatherHud() {
        super("WeatherHud", "Show weather state", Category.RENDER);
    }

    @Override
    public void onRender2D(GuiGraphics graphics, float partialTick) {
        Minecraft mc = mc();
        if (mc == null || mc.level == null) return;
        String w = mc.level.isThundering() ? "Thunder" : (mc.level.isRaining() ? "Rain" : "Clear");
        graphics.drawString(mc.font, w, 4, 128, 0xFF8B95A8, false);
    }
}
