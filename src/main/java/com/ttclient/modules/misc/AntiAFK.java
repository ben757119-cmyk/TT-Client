package com.ttclient.modules.misc;

import com.ttclient.modules.Category;
import com.ttclient.modules.Module;
import net.minecraft.client.Minecraft;

public class AntiAFK extends Module {
    private int timer;

    public AntiAFK() {
        super("AntiAFK", "Small movement to avoid AFK kick", Category.MISC);
    }

    @Override
    public void onTick() {
        Minecraft minecraft = mc();
        if (minecraft == null || minecraft.player == null) return;
        timer++;
        if (timer % 100 == 0) {
            minecraft.player.setYRot(minecraft.player.getYRot() + 1.5f);
        }
    }
}
