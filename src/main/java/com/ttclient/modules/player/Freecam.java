package com.ttclient.modules.player;

import com.ttclient.modules.Category;
import com.ttclient.modules.Module;
import com.ttclient.settings.BoolSetting;
import com.ttclient.settings.NumberSetting;
import com.ttclient.util.ChunkMeshReload;
import net.minecraft.client.Camera;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

/** Meteor-style freecam — camera via mixins; forceCamera uses reflection (no CameraAccessor required). */
public class Freecam extends Module {
    public final NumberSetting speed = addSetting(new NumberSetting("Speed", "Camera fly speed", 1.0, 0.1, 10.0, 0.1));
    public final NumberSetting speed2 = addSetting(new NumberSetting("Speed2", "Sprint multiplier", 2.0, 1.0, 5.0, 0.1));
    public final BoolSetting wallVision = addSetting(new BoolSetting(
            "WallVision", "See through terrain in loaded chunks", true));
    public final BoolSetting fullbright = addSetting(new BoolSetting(
            "Fullbright", "Brighten while wall vision on", true));

    private static volatile boolean activeCache;
    public static volatile boolean wallVisionCache;

    private double posX, posY, posZ;
    private double prevX, prevY, prevZ;
    public float yaw, pitch;
    public float lastYaw, lastPitch;

    private double originX, originY, originZ;
    private float originYaw, originPitch;
    private CameraType savedPerspective;
    private boolean saved;
    private Double oldGamma;

    private boolean forward, backward, left, right, up, down;

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

    public double getX(float td) { return Mth.lerp(td, prevX, posX); }
    public double getY(float td) { return Mth.lerp(td, prevY, posY); }
    public double getZ(float td) { return Mth.lerp(td, prevZ, posZ); }
    public float getYaw(float td) { return Mth.lerp(td, lastYaw, yaw); }
    public float getPitch(float td) { return Mth.lerp(td, lastPitch, pitch); }

    public void changeLookDirection(double deltaX, double deltaY) {
        lastYaw = yaw;
        lastPitch = pitch;
        yaw += (float) deltaX;
        pitch += (float) deltaY;
        pitch = Mth.clamp(pitch, -90f, 90f);
    }

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
            for (var m : Camera.class.getDeclaredMethods()) {
                if (m.getParameterCount() == 3
                        && m.getParameterTypes()[0] == double.class) {
                    String n = m.getName().toLowerCase();
                    if (n.contains("pos")) {
                        m.setAccessible(true);
                        m.invoke(cam, x, y, z);
                    }
                }
                if (m.getParameterCount() == 2
                        && m.getParameterTypes()[0] == float.class
                        && m.getParameterTypes()[1] == float.class) {
                    String n = m.getName().toLowerCase();
                    if (n.contains("rot")) {
                        m.setAccessible(true);
                        m.invoke(cam, yw, pi);
                    }
                }
            }
        } catch (Throwable ignored) {}
    }

    private static boolean isKeepVisible(String n) {
        return n.contains("ore") || n.contains("raw_iron") || n.contains("raw_gold") || n.contains("raw_copper")
                || n.contains("chest") || n.contains("barrel") || n.contains("shulker") || n.contains("hopper")
                || n.contains("spawner") || n.contains("trial_spawner") || n.contains("vault")
                || n.contains("portal") || n.contains("bedrock") || n.contains("obsidian")
                || n.contains("glass") || n.contains("leave") || n.contains("log") || n.contains("stem")
                || n.contains("plank") || n.contains("wool") || n.contains("concrete")
                || n.contains("torch") || n.contains("lantern") || n.contains("glow")
                || n.contains("ladder") || n.contains("rail") || n.contains("sign")
                || n.contains("door") || n.contains("trapdoor") || n.contains("fence")
                || n.contains("slab") || n.contains("stair") || n.contains("water") || n.contains("lava")
                || n.contains("kelp") || n.contains("amethyst") || n.contains("sculk_sensor")
                || n.contains("sculk_shrieker") || n.contains("sculk_catalyst") || n.contains("flower")
                || n.contains("crop") || n.contains("wheat") || n.contains("vine");
    }

    public static boolean shouldHideForWallVision(BlockState st) {
        if (st == null || st.isAir()) return false;
        try {
            if (!st.getFluidState().isEmpty()) return false;
        } catch (Throwable ignored) {}
        try {
            String n = st.getBlock().toString().toLowerCase();
            if (isKeepVisible(n)) return false;
            return n.contains("stone") || n.contains("deepslate") || n.contains("dirt")
                    || n.contains("grass") || n.contains("gravel") || n.contains("sand")
                    || n.contains("andesite") || n.contains("diorite") || n.contains("granite")
                    || n.contains("tuff") || n.contains("calcite") || n.contains("netherrack")
                    || n.contains("basalt") || n.contains("blackstone") || n.contains("end_stone")
                    || n.contains("terracotta") || n.contains("clay") || n.contains("mud")
                    || n.contains("snow") || n.contains("ice") || n.contains("magma")
                    || n.contains("soul_sand") || n.contains("soul_soil")
                    || n.contains("cobble") || n.contains("moss") || n.contains("dripstone")
                    || n.contains("sculk") || n.contains("smooth_basalt") || n.contains("packed_mud")
                    || n.contains("nylium") || n.contains("podzol") || n.contains("mycelium")
                    || n.contains("infested") || n.contains("reinforced_deepslate");
        } catch (Throwable t) {
            return false;
        }
    }

    public static boolean isTerrainFiller(BlockState st) {
        return shouldHideForWallVision(st);
    }

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
            Vec3 cp = mc.gameRenderer.mainCamera().position();
            x = cp.x; y = cp.y; z = cp.z;
        } catch (Throwable ignored) {}

        posX = prevX = x;
        posY = prevY = y;
        posZ = prevZ = z;
        yaw = lastYaw = p.getYRot();
        pitch = lastPitch = p.getXRot();

        try {
            savedPerspective = mc.options.getCameraType();
            mc.options.setCameraType(CameraType.FIRST_PERSON);
        } catch (Throwable ignored) {}

        forward = backward = left = right = up = down = false;
        saved = true;
        activeCache = true;

        try {
            forward = mc.options.keyUp.isDown();
            backward = mc.options.keyDown.isDown();
            left = mc.options.keyLeft.isDown();
            right = mc.options.keyRight.isDown();
            up = mc.options.keyJump.isDown();
            down = mc.options.keyShift.isDown();
        } catch (Throwable ignored) {}

        if (wallVision.get()) {
            wallVisionCache = true;
            if (fullbright.get()) applyGamma(mc);
            ChunkMeshReload.request();
        }
    }

    @Override
    public void onDisable() {
        activeCache = false;
        boolean hadWall = wallVisionCache;
        wallVisionCache = false;
        restoreGamma();

        Minecraft mc = mc();
        if (mc != null) {
            try {
                if (savedPerspective != null) mc.options.setCameraType(savedPerspective);
            } catch (Throwable ignored) {}
            if (mc.player != null && saved) {
                LocalPlayer p = mc.player;
                try {
                    p.setPos(originX, originY, originZ);
                    p.setYRot(originYaw);
                    p.setXRot(originPitch);
                    p.yRotO = originYaw;
                    p.xRotO = originPitch;
                } catch (Throwable ignored) {}
                p.setDeltaMovement(Vec3.ZERO);
            }
            try {
                mc.options.keyUp.setDown(false);
                mc.options.keyDown.setDown(false);
                mc.options.keyLeft.setDown(false);
                mc.options.keyRight.setDown(false);
                mc.options.keyJump.setDown(false);
                mc.options.keyShift.setDown(false);
            } catch (Throwable ignored) {}
        }
        saved = false;
        if (hadWall) ChunkMeshReload.request();
    }

    @Override
    public void onTick() {
        if (!saved || !activeCache) return;
        Minecraft mc = mc();
        if (mc == null || mc.player == null) return;
        LocalPlayer p = mc.player;

        try {
            if (mc.options.getCameraType() != CameraType.FIRST_PERSON) {
                mc.options.setCameraType(CameraType.FIRST_PERSON);
            }
        } catch (Throwable ignored) {}

        try {
            forward = mc.options.keyUp.isDown();
            backward = mc.options.keyDown.isDown();
            left = mc.options.keyLeft.isDown();
            right = mc.options.keyRight.isDown();
            up = mc.options.keyJump.isDown();
            down = mc.options.keyShift.isDown();

            mc.options.keyUp.setDown(false);
            mc.options.keyDown.setDown(false);
            mc.options.keyLeft.setDown(false);
            mc.options.keyRight.setDown(false);
            mc.options.keyJump.setDown(false);
            mc.options.keyShift.setDown(false);
        } catch (Throwable ignored) {}

        prevX = posX; prevY = posY; prevZ = posZ;
        lastYaw = yaw; lastPitch = pitch;

        double s = speed.get().doubleValue();
        try {
            if (mc.options.keySprint.isDown()) s *= speed2.get().doubleValue();
        } catch (Throwable ignored) {}

        Vec3 forwardVec = Vec3.directionFromRotation(0, yaw);
        Vec3 rightVec = Vec3.directionFromRotation(0, yaw + 90);

        double velX = 0, velY = 0, velZ = 0;
        if (forward) { velX += forwardVec.x * s; velZ += forwardVec.z * s; }
        if (backward) { velX -= forwardVec.x * s; velZ -= forwardVec.z * s; }
        if (right) { velX += rightVec.x * s; velZ += rightVec.z * s; }
        if (left) { velX -= rightVec.x * s; velZ -= rightVec.z * s; }
        if (up) velY += s;
        if (down) velY -= s;

        posX += velX; posY += velY; posZ += velZ;

        try {
            p.setDeltaMovement(Vec3.ZERO);
            p.setPos(originX, originY, originZ);
            p.setYRot(originYaw);
            p.setXRot(originPitch);
            try {
                Object in = p.input;
                if (in != null) {
                    for (var f : in.getClass().getDeclaredFields()) {
                        if (f.getType() == float.class || f.getType() == boolean.class) {
                            f.setAccessible(true);
                            if (f.getType() == float.class) f.setFloat(in, 0f);
                            else f.setBoolean(in, false);
                        }
                    }
                }
            } catch (Throwable ignored) {}
        } catch (Throwable ignored) {}

        boolean want = wallVision.get() && activeCache;
        if (want != wallVisionCache) {
            wallVisionCache = want;
            if (want && fullbright.get()) applyGamma(mc);
            else if (!want) restoreGamma();
            ChunkMeshReload.request();
        } else if (want && fullbright.get()) {
            applyGamma(mc);
        }
    }

    private void applyGamma(Minecraft mc) {
        try {
            Object gammaOpt = mc.options.getClass().getMethod("gamma").invoke(mc.options);
            if (gammaOpt != null) {
                if (oldGamma == null) {
                    Object v = gammaOpt.getClass().getMethod("get").invoke(gammaOpt);
                    if (v instanceof Number n) oldGamma = n.doubleValue();
                }
                try { gammaOpt.getClass().getMethod("set", double.class).invoke(gammaOpt, 16.0); } catch (Throwable ignored) {}
            }
        } catch (Throwable ignored) {}
    }

    private void restoreGamma() {
        if (oldGamma == null) return;
        Minecraft mc = mc();
        if (mc == null) { oldGamma = null; return; }
        try {
            Object gammaOpt = mc.options.getClass().getMethod("gamma").invoke(mc.options);
            gammaOpt.getClass().getMethod("set", double.class).invoke(gammaOpt, oldGamma.doubleValue());
        } catch (Throwable ignored) {}
        oldGamma = null;
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
