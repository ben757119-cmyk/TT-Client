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
        Minecraft minecraft = mc();
        if (minecraft == null || minecraft.player == null || minecraft.options == null) return;
        minecraft.options.keyUp.setDown(true);
    }

    @Override
    public void onDisable() {
        Minecraft minecraft = mc();
        if (minecraft == null || minecraft.options == null) return;
        minecraft.options.keyUp.setDown(false);
    }
}
