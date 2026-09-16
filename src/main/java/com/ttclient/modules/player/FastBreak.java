package com.ttclient.modules.player;

import com.ttclient.modules.Category;
import com.ttclient.modules.Module;
import net.minecraft.client.Minecraft;

public class FastBreak extends Module {
    public FastBreak() {
        super("FastBreak", "Speed up block breaking progress", Category.PLAYER);
    }

    @Override
    public void onTick() {
        Minecraft mc = mc();
        if (mc == null || mc.gameMode == null) return;
        try {
            var progress = mc.gameMode.getClass().getDeclaredField("destroyProgress");
            progress.setAccessible(true);
            float p = progress.getFloat(mc.gameMode);
            if (p > 0 && p < 1f) {
                progress.setFloat(mc.gameMode, Math.min(1f, p + 0.2f));
            }
        } catch (Exception ignored) {}
        try {
            var delay = mc.gameMode.getClass().getDeclaredField("destroyDelay");
            delay.setAccessible(true);
            delay.setInt(mc.gameMode, 0);
        } catch (Exception ignored) {}
    }
}
