package com.ttclient.modules.player;

import com.ttclient.mixin.CameraAccessor;
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

/**
 * Detached freecam camera. Body stays frozen; camera position/rotation driven by mixins + accessor.
 */
public class Freecam extends Module {
    public final NumberSetting speed = addSetting(new NumberSetting("Speed", "Blocks per tick", 2.0, 0.1, 20.0, 0.1));
    public final NumberSetting speed2 = addSetting(new NumberSetting("Speed2", "Sprint multiplier", 3.0, 1.0, 10.0, 0.1));
    public final BoolSetting wallVision = addSetting(new BoolSetting(
            "WallVision", "Hide terrain fillers so caves/structures show", true));
    public final BoolSetting fullbright = addSetting(new BoolSetting(
            "Fullbright", "Force high gamma while wall vision on", true));
    public final BoolSetting pitchMove = addSetting(new BoolSetting(
            "PitchMove", "WASD follows look pitch (fly where you look)", true));

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
        super("Freecam", "Detached camera (F4) — body frozen, free fly", Category.PLAYER);
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

    public void changeLookDirection(double deltaYaw, double deltaPitch) {
        lastYaw = yaw;
        lastPitch = pitch;
        yaw += (float) deltaYaw;
        pitch += (float) deltaPitch;
        pitch = Mth.clamp(pitch, -90f, 90f);
    }

    /** Apply freecam pos/rot to the active Camera. Prefer invoker, fall back to reflection. */
    public static void forceCamera(Camera cam) {
        if (cam == null || !activeCache) return;
        Freecam fc = get();
        if (fc == null || !fc.saved) return;
        float pt = partialTick();
        double x = fc.getX(pt), y = fc.getY(pt), z = fc.getZ(pt);
        float yw = fc.getYaw(pt), pi = fc.getPitch(pt);
        applyCamera(cam, x, y, z, yw, pi);
    }

    static float partialTick() {
        try {
            return Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false);
        } catch (Throwable t) {
            return 1f;
        }
    }

    static void applyCamera(Camera cam, double x, double y, double z, float yaw, float pitch) {
        // 1) Accessor invokers (preferred)
        try {
            CameraAccessor acc = (CameraAccessor) (Object) cam;
            acc.ttclient$setPos(x, y, z);
            try {
                acc.ttclient$setRot(yaw, pitch);
            } catch (Throwable ignored) {
                try {
                    var m = Camera.class.getDeclaredMethod("setRotation", float.class, float.class, float.class);
                    m.setAccessible(true);
                    m.invoke(cam, yaw, pitch, 0f);
                } catch (Throwable ignored2) {}
            }
        } catch (Throwable ignored) {}

        // 2) Reflection fallback for mappings drift
        try {
            for (var m : Camera.class.getDeclaredMethods()) {
                Class<?>[] pt = m.getParameterTypes();
                String n = m.getName().toLowerCase();
                if (pt.length == 3 && pt[0] == double.class && pt[1] == double.class && pt[2] == double.class
                        && (n.contains("pos") || n.contains("position"))) {
                    m.setAccessible(true);
                    try { m.invoke(cam, x, y, z); } catch (Throwable ignored) {}
                }
                if (pt.length == 2 && pt[0] == float.class && pt[1] == float.class
                        && (n.contains("rot") || n.contains("rotation"))) {
                    m.setAccessible(true);
                    try { m.invoke(cam, yaw, pitch); } catch (Throwable ignored) {}
                }
                if (pt.length == 3 && pt[0] == float.class && pt[1] == float.class && pt[2] == float.class
                        && (n.contains("rot") || n.contains("rotation"))) {
                    m.setAccessible(true);
                    try { m.invoke(cam, yaw, pitch, 0f); } catch (Throwable ignored) {}
                }
            }
        } catch (Throwable ignored) {}

        // Mark detached so vanilla doesn't yank camera back to entity eyes
        try {
            for (var f : Camera.class.getDeclaredFields()) {
                if (f.getType() == boolean.class) {
                    String n = f.getName().toLowerCase();
                    if (n.contains("detach") || n.contains("detached")) {
                        f.setAccessible(true);
                        f.setBoolean(cam, true);
                    }
                }
            }
        } catch (Throwable ignored) {}
    }

    private static boolean isInteresting(String n) {
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
                || n.contains("crop") || n.contains("wheat") || n.contains("vine")
                || n.contains("crafting") || n.contains("furnace") || n.contains("smoker")
                || n.contains("blast") || n.contains("anvil") || n.contains("enchant")
                || n.contains("bookshelf") || n.contains("beacon") || n.contains("conduit")
                || n.contains("frame") || n.contains("piston") || n.contains("dispenser")
                || n.contains("dropper") || n.contains("observer") || n.contains("redstone")
                || n.contains("lever") || n.contains("button") || n.contains("pressure")
                || n.contains("carpet") || n.contains("bed") || n.contains("banner")
                || n.contains("skull") || n.contains("head") || n.contains("candle")
                || n.contains("campfire") || n.contains("brewing") || n.contains("cauldron");
    }

    public static boolean shouldHideForWallVision(BlockState st) {
        if (st == null || st.isAir()) return false;
        try {
            if (!st.getFluidState().isEmpty()) return false;
        } catch (Throwable ignored) {}
        try {
            String n = st.getBlock().toString().toLowerCase();
            if (isInteresting(n)) return false;
            if (n.contains("stone") || n.contains("deepslate") || n.contains("dirt")
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
                    || n.contains("infested") || n.contains("reinforced_deepslate")
                    || n.contains("rooted") || n.contains("powder_snow")
                    || n.contains("nether_wart_block") || n.contains("warped_wart")) {
                return true;
            }
            // Do NOT call st.canOcclude() here — MixinBlockStateBase hooks it and can recurse.
            return false;
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

        float lookPitch = pitchMove.get() ? pitch : 0f;
        Vec3 look = Vec3.directionFromRotation(lookPitch, yaw);
        Vec3 rightVec = Vec3.directionFromRotation(0, yaw + 90f);
        Vec3 forwardFlat = Vec3.directionFromRotation(0, yaw);

        double velX = 0, velY = 0, velZ = 0;
        if (forward) {
            if (pitchMove.get()) {
                velX += look.x * s;
                velY += look.y * s;
                velZ += look.z * s;
            } else {
                velX += forwardFlat.x * s;
                velZ += forwardFlat.z * s;
            }
        }
        if (backward) {
            if (pitchMove.get()) {
                velX -= look.x * s;
                velY -= look.y * s;
                velZ -= look.z * s;
            } else {
                velX -= forwardFlat.x * s;
                velZ -= forwardFlat.z * s;
            }
        }
        if (right) { velX += rightVec.x * s; velZ += rightVec.z * s; }
        if (left)  { velX -= rightVec.x * s; velZ -= rightVec.z * s; }
        if (up)   velY += s;
        if (down) velY -= s;

        double len = Math.sqrt(velX * velX + velY * velY + velZ * velZ);
        if (len > s && len > 1e-6) {
            double scale = s / len;
            velX *= scale; velY *= scale; velZ *= scale;
        }

        posX += velX;
        posY += velY;
        posZ += velZ;

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
        float pt = partialTick();
        try {
            event.setYaw(getYaw(pt));
            event.setPitch(getPitch(pt));
        } catch (Throwable ignored) {}
    }
}
