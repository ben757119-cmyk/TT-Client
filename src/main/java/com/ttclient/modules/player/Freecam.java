package com.ttclient.modules.player;

import com.ttclient.modules.Category;
import com.ttclient.modules.Module;
import com.ttclient.settings.BoolSetting;
import com.ttclient.settings.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

/**
 * Detached camera freecam.
 * Body stays at enable position (server sees that).
 * Camera flies with WASD / jump / sneak; MixinCamera applies position every frame.
 */
public class Freecam extends Module {
    public final NumberSetting speed = addSetting(new NumberSetting(
            "Speed", "Camera fly speed", 2.0, 0.1, 20.0, 0.1));
    public final NumberSetting vSpeed = addSetting(new NumberSetting(
            "VSpeed", "Vertical speed multiplier", 1.0, 0.2, 3.0, 0.1));
    public final BoolSetting freezeLook = addSetting(new BoolSetting(
            "FreezeBodyLook", "Lock body yaw/pitch at enable values", false));

    private double originX, originY, originZ;
    private float originYaw, originPitch;
    private boolean saved;

    private double camX, camY, camZ;
    private float camYaw, camPitch;

    public Freecam() {
        super("Freecam", "Detach camera only — body stays put", Category.PLAYER);
        setKeyBind(GLFW.GLFW_KEY_F4);
    }

    public static Freecam get() {
        try {
            var mods = com.ttclient.client.TTClientClient.modules;
            if (mods == null) return null;
            return mods.getModule(Freecam.class);
        } catch (Throwable t) {
            return null;
        }
    }

    public boolean active() {
        return isEnabled() && saved;
    }

    public double getCamX() { return camX; }
    public double getCamY() { return camY; }
    public double getCamZ() { return camZ; }
    public float getCamYaw() { return camYaw; }
    public float getCamPitch() { return camPitch; }

    @Override
    public void onEnable() {
        Minecraft mc = mc();
        if (mc == null || mc.player == null) {
            setEnabled(false);
            return;
        }
        LocalPlayer p = mc.player;
        originX = p.getX();
        originY = p.getY();
        originZ = p.getZ();
        originYaw = p.getYRot();
        originPitch = p.getXRot();

        camX = p.getX();
        camY = p.getY() + p.getEyeHeight();
        camZ = p.getZ();
        camYaw = p.getYRot();
        camPitch = p.getXRot();
        saved = true;
        p.setDeltaMovement(Vec3.ZERO);
        p.fallDistance = 0;
    }

    @Override
    public void onDisable() {
        Minecraft mc = mc();
        if (mc != null && mc.player != null && saved) {
            LocalPlayer p = mc.player;
            try {
                p.snapTo(originX, originY, originZ, originYaw, originPitch);
            } catch (Throwable t) {
                p.setPos(originX, originY, originZ);
                p.setYRot(originYaw);
                p.setXRot(originPitch);
            }
            p.setDeltaMovement(Vec3.ZERO);
        }
        saved = false;
    }

    @Override
    public void onTick() {
        Minecraft mc = mc();
        if (mc == null || mc.player == null || !saved) return;
        LocalPlayer p = mc.player;

        if (!freezeLook.get()) {
            camYaw = p.getYRot();
            camPitch = p.getXRot();
        }

        moveCamera(mc);
        holdBody(p);
    }

    private void moveCamera(Minecraft mc) {
        double sp = speed.get();
        double vsp = sp * vSpeed.get();
        double yawRad = Math.toRadians(camYaw);
        double sin = Math.sin(yawRad);
        double cos = Math.cos(yawRad);

        double dx = 0, dy = 0, dz = 0;
        try {
            if (mc.options.keyUp.isDown()) { dx -= sin * sp; dz += cos * sp; }
            if (mc.options.keyDown.isDown()) { dx += sin * sp; dz -= cos * sp; }
            if (mc.options.keyLeft.isDown()) { dx -= cos * sp; dz -= sin * sp; }
            if (mc.options.keyRight.isDown()) { dx += cos * sp; dz += sin * sp; }
            if (mc.options.keyJump.isDown()) dy += vsp;
            if (mc.options.keyShift.isDown()) dy -= vsp;
        } catch (Throwable ignored) {}

        camX += dx;
        camY += dy;
        camZ += dz;
    }

    private void holdBody(LocalPlayer p) {
        try {
            p.setDeltaMovement(Vec3.ZERO);
            p.fallDistance = 0;
            float yaw = freezeLook.get() ? originYaw : camYaw;
            float pitch = freezeLook.get() ? originPitch : camPitch;
            try {
                p.snapTo(originX, originY, originZ, yaw, pitch);
            } catch (Throwable t) {
                p.setPos(originX, originY, originZ);
                if (freezeLook.get()) {
                    p.setYRot(yaw);
                    p.setXRot(pitch);
                }
            }
            // Zero movement impulses via reflection (26.2 Input field names vary)
            try {
                Object in = p.input;
                if (in != null) {
                    for (var f : in.getClass().getDeclaredFields()) {
                        f.setAccessible(true);
                        if (f.getType() == float.class) {
                            f.setFloat(in, 0f);
                        }
                    }
                }
            } catch (Throwable ignored) {}
        } catch (Throwable ignored) {}
    }

    public void onCameraAngles(net.neoforged.neoforge.client.event.ViewportEvent.ComputeCameraAngles event) {
        if (!active()) return;
        try {
            event.setYaw(camYaw);
            event.setPitch(camPitch);
        } catch (Throwable ignored) {}
    }
}
