package com.ttclient.modules.render;

import com.ttclient.modules.Category;
import com.ttclient.modules.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public class Compass extends Module {
    public Compass() {
        super("Compass", "Show facing compass", Category.RENDER);
    }

    @Override
    public void onRender2D(GuiGraphicsExtractor graphics, float partialTick) {
        Minecraft minecraft = mc();
        if (minecraft == null || minecraft.player == null) return;
        String dir = minecraft.player.getDirection().getSerializedName().toUpperCase();
        graphics.text(minecraft.font, dir, 4, 92, 0xFF00E8A0, false);
    }
}
