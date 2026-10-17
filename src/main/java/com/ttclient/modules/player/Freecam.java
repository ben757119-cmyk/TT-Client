package com.ttclient.modules.player;

import com.ttclient.modules.Category;
import com.ttclient.modules.Module;
import com.ttclient.settings.BoolSetting;
import com.ttclient.settings.NumberSetting;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

/**
 * True freecam: body stays where you enabled it (server still sees that spot).
 * Only the camera flies with WASD / space / sneak — through walls, no body move.
 *
 * Anti-ban design:
 * - Player entity is snapped back to origin every tick
 * - Velocity forced to zero
 * - Movement keys do not move the player; they only move the free camera
 */
public class Freecam extends Module {
    public final NumberSetting speed = addSetting(new NumberSetting(
            "Speed", "Camera fly speed", 1.5, 0.1, 10.0, 0.1));
    public final NumberSetting vSpeed = addSetting(new NumberSetting(
            "VSpeed", "Vertical camera speed multiplier", 1.0, 0.2, 3.0, 0.1));
    public final BoolSetting freezeRot = addSetting(new BoolSetting(
            "FreezeBodyLook", "Lock body head rotation (usually leave off)", false));

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
        if (mc == null || mc.player == null) {
            saved = false;
            return;
        }
        LocalPlayer p = mc.player;
        if (saved) {
            try {
                p.snapTo(originX, originY, originZ, originYaw, originPitch);
            } catch (Throwable t) {
                p.setPos(originX, originY, originZ);
                p.setYRot(originYaw);
                p.setXRot(originPitch);
            }
        }
        p.setDeltaMovement(Vec3.ZERO);
        p.noPhysics = false;
        saved = false;
    }

    @Override
    public void onTick() {
        Minecraft mc = mc();
        if (mc == null || mc.player == null || !saved) return;
        LocalPlayer p = mc.player;

        camYaw = p.getYRot();
        camPitch = p.getXRot();

        updateCameraMotion(mc);
        freezeBody(p);
        applyCamera(mc);
    }

    private void updateCameraMotion(Minecraft mc) {
        double sp = speed.get();
        double vsp = sp * vSpeed.get();

        float yaw = camYaw;
        double rad = Math.toRadians(yaw);
        double sin = Math.sin(rad);
        double cos = Math.cos(rad);

        double dx = 0, dy = 0, dz = 0;

        try {
            if (mc.options.keyUp.isDown()) {
                dx -= sin * sp;
                dz += cos * sp;
            }
            if (mc.options.keyDown.isDown()) {
                dx += sin * sp;
                dz -= cos * sp;
            }
            if (mc.options.keyLeft.isDown()) {
                dx -= cos * sp;
                dz -= sin * sp;
            }
            if (mc.options.keyRight.isDown()) {
                dx += cos * sp;
                dz += sin * sp;
            }
            if (mc.options.keyJump.isDown()) {
                dy += vsp;
            }
            if (mc.options.keyShift.isDown()) {
                dy -= vsp;
            }
        } catch (Throwable ignored) {}

        camX += dx;
        camY += dy;
        camZ += dz;
    }

    private void freezeBody(LocalPlayer p) {
        try {
            p.setDeltaMovement(Vec3.ZERO);
            p.fallDistance = 0;
            p.noPhysics = false;

            float yaw = freezeRot.get() ? originYaw : camYaw;
            float pitch = freezeRot.get() ? originPitch : camPitch;
            try {
                p.snapTo(originX, originY, originZ, yaw, pitch);
            } catch (Throwable t) {
                p.setPos(originX, originY, originZ);
                p.setYRot(yaw);
                p.setXRot(pitch);
            }

            p.setOnGround(true);
        } catch (Throwable ignored) {}
    }

    public void applyCamera(Minecraft mc) {
        try {
            Camera cam = null;
            try {
                cam = mc.gameRenderer.mainCamera();
            } catch (Throwable t) {
                try {
                    cam = (Camera) mc.gameRenderer.getClass().getMethod("getMainCamera").invoke(mc.gameRenderer);
                } catch (Throwable ignored) {}
            }
            if (cam == null) return;

            // setPosition is protected in 26.2 — use reflection
            try {
                var m = Camera.class.getDeclaredMethod("setPosition", double.class, double.class, double.class);
                m.setAccessible(true);
                m.invoke(cam, camX, camY, camZ);
            } catch (Throwable t) {
                try {
                    for (var f : Camera.class.getDeclaredFields()) {
                        if (f.getType() == Vec3.class) {
                            f.setAccessible(true);
                            f.set(cam, new Vec3(camX, camY, camZ));
                            break;
                        }
                    }
                } catch (Throwable ignored) {}
            }

            try {
                var m = Camera.class.getDeclaredMethod("setRotation", float.class, float.class);
                m.setAccessible(true);
                m.invoke(cam, camYaw, camPitch);
            } catch (Throwable t) {
                try {
                    var m = Camera.class.getDeclaredMethod("setRotation", float.class, float.class, float.class);
                    m.setAccessible(true);
                    m.invoke(cam, camYaw, camPitch, 0f);
                } catch (Throwable ignored) {}
            }
        } catch (Throwable ignored) {}
    }

    public void onCameraAngles(net.neoforged.neoforge.client.event.ViewportEvent.ComputeCameraAngles event) {
        if (!active()) return;
        try {
            event.setYaw(camYaw);
            event.setPitch(camPitch);
        } catch (Throwable ignored) {}
        Minecraft mc = mc();
        if (mc != null) applyCamera(mc);
    }
}
