package com.ttclient.modules.movement;

import com.ttclient.modules.Category;
import com.ttclient.modules.Module;
import com.ttclient.settings.BoolSetting;
import com.ttclient.settings.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;

/**
 * Velocity-based fly (yaw only). Does not enable creative flying ability by default
 * so AntiFlightKick / servers see less of the vanilla fly flag.
 */
public class Fly extends Module {
    public final NumberSetting speed = addSetting(new NumberSetting("Speed", "Fly speed", 1.0, 0.2, 5.0, 0.1));
    public final BoolSetting useAbility = addSetting(new BoolSetting(
            "CreativeFlag", "Set abilities.flying (easier control, easier kicks)", false));

    public Fly() {
        super("Fly", "Fly with equal speed in every direction (yaw-based)", Category.MOVEMENT);
    }

    @Override
    public void onTick() {
        Minecraft mc = mc();
        if (mc == null || mc.player == null || mc.options == null) return;
        LocalPlayer p = mc.player;

        // Prefer velocity fly without the creative ability bit
        if (useAbility.get()) {
            p.getAbilities().flying = true;
            p.getAbilities().mayfly = true;
        } else {
            p.getAbilities().flying = false;
        }

        p.fallDistance = 0;

        double s = speed.get() * 0.5;
        Vec3 move = horizontalMove(p.getYRot(), s, mc);
        double my = 0;
        if (mc.options.keyJump.isDown()) my += s;
        if (mc.options.keyShift.isDown()) my -= s;

        // Cancel gravity when not holding vertical keys
        if (my == 0 && !useAbility.get()) {
            // Hold altitude — zero Y velocity when idle vertically
            my = 0;
        }

        p.setDeltaMovement(move.x, my, move.z);
        p.setOnGround(false);
    }

    public static Vec3 horizontalMove(float yawDeg, double s, Minecraft mc) {
        double rad = Math.toRadians(yawDeg);
        double sin = Math.sin(rad);
        double cos = Math.cos(rad);

        double mx = 0;
        double mz = 0;

        if (mc.options.keyUp.isDown()) {
            mx -= sin;
            mz += cos;
        }
        if (mc.options.keyDown.isDown()) {
            mx += sin;
            mz -= cos;
        }
        if (mc.options.keyLeft.isDown()) {
            mx += cos;
            mz += sin;
        }
        if (mc.options.keyRight.isDown()) {
            mx -= cos;
            mz -= sin;
        }

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
