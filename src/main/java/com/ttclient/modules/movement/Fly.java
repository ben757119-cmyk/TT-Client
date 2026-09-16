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
        Minecraft mc = mc();
        if (mc == null || mc.player == null || mc.options == null) return;
        LocalPlayer p = mc.player;
        p.getAbilities().flying = true;
        p.getAbilities().mayfly = true;

        double s = speed.get() * 0.5;
        Vec3 look = p.getLookAngle();
        double mx = 0, my = 0, mz = 0;
        if (mc.options.keyUp.isDown()) {
            mx += look.x * s;
            my += look.y * s;
            mz += look.z * s;
        }
        if (mc.options.keyDown.isDown()) {
            mx -= look.x * s;
            my -= look.y * s;
            mz -= look.z * s;
        }
        if (mc.options.keyJump.isDown()) my += s;
        if (mc.options.keyShift.isDown()) my -= s;
        p.setDeltaMovement(mx, my, mz);
        p.fallDistance = 0;
    }

    @Override
    public void onDisable() {
        Minecraft mc = mc();
        if (mc == null || mc.player == null) return;
        if (!mc.player.isCreative() && !mc.player.isSpectator()) {
            mc.player.getAbilities().flying = false;
            mc.player.getAbilities().mayfly = false;
        }
    }
}
