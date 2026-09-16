package com.ttclient.modules.player;

import com.ttclient.modules.Category;
import com.ttclient.modules.Module;
import net.minecraft.client.Minecraft;

public class NoFall extends Module {
    public NoFall() {
        super("NoFall", "Cancel fall damage (client fallDistance)", Category.PLAYER);
    }

    @Override
    public void onTick() {
        Minecraft mc = mc();
        if (mc == null || mc.player == null) return;
        if (mc.player.fallDistance > 2.0f) {
            mc.player.fallDistance = 0;
        }
    }
}
