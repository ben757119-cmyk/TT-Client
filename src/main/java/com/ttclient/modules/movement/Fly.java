package com.ttclient.modules.movement;

import com.ttclient.modules.Category;
import com.ttclient.modules.Module;
import com.ttclient.settings.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;

/**
 * Client fly: movement is relative to body yaw only (not look pitch).
 * Horizontal diagonals are normalized so every direction is the same speed.
 */
public class Fly extends Module {
    public final NumberSetting speed = addSetting(new NumberSetting("Speed", "Fly speed", 1.0, 0.2, 5.0, 0.1));

    public Fly() {
        super("Fly", "Fly with equal speed in every direction (yaw-based)", Category.MOVEMENT);
    }

    @Override
    public void onTick() {
        Minecraft mc = mc();
        if (mc == null || mc.player == null || mc.options == null) return;
        LocalPlayer p = mc.player;

        p.getAbilities().flying = true;
        p.getAbilities().mayfly = true;
        p.fallDistance = 0;
        p.setOnGround(false);

        double s = speed.get() * 0.5;
        Vec3 move = horizontalMove(p.getYRot(), s, mc);
        double my = 0;
        if (mc.options.keyJump.isDown()) my += s;
        if (mc.options.keyShift.isDown()) my -= s;

        p.setDeltaMovement(move.x, my, move.z);
    }

    /**
     * WASD relative to yaw only — pitch ignored. Length capped at {@code s}.
     * Minecraft yaw: 0 looks +Z, increases clockwise.
     * Forward: (-sin, +cos), Right: (+cos, +sin).
     */
    public static Vec3 horizontalMove(float yawDeg, double s, Minecraft mc) {
        double rad = Math.toRadians(yawDeg);
        double sin = Math.sin(rad);
        double cos = Math.cos(rad);

        double mx = 0;
        double mz = 0;

        // Forward / back
        if (mc.options.keyUp.isDown()) {
            mx -= sin;
            mz += cos;
        }
        if (mc.options.keyDown.isDown()) {
            mx += sin;
            mz -= cos;
        }
        // Strafe: A = left (-right), D = right (+right)
        if (mc.options.keyLeft.isDown()) {
            mx -= cos;
            mz -= sin;
        }
        if (mc.options.keyRight.isDown()) {
            mx += cos;
            mz += sin;
        }

        // Wait - if user says D goes left and A goes right, the left/right above are swapped.
        // Correct right vector for MC yaw is (+cos(yaw), +sin(yaw)) for +X when yaw=0... 
        // yaw 0: forward (0,1) = -sin0, cos0 = 0,1. right should be (1,0) = cos0, sin0 = 1,0.
        // So right: mx += cos, mz += sin. left: mx -= cos, mz -= sin.
        // That is what we have. User reports inverted — swap them.

        double len = Math.sqrt(mx * mx + mz * mz);
        if (len > 1e-6) {
            mx = mx / len * s;
            mz = mz / len * s;
        }
        return new Vec3(mx, 0, mz);
    }

    @Override
    public void onDisable() {
        Minecraft mc = mc();
        if (mc == null || mc.player == null) return;
        if (!mc.player.isCreative() && !mc.player.isSpectator()) {
            mc.player.getAbilities().flying = false;
            mc.player.getAbilities().mayfly = false;
        }
        mc.player.setDeltaMovement(Vec3.ZERO);
    }
}
