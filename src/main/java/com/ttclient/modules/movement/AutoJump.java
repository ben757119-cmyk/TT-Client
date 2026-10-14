package com.ttclient.modules.movement;

import com.ttclient.modules.Category;
import com.ttclient.modules.Module;
import net.minecraft.client.Minecraft;

public class AutoJump extends Module {
    public AutoJump() {
        super("AutoJump", "Jump when on ground", Category.MOVEMENT);
    }

    @Override
    public void onTick() {
        Minecraft minecraft = mc();
        if (minecraft == null || minecraft.player == null || minecraft.options == null) return;
        if (minecraft.player.onGround()) {
            minecraft.options.keyJump.setDown(true);
        }
    }

    @Override
    public void onDisable() {
        Minecraft minecraft = mc();
        if (minecraft == null || minecraft.options == null) return;
        minecraft.options.keyJump.setDown(false);
    }
}
