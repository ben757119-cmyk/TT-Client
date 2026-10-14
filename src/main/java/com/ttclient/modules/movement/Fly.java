package com.ttclient.modules.movement;

import com.ttclient.modules.Category;
import com.ttclient.modules.Module;
import com.ttclient.settings.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;

public class Fly extends Module {
    public final NumberSetting speed = addSetting(new NumberSetting("Speed", "Fly speed", 1.0, 0.2, 5.0, 0.1));

    public Fly() {
        super("Fly", "Creative-style flight (client)", Category.MOVEMENT);
    }

    @Override
    public void onTick() {
        Minecraft minecraft = mc();
        if (minecraft == null || minecraft.player == null || minecraft.options == null) return;
        LocalPlayer p = minecraft.player;
        p.getAbilities().flying = true;
        p.getAbilities().mayfly = true;

        double s = speed.get() * 0.5;
        Vec3 look = p.getLookAngle();
        double mx = 0, my = 0, mz = 0;
        if (minecraft.options.keyUp.isDown()) {
            mx += look.x * s;
            my += look.y * s;
            mz += look.z * s;
        }
        if (minecraft.options.keyDown.isDown()) {
            mx -= look.x * s;
            my -= look.y * s;
            mz -= look.z * s;
        }
        if (minecraft.options.keyJump.isDown()) my += s;
        if (minecraft.options.keyShift.isDown()) my -= s;
        p.setDeltaMovement(mx, my, mz);
        p.fallDistance = 0;
    }

    @Override
    public void onDisable() {
        Minecraft minecraft = mc();
        if (minecraft == null || minecraft.player == null) return;
        if (!minecraft.player.isCreative() && !minecraft.player.isSpectator()) {
            minecraft.player.getAbilities().flying = false;
            minecraft.player.getAbilities().mayfly = false;
        }
    }
}
