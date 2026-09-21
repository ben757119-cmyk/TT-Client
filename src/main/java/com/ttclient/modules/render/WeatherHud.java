package com.ttclient.modules.render;

import com.ttclient.modules.Category;
import com.ttclient.modules.Module;
import net.minecraft.client.gui.GuiGraphics;

public class WeatherHud extends Module {
    public WeatherHud() {
        super("WeatherHud", "Clear / rain / thunder from the client level", Category.RENDER);
        setEnabled(true);
    }

    @Override
    public void onRender2D(GuiGraphics g, float partialTick) {
        if (mc.level == null || mc.options.hideGui) return;
        String weather;
        if (mc.level.isThundering()) weather = "Thunder";
        else if (mc.level.isRaining()) weather = "Rain";
        else weather = "Clear";
        int w = mc.getWindow().getGuiScaledWidth();
        g.drawString(mc.font, weather, w - mc.font.width(weather) - 4, 30, 0xFF8B95A8, true);
    }
}
