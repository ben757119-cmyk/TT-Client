package com.ttclient.modules.movement;

import com.ttclient.modules.Category;
import com.ttclient.modules.Module;
import com.ttclient.settings.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

public class Strafe extends Module {
    public final NumberSetting speed = addSetting(new NumberSetting("Speed", "Air strafe speed", 0.25, 0.1, 0.5, 0.01));

    public Strafe() {
        super("Strafe", "Control movement direction in air", Category.MOVEMENT);
    }

    @Override
    public void onTick() {
        Minecraft mc = mc();
        if (mc == null || mc.player == null || mc.options == null) return;
        LocalPlayer p = mc.player;
        if (p.onGround()) return;
        boolean f = mc.options.keyUp.isDown();
        boolean b = mc.options.keyDown.isDown();
        boolean l = mc.options.keyLeft.isDown();
        boolean r = mc.options.keyRight.isDown();
        if (!f && !b && !l && !r) return;
        float fwd = (f ? 1 : 0) - (b ? 1 : 0);
        float str = (l ? 1 : 0) - (r ? 1 : 0);
        double yaw = Math.toRadians(p.getYRot());
        double s = speed.get();
        double mx = -Math.sin(yaw) * fwd + Math.cos(yaw) * str;
        double mz = Math.cos(yaw) * fwd + Math.sin(yaw) * str;
        double len = Math.sqrt(mx * mx + mz * mz);
        if (len < 1e-4) return;
        p.setDeltaMovement(mx / len * s, p.getDeltaMovement().y, mz / len * s);
    }
}
