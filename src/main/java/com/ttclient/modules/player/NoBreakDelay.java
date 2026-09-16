package com.ttclient.modules.player;

import com.ttclient.modules.Category;
import com.ttclient.modules.Module;
import net.minecraft.client.Minecraft;

public class NoBreakDelay extends Module {
    public NoBreakDelay() {
        super("NoBreakDelay", "Remove delay between breaking blocks", Category.PLAYER);
    }

    @Override
    public void onTick() {
        Minecraft mc = mc();
        if (mc == null || mc.gameMode == null) return;
        try {
            var f = mc.gameMode.getClass().getDeclaredField("destroyDelay");
            f.setAccessible(true);
            f.setInt(mc.gameMode, 0);
        } catch (Exception ignored) {
            try {
                var f = mc.gameMode.getClass().getDeclaredField("blockHitDelay");
                f.setAccessible(true);
                f.setInt(mc.gameMode, 0);
            } catch (Exception ignored2) {}
        }
    }
}
