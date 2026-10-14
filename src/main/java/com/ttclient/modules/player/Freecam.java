package com.ttclient.modules.player;

import com.ttclient.modules.Category;
import com.ttclient.modules.Module;
import com.ttclient.modules.movement.Fly;
import com.ttclient.settings.BoolSetting;
import com.ttclient.settings.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;

/**
 * Free-fly through blocks at the player entity (client).
 * True camera-only freecam needs mixins; this is a solid no-clip fly that restores on disable.
 * Movement is yaw-based only — same speed every horizontal direction.
 */
public class Freecam extends Module {
    public final NumberSetting speed = addSetting(new NumberSetting("Speed", "Move speed", 1.5, 0.2, 5.0, 0.1));
    public final BoolSetting restore = addSetting(new BoolSetting("Restore", "Teleport back when disabled", true));

    private double sx, sy, sz;
    private float syaw, spitch;
    private boolean saved;
    private boolean wasFlying;
    private boolean wasMayfly;

    public Freecam() {
        super("Freecam", "No-clip free fly; restores position when off", Category.PLAYER);
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

        wasFlying = p.getAbilities().flying;
        wasMayfly = p.getAbilities().mayfly;

        p.getAbilities().mayfly = true;
        p.getAbilities().flying = true;
        p.noPhysics = true;
        p.setDeltaMovement(Vec3.ZERO);
        p.fallDistance = 0;
        p.setOnGround(false);
    }

    @Override
    public void onDisable() {
        Minecraft mc = mc();
        if (mc == null || mc.player == null) return;
        LocalPlayer p = mc.player;

        p.noPhysics = false;
        p.setDeltaMovement(Vec3.ZERO);

        if (!p.isCreative() && !p.isSpectator()) {
            p.getAbilities().flying = wasFlying;
            p.getAbilities().mayfly = wasMayfly;
        } else {
            p.getAbilities().flying = wasFlying;
            p.getAbilities().mayfly = wasMayfly;
        }

        if (restore.get() && saved) {
            try {
                p.snapTo(sx, sy, sz, syaw, spitch);
            } catch (Throwable t) {
                p.setPos(sx, sy, sz);
                p.setYRot(syaw);
                p.setXRot(spitch);
            }
            p.setDeltaMovement(Vec3.ZERO);
        }
        saved = false;
    }

    @Override
    public void onTick() {
        Minecraft mc = mc();
        if (mc == null || mc.player == null || mc.options == null) return;
        LocalPlayer p = mc.player;

        // Keep state every tick (server/other logic can clear it)
        p.getAbilities().mayfly = true;
        p.getAbilities().flying = true;
        p.noPhysics = true;
        p.fallDistance = 0;
        p.setOnGround(false);

        double s = speed.get() * 0.5;
        Vec3 horiz = Fly.horizontalMove(p.getYRot(), s, mc);
        double my = 0;
        if (mc.options.keyJump.isDown()) my += s;
        if (mc.options.keyShift.isDown()) my -= s;

        // Apply velocity and force position (helps when noPhysics alone is ignored)
        p.setDeltaMovement(horiz.x, my, horiz.z);
        p.setPos(p.getX() + horiz.x, p.getY() + my, p.getZ() + horiz.z);
    }
}
