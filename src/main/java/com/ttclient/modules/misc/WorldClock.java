package com.ttclient.modules.misc;

import com.ttclient.modules.Category;
import com.ttclient.modules.Module;
import net.minecraft.client.gui.GuiGraphics;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class WorldClock extends Module {
    private static final DateTimeFormatter REAL = DateTimeFormatter.ofPattern("HH:mm:ss");

    public WorldClock() {
        super("WorldClock", "Show world time and real time", Category.MISC);
        setEnabled(true);
    }

    @Override
    public void onRender2D(GuiGraphics g, float partialTick) {
        if (mc.level == null || mc.options.hideGui) return;
        long dayTime = mc.level.getGameTime() % 24000L;
        int hours = (int) ((dayTime / 1000L + 6) % 24);
        int minutes = (int) ((dayTime % 1000L) * 60 / 1000);
        String world = String.format("World §f%02d:%02d", hours, minutes);
        String real = "Real §f" + LocalTime.now().format(REAL);
        int x = 4;
        int y = mc.getWindow().getGuiScaledHeight() - 26;
        g.drawString(mc.font, world, x + 1, y + 1, 0x000000, false);
        g.drawString(mc.font, world, x, y, 0xFF00E8A0, false);
        g.drawString(mc.font, real, x + 1, y + 12, 0x000000, false);
        g.drawString(mc.font, real, x, y + 11, 0xFF8B95A8, false);
    }
}
