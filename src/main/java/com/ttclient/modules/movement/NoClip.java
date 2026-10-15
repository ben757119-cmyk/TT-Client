package com.ttclient.modules.movement;

import com.ttclient.modules.Category;
import com.ttclient.modules.Module;
import com.ttclient.settings.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;

/**
 * Client no-clip: disable collision and move through blocks with WASD + jump/sneak.
 * noPhysics alone is not enough — position must be applied each tick.
 * Multiplayer servers may still rubberband hard movement.
 */
public class NoClip extends Module {
    public final NumberSetting speed = addSetting(new NumberSetting(
            "Speed", "Phase move speed", 1.0, 0.2, 5.0, 0.1));

    public NoClip() {
        super("NoClip", "Phase through blocks (client)", Category.MOVEMENT);
    }

    @Override
    public void onEnable() {
        Minecraft mc = mc();
        if (mc == null || mc.player == null) {
            setEnabled(false);
            return;
        }
        LocalPlayer p = mc.player;
        p.noPhysics = true;
        p.fallDistance = 0;
        p.setOnGround(false);
        p.setDeltaMovement(Vec3.ZERO);
    }

    @Override
    public void onDisable() {
        Minecraft mc = mc();
        if (mc == null || mc.player == null) return;
        LocalPlayer p = mc.player;
        p.noPhysics = false;
        p.setDeltaMovement(Vec3.ZERO);
    }

    @Override
    public void onTick() {
        Minecraft mc = mc();
        if (mc == null || mc.player == null || mc.options == null) return;
        LocalPlayer p = mc.player;

        // Keep collision off every tick (other systems can clear it)
        p.noPhysics = true;
        p.fallDistance = 0;
        p.setOnGround(false);

        double s = speed.get() * 0.5;
        Vec3 horiz = Fly.horizontalMove(p.getYRot(), s, mc);
        double my = 0;
        if (mc.options.keyJump.isDown()) my += s;
        if (mc.options.keyShift.isDown()) my -= s;

        // When not holding movement keys, cancel gravity so you don't sink in blocks
        if (horiz.x == 0 && horiz.z == 0 && my == 0) {
            p.setDeltaMovement(0, 0, 0);
            return;
        }

        p.setDeltaMovement(horiz.x, my, horiz.z);
        // Force position so we actually phase (collision ignored via noPhysics)
        p.setPos(p.getX() + horiz.x, p.getY() + my, p.getZ() + horiz.z);
    }
}
