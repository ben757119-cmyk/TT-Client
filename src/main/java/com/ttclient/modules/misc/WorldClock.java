package com.ttclient.modules.misc;

import com.ttclient.modules.Category;
import com.ttclient.modules.Module;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class WorldClock extends Module {
    private static final DateTimeFormatter REAL = DateTimeFormatter.ofPattern("HH:mm:ss");

    public WorldClock() {
        super("WorldClock", "Show world time and real time", Category.MISC);
        setEnabled(true);
    }

    @Override
    public void onRender2D(GuiGraphicsExtractor g, float partialTick) {
        if (mc().level == null) return;
        long dayTime = mc().level.getGameTime() % 24000L;
        int hours = (int) ((dayTime / 1000L + 6) % 24);
        int minutes = (int) ((dayTime % 1000L) * 60 / 1000);
        String world = String.format("World %02d:%02d", hours, minutes);
        String real = "Real " + LocalTime.now().format(REAL);
        int x = 4;
        int y = mc().getWindow().getGuiScaledHeight() - 26;
        g.text(mc().font, world, x, y, 0xFF00E8A0, false);
        g.text(mc().font, real, x, y + 11, 0xFF8B95A8, false);
    }
}
