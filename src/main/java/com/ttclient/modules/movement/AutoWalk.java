package com.ttclient.modules.movement;

import com.ttclient.modules.Category;
import com.ttclient.modules.Module;
import net.minecraft.client.Minecraft;

public class AutoWalk extends Module {
    public AutoWalk() {
        super("AutoWalk", "Automatically walk forward", Category.MOVEMENT);
    }

    @Override
    public void onTick() {
        Minecraft mc = mc();
        if (mc == null || mc.player == null || mc.options == null) return;
        mc.options.keyUp.setDown(true);
    }

    @Override
    public void onDisable() {
        Minecraft mc = mc();
        if (mc == null || mc.options == null) return;
        mc.options.keyUp.setDown(false);
    }
}
