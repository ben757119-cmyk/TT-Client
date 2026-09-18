package com.ttclient.modules.render;

import com.ttclient.modules.Category;
import com.ttclient.modules.Module;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LightLayer;

public class LightLevel extends Module {
    public LightLevel() {
        super("LightLevel", "Show block and sky light at your feet", Category.RENDER);
        setEnabled(true);
    }

    @Override
    public void onRender2D(GuiGraphics g, float partialTick) {
        if (mc.player == null || mc.level == null || mc.options.hideGui) return;
        BlockPos pos = mc.player.blockPosition();
        int block = mc.level.getBrightness(LightLayer.BLOCK, pos);
        int sky = mc.level.getBrightness(LightLayer.SKY, pos);
        String text = "Light §f" + block + " §7block  §f" + sky + " §7sky";
        int y = mc.getWindow().getGuiScaledHeight() - 24;
        g.drawString(mc.font, text, 4, y, 0xFF00E8A0, false);
    }
}
