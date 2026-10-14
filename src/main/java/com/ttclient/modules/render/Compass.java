package com.ttclient.modules.render;

import com.ttclient.modules.Category;
import com.ttclient.modules.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

public class Compass extends Module {
    public Compass() {
        super("Compass", "Show facing compass", Category.RENDER);
    }

    @Override
    public void onRender2D(GuiGraphics graphics, float partialTick) {
        Minecraft mc = mc();
        if (mc == null || mc.player == null) return;
        String dir = mc.player.getDirection().getSerializedName().toUpperCase();
        graphics.drawString(mc.font, dir, 4, 92, 0xFF00E8A0, false);
    }
}
