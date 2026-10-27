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

/** Detached freecam. Body frozen; camera driven by mixins + CameraAccessor. */
public class Freecam extends Module {
    public final NumberSetting speed = addSetting(new NumberSetting("Speed", "Blocks per tick", 2.0, 0.1, 20.0, 0.1));
    public final NumberSetting speed2 = addSetting(new NumberSetting("Speed2", "Sprint multiplier", 3.0, 1.0, 10.0, 0.1));
    public final BoolSetting wallVision = addSetting(new BoolSetting("WallVision", "Hide terrain fillers so caves/structures show", true));
    public final BoolSetting fullbright = addSetting(new BoolSetting("Fullbright", "Force high gamma while wall vision on", true));
    public final BoolSetting pitchMove = addSetting(new BoolSetting("PitchMove", "WASD follows look pitch (fly where you look)", true));

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

    public static void forceCamera(Camera cam) {
        if (cam == null || !activeCache) return;
        Freecam fc = get();
        if (fc == null || !fc.saved) return;
        float pt = partialTick();
        applyCamera(cam, fc.getX(pt), fc.getY(pt), fc.getZ(pt), fc.getYaw(pt), fc.getPitch(pt));
    }

    public static float partialTick() {
        try {
            return Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false);
        } catch (Throwable t) {
            return 1f;
        }
    }

    public static void applyCamera(Camera cam, double x, double y, double z, float yaw, float pitch) {
        try {
            CameraAccessor acc = (CameraAccessor) (Object) cam;
            acc.ttclient$setPos(x, y, z);
            try { acc.ttclient$setRot(yaw, pitch); } catch (Throwable ignored) {}
        } catch (Throwable ignored) {}
    }

    public static boolean shouldHideForWallVision(BlockState st) {
        if (st == null || st.isAir()) return false;
        try { if (!st.getFluidState().isEmpty()) return false; } catch (Throwable ignored) {}
        try {
            String n = st.getBlock().toString().toLowerCase();
            if (n.contains("ore") || n.contains("chest") || n.contains("spawner") || n.contains("portal")) return false;
            return n.contains("stone") || n.contains("deepslate") || n.contains("dirt")
                    || n.contains("grass") || n.contains("gravel") || n.contains("sand")
                    || n.contains("andesite") || n.contains("diorite") || n.contains("granite")
                    || n.contains("tuff") || n.contains("netherrack") || n.contains("basalt");
        } catch (Throwable t) { return false; }
    }

    public static boolean isTerrainFiller(BlockState st) { return shouldHideForWallVision(st); }

    /** True if mapping is held OR the default GLFW key is held. Never call setDown(false). */
    private static boolean keyHeld(Object mapping, int defaultGlfw) {
        try {
            if (mapping != null) {
                Object down = mapping.getClass().getMethod("isDown").invoke(mapping);
                if (down instanceof Boolean b && b) return true;
            }
        } catch (Throwable ignored) {}
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc == null || mc.getWindow() == null) return false;
            long window = mc.getWindow().handle();
            // Prefer the bound key code when available
            if (mapping != null) {
                try {
                    Object key = mapping.getClass().getMethod("getKey").invoke(mapping);
                    if (key != null) {
                        int value = ((Number) key.getClass().getMethod("getValue").invoke(key)).intValue();
                        Object type = null;
                        try { type = key.getClass().getMethod("getType").invoke(key); } catch (Throwable ignored) {}
                        String typeName = type != null ? type.toString().toLowerCase() : "";
                        if (typeName.contains("mouse")) {
                            if (GLFW.glfwGetMouseButton(window, value) == GLFW.GLFW_PRESS) return true;
                        } else if (value >= 0 && value < 512) {
                            if (GLFW.glfwGetKey(window, value) == GLFW.GLFW_PRESS) return true;
                        }
                    }
                } catch (Throwable ignored) {}
            }
            if (defaultGlfw >= 0 && GLFW.glfwGetKey(window, defaultGlfw) == GLFW.GLFW_PRESS) return true;
        } catch (Throwable ignored) {}
        return false;
    }

    private void pollKeys(Minecraft mc) {
        // Defaults match vanilla WASD / space / shift / ctrl — OR'd with KeyMapping.isDown
        forward  = keyHeld(mc.options.keyUp,     GLFW.GLFW_KEY_W);
        backward = keyHeld(mc.options.keyDown,   GLFW.GLFW_KEY_S);
        left     = keyHeld(mc.options.keyLeft,   GLFW.GLFW_KEY_A);
        right    = keyHeld(mc.options.keyRight,  GLFW.GLFW_KEY_D);
        up       = keyHeld(mc.options.keyJump,   GLFW.GLFW_KEY_SPACE);
        down     = keyHeld(mc.options.keyShift,  GLFW.GLFW_KEY_LEFT_SHIFT)
                || keyHeld(null, GLFW.GLFW_KEY_RIGHT_SHIFT);
    }

    @Override
    public void onEnable() {
        Minecraft mc = mc();
        if (mc == null || mc.player == null) { setEnabled(false); return; }
        LocalPlayer p = mc.player;
        originX = p.getX(); originY = p.getY(); originZ = p.getZ();
        originYaw = p.getYRot(); originPitch = p.getXRot();
        double x = p.getX(), y = p.getY() + p.getEyeHeight(), z = p.getZ();
        try { Vec3 cp = mc.gameRenderer.mainCamera().position(); x = cp.x; y = cp.y; z = cp.z; } catch (Throwable ignored) {}
        posX = prevX = x; posY = prevY = y; posZ = prevZ = z;
        yaw = lastYaw = p.getYRot(); pitch = lastPitch = p.getXRot();
        try {
            savedPerspective = mc.options.getCameraType();
            mc.options.setCameraType(CameraType.FIRST_PERSON);
        } catch (Throwable ignored) {}
        forward = backward = left = right = up = down = false;
        saved = true; activeCache = true;
        try { pollKeys(mc); } catch (Throwable ignored) {}
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
            try { if (savedPerspective != null) mc.options.setCameraType(savedPerspective); } catch (Throwable ignored) {}
            if (mc.player != null && saved) {
                LocalPlayer p = mc.player;
                try {
                    p.setPos(originX, originY, originZ);
                    p.setYRot(originYaw); p.setXRot(originPitch);
                    p.yRotO = originYaw; p.xRotO = originPitch;
                } catch (Throwable ignored) {}
                p.setDeltaMovement(Vec3.ZERO);
            }
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
            if (mc.options.getCameraType() != CameraType.FIRST_PERSON)
                mc.options.setCameraType(CameraType.FIRST_PERSON);
        } catch (Throwable ignored) {}

        try { pollKeys(mc); } catch (Throwable ignored) {}

        prevX = posX; prevY = posY; prevZ = posZ;
        lastYaw = yaw; lastPitch = pitch;

        double s = speed.get().doubleValue();
        try {
            if (keyHeld(mc.options.keySprint, GLFW.GLFW_KEY_LEFT_CONTROL)
                    || keyHeld(null, GLFW.GLFW_KEY_RIGHT_CONTROL))
                s *= speed2.get().doubleValue();
        } catch (Throwable ignored) {}

        float lookPitch = pitchMove.get() ? pitch : 0f;
        Vec3 look = Vec3.directionFromRotation(lookPitch, yaw);
        Vec3 rightVec = Vec3.directionFromRotation(0, yaw + 90f);
        Vec3 forwardFlat = Vec3.directionFromRotation(0, yaw);
        double velX = 0, velY = 0, velZ = 0;

        if (forward) {
            if (pitchMove.get()) { velX += look.x * s; velY += look.y * s; velZ += look.z * s; }
            else { velX += forwardFlat.x * s; velZ += forwardFlat.z * s; }
        }
        if (backward) {
            if (pitchMove.get()) { velX -= look.x * s; velY -= look.y * s; velZ -= look.z * s; }
            else { velX -= forwardFlat.x * s; velZ -= forwardFlat.z * s; }
        }
        if (right) { velX += rightVec.x * s; velZ += rightVec.z * s; }
        if (left)  { velX -= rightVec.x * s; velZ -= rightVec.z * s; }
        if (up) velY += s;
        if (down) velY -= s;

        double len = Math.sqrt(velX * velX + velY * velY + velZ * velZ);
        if (len > s && len > 1e-6) {
            double scale = s / len;
            velX *= scale; velY *= scale; velZ *= scale;
        }
        posX += velX; posY += velY; posZ += velZ;

        // Freeze body at origin — do NOT KeyMapping.setDown(false)
        try {
            p.setDeltaMovement(Vec3.ZERO);
            p.setPos(originX, originY, originZ);
            p.setYRot(originYaw); p.setXRot(originPitch);
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
        try { event.setYaw(getYaw(pt)); event.setPitch(getPitch(pt)); } catch (Throwable ignored) {}
    }
}
