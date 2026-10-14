package com.ttclient.modules.movement;

import com.ttclient.modules.Category;
import com.ttclient.modules.Module;
import com.ttclient.settings.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;

public class Speed extends Module {
    public final NumberSetting multiplier = addSetting(new NumberSetting("Speed", "Horizontal speed multiplier", 1.5, 1.0, 3.0, 0.1));

    public Speed() {
        super("Speed", "Move faster on the ground", Category.MOVEMENT);
    }

    @Override
    public void onTick() {
        Minecraft minecraft = mc();
        if (minecraft == null || minecraft.player == null) return;
        LocalPlayer p = minecraft.player;
        if (!p.onGround()) return;
        Vec3 v = p.getDeltaMovement();
        double m = multiplier.get();
        p.setDeltaMovement(v.x * m, v.y, v.z * m);
    }
}
