package com.ttclient.modules.movement;

import com.ttclient.modules.Category;
import com.ttclient.modules.Module;
import net.minecraft.client.Minecraft;

public class Sprint extends Module {
    public Sprint() {
        super("Sprint", "Always sprint when moving forward", Category.MOVEMENT);
    }

    @Override
    public void onTick() {
        Minecraft minecraft = mc();
        if (minecraft == null || minecraft.player == null || minecraft.options == null) return;
        if (minecraft.options.keyUp.isDown() && !minecraft.player.isShiftKeyDown()) {
            minecraft.player.setSprinting(true);
        }
    }
}
