package com.ttclient.modules.movement;

import com.ttclient.modules.Category;
import com.ttclient.modules.Module;
import com.ttclient.settings.BoolSetting;
import com.ttclient.settings.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

/**
 * One-shot: fly + noclip toward the nearest player, then auto-disable.
 * Default bind: mouse side button (MOUSE4). Rebind in ClickGUI.
 */
public class WarpNearest extends Module {
    public final NumberSetting speed = addSetting(new NumberSetting(
            "Speed", "Blocks per tick while warping", 4.0, 0.5, 20.0, 0.5));
    public final NumberSetting arrive = addSetting(new NumberSetting(
            "Arrive", "Stop distance", 1.5, 0.5, 5.0, 0.1));
    public final BoolSetting instant = addSetting(new BoolSetting(
            "Instant", "Teleport in one step (still client-side)", false));
    public final NumberSetting maxTime = addSetting(new NumberSetting(
            "MaxTicks", "Safety timeout", 80, 20, 200, 5));

    /** GLFW_MOUSE_BUTTON_4 (side) encoded as 2000+button */
    public static final int BIND_MOUSE4 = 2000 + GLFW.GLFW_MOUSE_BUTTON_4;
    public static final int BIND_MOUSE5 = 2000 + GLFW.GLFW_MOUSE_BUTTON_5;

    private Player target;
    private int ticks;
    private boolean savedNoPhysics;

    public WarpNearest() {
        super("WarpNearest", "Fly/noclip to nearest player, then off", Category.MOVEMENT);
        setKeyBind(BIND_MOUSE4);
    }

    @Override
    public void onEnable() {
        ticks = 0;
        target = null;
        Minecraft mc = mc();
        if (mc == null || mc.player == null || mc.level == null) {
            setEnabled(false);
            return;
        }
        target = findNearest(mc, mc.player);
        if (target == null) {
            setEnabled(false);
            return;
        }

        LocalPlayer p = mc.player;
        savedNoPhysics = p.noPhysics;
        p.noPhysics = true;
        p.fallDistance = 0;

        if (instant.get()) {
            p.setPos(target.getX(), target.getY(), target.getZ());
            p.setDeltaMovement(Vec3.ZERO);
            setEnabled(false);
        }
    }

    @Override
    public void onDisable() {
        Minecraft mc = mc();
        if (mc != null && mc.player != null) {
            mc.player.noPhysics = savedNoPhysics;
            mc.player.setDeltaMovement(Vec3.ZERO);
        }
        target = null;
        ticks = 0;
    }

    @Override
    public void onTick() {
        Minecraft mc = mc();
        if (mc == null || mc.player == null || mc.level == null) {
            setEnabled(false);
            return;
        }
        LocalPlayer p = mc.player;

        if (target == null || !target.isAlive()) {
            target = findNearest(mc, p);
            if (target == null) {
                setEnabled(false);
                return;
            }
        }

        ticks++;
        if (ticks > maxTime.getInt()) {
            setEnabled(false);
            return;
        }

        p.noPhysics = true;
        p.fallDistance = 0;
        p.setOnGround(false);

        Vec3 goal = new Vec3(target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ());
        Vec3 pos = p.position();
        Vec3 diff = goal.subtract(pos);
        double dist = diff.length();

        if (dist <= arrive.get()) {
            p.setPos(goal.x, target.getY(), goal.z);
            p.setDeltaMovement(Vec3.ZERO);
            setEnabled(false);
            return;
        }

        double step = Math.min(speed.get(), dist);
        Vec3 next = pos.add(diff.normalize().scale(step));
        p.setPos(next.x, next.y, next.z);
        p.setDeltaMovement(Vec3.ZERO);
    }

    private static Player findNearest(Minecraft mc, LocalPlayer self) {
        Player best = null;
        double bestD = Double.MAX_VALUE;
        for (Player pl : mc.level.players()) {
            if (pl == self || !pl.isAlive()) continue;
            double d = self.distanceToSqr(pl);
            if (d < bestD) {
                bestD = d;
                best = pl;
            }
        }
        return best;
    }
}
