package com.ttclient.modules.movement;

import com.ttclient.modules.Category;
import com.ttclient.modules.Module;
import net.minecraft.client.Minecraft;

public class NoClip extends Module {
    public NoClip() {
        super("NoClip", "Phase through blocks (client collision off)", Category.MOVEMENT);
    }

    @Override
    public void onTick() {
        Minecraft mc = mc();
        if (mc == null || mc.player == null) return;
        mc.player.noPhysics = true;
        mc.player.fallDistance = 0;
    }

    @Override
    public void onDisable() {
        Minecraft mc = mc();
        if (mc == null || mc.player == null) return;
        mc.player.noPhysics = false;
    }
}
