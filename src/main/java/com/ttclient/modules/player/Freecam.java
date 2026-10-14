package com.ttclient.modules.player;

import com.ttclient.modules.Category;
import com.ttclient.modules.Module;
import com.ttclient.settings.BoolSetting;
import com.ttclient.settings.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;

/**
 * Client freecam: free-fly camera, restores position on disable.
 * Uses noPhysics + local movement (no mixins).
 */
public class Freecam extends Module {
    public final NumberSetting speed = addSetting(new NumberSetting("Speed", "Fly speed", 1.5, 0.2, 5.0, 0.1));
    public final BoolSetting restore = addSetting(new BoolSetting("Restore", "Teleport back when disabled", true));

    private double sx, sy, sz;
    private float syaw, spitch;
    private boolean saved;

    public Freecam() {
        super("Freecam", "Detach and fly the camera freely", Category.PLAYER);
    }

    @Override
    public void onEnable() {
        Minecraft mc = mc();
        if (mc == null || mc.player == null) {
            setEnabled(false);
            return;
        }
        LocalPlayer p = mc.player;
        sx = p.getX();
        sy = p.getY();
        sz = p.getZ();
        syaw = p.getYRot();
        spitch = p.getXRot();
        saved = true;
        p.noPhysics = true;
        p.setDeltaMovement(Vec3.ZERO);
        p.fallDistance = 0;
    }

    @Override
    public void onDisable() {
        Minecraft mc = mc();
        if (mc == null || mc.player == null) return;
        LocalPlayer p = mc.player;
        p.noPhysics = false;
        p.setDeltaMovement(Vec3.ZERO);
        if (restore.get() && saved) {
            p.snapTo(sx, sy, sz, syaw, spitch);
            p.setPos(sx, sy, sz);
            p.setYRot(syaw);
            p.setXRot(spitch);
        }
        saved = false;
    }

    @Override
    public void onTick() {
        Minecraft mc = mc();
        if (mc == null || mc.player == null || mc.options == null) return;
        LocalPlayer p = mc.player;

        p.noPhysics = true;
        p.fallDistance = 0;
        p.setOnGround(false);

        float yaw = p.getYRot();
        double rad = Math.toRadians(yaw);
        double s = speed.get();

        double mx = 0;
        double my = 0;
        double mz = 0;

        if (mc.options.keyUp.isDown()) {
            mx -= Math.sin(rad) * s;
            mz += Math.cos(rad) * s;
        }
        if (mc.options.keyDown.isDown()) {
            mx += Math.sin(rad) * s;
            mz -= Math.cos(rad) * s;
        }
        if (mc.options.keyLeft.isDown()) {
            mx -= Math.cos(rad) * s;
            mz -= Math.sin(rad) * s;
        }
        if (mc.options.keyRight.isDown()) {
            mx += Math.cos(rad) * s;
            mz += Math.sin(rad) * s;
        }
        if (mc.options.keyJump.isDown()) my += s;
        if (mc.options.keyShift.isDown()) my -= s;

        // Normalize horizontal if strafing diagonally
        double h = Math.sqrt(mx * mx + mz * mz);
        if (h > s && h > 1e-6) {
            mx = mx / h * s;
            mz = mz / h * s;
        }

        p.setDeltaMovement(mx, my, mz);
        p.setPos(p.getX() + mx, p.getY() + my, p.getZ() + mz);
    }
}
