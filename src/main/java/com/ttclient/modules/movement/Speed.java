package com.ttclient.modules.movement;

import com.ttclient.modules.Category;
import com.ttclient.modules.Module;
import com.ttclient.settings.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

public class Speed extends Module {
    public final NumberSetting multiplier = addSetting(new NumberSetting("Multiplier", "Speed multiplier", 1.3, 1.0, 3.0, 0.05));

    public Speed() {
        super("Speed", "Move faster on ground", Category.MOVEMENT);
    }

    @Override
    public void onTick() {
        Minecraft mc = mc();
        if (mc == null || mc.player == null || mc.options == null) return;
        LocalPlayer p = mc.player;
        if (!p.onGround() || p.isInWater() || p.isInLava() || p.isFallFlying()) return;

        boolean forward = mc.options.keyUp.isDown();
        boolean back = mc.options.keyDown.isDown();
        boolean left = mc.options.keyLeft.isDown();
        boolean right = mc.options.keyRight.isDown();
        if (!forward && !back && !left && !right) return;

        float fwd = (forward ? 1 : 0) - (back ? 1 : 0);
        float str = (left ? 1 : 0) - (right ? 1 : 0);
        double yaw = Math.toRadians(p.getYRot());
        double speed = 0.26 * multiplier.get();
        double mx = -Math.sin(yaw) * fwd + Math.cos(yaw) * str;
        double mz = Math.cos(yaw) * fwd + Math.sin(yaw) * str;
        double len = Math.sqrt(mx * mx + mz * mz);
        if (len < 1.0E-4) return;
        mx = mx / len * speed;
        mz = mz / len * speed;
        p.setDeltaMovement(mx, p.getDeltaMovement().y, mz);
    }
}
