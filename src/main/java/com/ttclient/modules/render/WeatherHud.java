package com.ttclient.modules.render;

import com.ttclient.modules.Category;
import com.ttclient.modules.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public class WeatherHud extends Module {
    public WeatherHud() {
        super("WeatherHud", "Show weather state", Category.RENDER);
    }

    @Override
    public void onRender2D(GuiGraphicsExtractor graphics, float partialTick) {
        Minecraft minecraft = mc();
        if (minecraft == null || minecraft.level == null) return;
        String w = minecraft.level.isThundering() ? "Thunder" : (minecraft.level.isRaining() ? "Rain" : "Clear");
        graphics.text(minecraft.font, w, 4, 128, 0xFF8B95A8, false);
    }
}
