package com.ttclient.modules.movement;

import com.ttclient.modules.Category;
import com.ttclient.modules.Module;
import net.minecraft.client.Minecraft;

public class ToggleSneak extends Module {
    public ToggleSneak() {
        super("ToggleSneak", "Hold sneak while enabled", Category.MOVEMENT);
    }

    @Override
    public void onTick() {
        Minecraft minecraft = mc();
        if (minecraft == null || minecraft.options == null) return;
        minecraft.options.keyShift.setDown(true);
    }

    @Override
    public void onDisable() {
        Minecraft minecraft = mc();
        if (minecraft == null || minecraft.options == null) return;
        minecraft.options.keyShift.setDown(false);
    }
}
