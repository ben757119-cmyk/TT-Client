package com.ttclient.modules.player;

import com.ttclient.mixin.CameraAccessor;
import com.ttclient.modules.Category;
import com.ttclient.modules.Module;
import com.ttclient.settings.NumberSetting;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

public class Freecam extends Module {
    public final NumberSetting speed = addSetting(new NumberSetting("Speed", "Fly speed", 1.0, 0.1, 10.0, 0.1));
    public final NumberSetting speed2 = addSetting(new NumberSetting("Speed2", "Sprint mult", 2.0, 1.0, 5.0, 0.1));

    private static volatile boolean activeCache;

    private double posX, posY, posZ;
    private double prevX, prevY, prevZ;
    private float yaw, pitch;
    private float prevYaw, prevPitch;

    private double originX, originY, originZ;
    private float originYaw, originPitch;
    private boolean saved;

    public Freecam() {
        super("Freecam", "Meteor-style detached camera (F4)", Category.PLAYER);
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

    public static boolean isActive() { return activeCache; }

    public double getX(float tickDelta) { return Mth.lerp(tickDelta, prevX, posX); }
    public double getY(float tickDelta) { return Mth.lerp(tickDelta, prevY, posY); }
    public double getZ(float tickDelta) { return Mth.lerp(tickDelta, prevZ, posZ); }
    public float getYaw(float tickDelta) { return Mth.lerp(tickDelta, prevYaw, yaw); }
    public float getPitch(float tickDelta) { return Mth.lerp(tickDelta, prevPitch, pitch); }

    public double getCamX() { return posX; }
    public double getCamY() { return posY; }
    public double getCamZ() { return posZ; }
    public float getCamYaw() { return yaw; }
    public float getCamPitch() { return pitch; }

    public static void forceCamera(Camera cam) {
        if (cam == null || !activeCache) return;
        Freecam fc = get();
        if (fc == null || !fc.saved) return;
        float pt = 1f;
        try {
            pt = Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false);
        } catch (Throwable ignored) {}
        double x = fc.getX(pt), y = fc.getY(pt), z = fc.getZ(pt);
        float yw = fc.getYaw(pt), pi = fc.getPitch(pt);
        try {
            CameraAccessor a = (CameraAccessor) (Object) cam;
            a.ttclient$setPos(x, y, z);
            a.ttclient$setRot(yw, pi);
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

        double x = p.getX();
        double y = p.getY() + p.getEyeHeight();
        double z = p.getZ();
        try {
            Camera c = mc.gameRenderer.mainCamera();
            Vec3 cp = c.position();
            x = cp.x; y = cp.y; z = cp.z;
        } catch (Throwable ignored) {}

        posX = prevX = x;
        posY = prevY = y;
        posZ = prevZ = z;
        yaw = prevYaw = p.getYRot();
        pitch = prevPitch = p.getXRot();
        saved = true;
        activeCache = true;
        p.setDeltaMovement(Vec3.ZERO);
    }

    @Override
    public void onDisable() {
        activeCache = false;
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
    }

    @Override
    public void onTick() {
        if (!saved || !activeCache) return;
        Minecraft mc = mc();
        if (mc == null || mc.player == null) return;
        LocalPlayer p = mc.player;

        prevYaw = yaw;
        prevPitch = pitch;
        yaw = p.getYRot();
        pitch = p.getXRot();

        prevX = posX;
        prevY = posY;
        prevZ = posZ;

        double s = speed.get().doubleValue();
        try {
            if (mc.options.keySprint.isDown()) s *= speed2.get().doubleValue();
        } catch (Throwable ignored) {}

        double radYaw = Math.toRadians(yaw);
        double radPitch = Math.toRadians(pitch);
        double cosPitch = Math.cos(radPitch);

        double forward = 0, strafe = 0, vertical = 0;
        try {
            if (mc.options.keyUp.isDown()) forward += 1;
            if (mc.options.keyDown.isDown()) forward -= 1;
            if (mc.options.keyLeft.isDown()) strafe += 1;
            if (mc.options.keyRight.isDown()) strafe -= 1;
            if (mc.options.keyJump.isDown()) vertical += 1;
            if (mc.options.keyShift.isDown()) vertical -= 1;
        } catch (Throwable ignored) {}

        double len = Math.sqrt(forward * forward + strafe * strafe);
        if (len > 1.0e-6) { forward /= len; strafe /= len; }

        double fx = -Math.sin(radYaw) * cosPitch;
        double fy = -Math.sin(radPitch);
        double fz = Math.cos(radYaw) * cosPitch;
        double rx = Math.cos(radYaw);
        double rz = Math.sin(radYaw);

        posX += (fx * forward + rx * strafe) * s;
        posY += (fy * forward + vertical) * s;
        posZ += (fz * forward + rz * strafe) * s;

        try {
            p.setDeltaMovement(Vec3.ZERO);
            try { p.snapTo(originX, originY, originZ, originYaw, originPitch); }
            catch (Throwable t) {
                p.setPos(originX, originY, originZ);
                p.setYRot(originYaw); p.setXRot(originPitch);
            }
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
            p.setYRot(yaw);
            p.setXRot(pitch);
        } catch (Throwable ignored) {}
    }

    public void onCameraAngles(net.neoforged.neoforge.client.event.ViewportEvent.ComputeCameraAngles event) {
        if (!activeCache) return;
        float pt = 1f;
        try {
            pt = Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false);
        } catch (Throwable ignored) {}
        try {
            event.setYaw(getYaw(pt));
            event.setPitch(getPitch(pt));
        } catch (Throwable ignored) {}
    }
}
