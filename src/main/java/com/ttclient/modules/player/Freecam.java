package com.ttclient.modules.player;

import com.ttclient.mixin.CameraAccessor;
import com.ttclient.modules.Category;
import com.ttclient.modules.Module;
import com.ttclient.settings.NumberSetting;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

public class Freecam extends Module {
    public final NumberSetting speed = addSetting(new NumberSetting("Speed", "Fly speed", 3.0, 0.2, 30.0, 0.1));
    public final NumberSetting vSpeed = addSetting(new NumberSetting("VSpeed", "Vertical mult", 1.0, 0.2, 3.0, 0.1));

    private double originX, originY, originZ;
    private float originYaw, originPitch;
    private boolean saved;
    private static volatile boolean activeCache = false;
    private double camX, camY, camZ;
    private float camYaw, camPitch;

    public Freecam() {
        super("Freecam", "Detach camera from body (F4)", Category.PLAYER);
        setKeyBind(GLFW.GLFW_KEY_F4);
    }

    public static Freecam get() {
        try {
            var mods = com.ttclient.client.TTClientClient.modules;
            return mods == null ? null : mods.getModule(Freecam.class);
        } catch (Throwable t) {
            return null;
        }
    }

    public boolean active() { return activeCache; }
    public static boolean isActive() { return activeCache; }
    public double getCamX() { return camX; }
    public double getCamY() { return camY; }
    public double getCamZ() { return camZ; }
    public float getCamYaw() { return camYaw; }
    public float getCamPitch() { return camPitch; }

    public static void forceCamera(Camera cam) {
        if (cam == null || !activeCache) return;
        Freecam fc = get();
        if (fc == null || !fc.saved) return;
        try {
            CameraAccessor acc = (CameraAccessor) (Object) cam;
            acc.ttclient$invokeSetPosition(fc.camX, fc.camY, fc.camZ);
            acc.ttclient$invokeSetRotation(fc.camYaw, fc.camPitch);
            return;
        } catch (Throwable ignored) {}
        try {
            for (var f : Camera.class.getDeclaredFields()) {
                if (f.getType() == Vec3.class) {
                    f.setAccessible(true);
                    f.set(cam, new Vec3(fc.camX, fc.camY, fc.camZ));
                } else if (f.getType() == float.class) {
                    String n = f.getName().toLowerCase();
                    f.setAccessible(true);
                    if (n.contains("yrot") || n.equals("yaw")) f.setFloat(cam, fc.camYaw);
                    else if (n.contains("xrot") || n.equals("pitch")) f.setFloat(cam, fc.camPitch);
                }
            }
        } catch (Throwable ignored) {}
    }

    public static void applyTo(Camera cam) { forceCamera(cam); }

    @Override
    public void onEnable() {
        Minecraft mc = mc();
        if (mc == null || mc.player == null) { setEnabled(false); return; }
        LocalPlayer p = mc.player;
        originX = p.getX(); originY = p.getY(); originZ = p.getZ();
        originYaw = p.getYRot(); originPitch = p.getXRot();
        camX = p.getX();
        camY = p.getY() + p.getEyeHeight();
        camZ = p.getZ();
        camYaw = p.getYRot();
        camPitch = p.getXRot();
        saved = true;
        activeCache = true;
        p.setDeltaMovement(Vec3.ZERO);
    }

    @Override
    public void onDisable() {
        Minecraft mc = mc();
        if (mc != null && mc.player != null && saved) {
            LocalPlayer p = mc.player;
            try { p.snapTo(originX, originY, originZ, originYaw, originPitch); }
            catch (Throwable t) {
                p.setPos(originX, originY, originZ);
                p.setYRot(originYaw); p.setXRot(originPitch);
            }
            p.setDeltaMovement(Vec3.ZERO);
        }
        saved = false;
        activeCache = false;
    }

    @Override
    public void onTick() {
        Minecraft mc = mc();
        if (mc == null || mc.player == null || !saved) return;
        LocalPlayer p = mc.player;
        camYaw = p.getYRot();
        camPitch = p.getXRot();
        moveCamera(mc);
        try {
            p.setDeltaMovement(Vec3.ZERO);
            p.fallDistance = 0;
            try { p.snapTo(originX, originY, originZ, camYaw, camPitch); }
            catch (Throwable t) { p.setPos(originX, originY, originZ); }
            try {
                Object in = p.input;
                if (in != null) {
                    for (var f : in.getClass().getDeclaredFields()) {
                        if (f.getType() == float.class) {
                            f.setAccessible(true);
                            f.setFloat(in, 0f);
                        }
                    }
                }
            } catch (Throwable ignored) {}
        } catch (Throwable ignored) {}
        try {
            Camera cam = null;
            try { cam = mc.gameRenderer.mainCamera(); } catch (Throwable ignored) {}
            if (cam != null) forceCamera(cam);
        } catch (Throwable ignored) {}
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
        camX += dx; camY += dy; camZ += dz;
    }

    public void onCameraAngles(net.neoforged.neoforge.client.event.ViewportEvent.ComputeCameraAngles event) {
        if (!active()) return;
        try {
            event.setYaw(camYaw);
            event.setPitch(camPitch);
        } catch (Throwable ignored) {}
    }
}
