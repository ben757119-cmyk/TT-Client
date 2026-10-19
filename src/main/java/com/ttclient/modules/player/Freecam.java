package com.ttclient.modules.player;

import com.ttclient.mixin.CameraAccessor;
import com.ttclient.modules.Category;
import com.ttclient.modules.Module;
import com.ttclient.settings.BoolSetting;
import com.ttclient.settings.NumberSetting;
import com.ttclient.util.ChunkMeshReload;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

/**
 * Meteor-style freecam + full-depth wall vision.
 * Mesh rebuild via ChunkMeshReload (immediate + delayed) so toggle does not need block updates.
 */
public class Freecam extends Module {
    public final NumberSetting speed = addSetting(new NumberSetting("Speed", "Fly speed", 1.0, 0.1, 10.0, 0.1));
    public final NumberSetting speed2 = addSetting(new NumberSetting("Speed2", "Sprint mult", 2.0, 1.0, 5.0, 0.1));
    public final BoolSetting wallVision = addSetting(new BoolSetting(
            "WallVision", "See through terrain in loaded chunks (to bedrock)", true));
    public final BoolSetting onlyInWall = addSetting(new BoolSetting(
            "OnlyInWall", "Only when camera is inside a solid block", false));
    public final BoolSetting fullbright = addSetting(new BoolSetting(
            "Fullbright", "Brighten while wall vision is on", true));

    private static volatile boolean activeCache;
    public static volatile boolean wallVisionCache;

    private double posX, posY, posZ;
    private double prevX, prevY, prevZ;
    private float yaw, pitch;
    private float prevYaw, prevPitch;

    private double originX, originY, originZ;
    private float originYaw, originPitch;
    private boolean saved;
    private boolean lastWall;
    private Double oldGamma;

    public Freecam() {
        super("Freecam", "Detached camera + full-depth caves (F4)", Category.PLAYER);
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
    public float getYaw(float td) { return Mth.lerp(td, prevYaw, yaw); }
    public float getPitch(float td) { return Mth.lerp(td, prevPitch, pitch); }

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

    private static boolean isKeepVisible(String n) {
        return n.contains("ore") || n.contains("raw_iron") || n.contains("raw_gold") || n.contains("raw_copper")
                || n.contains("chest") || n.contains("barrel") || n.contains("shulker") || n.contains("hopper")
                || n.contains("spawner") || n.contains("trial_spawner") || n.contains("vault")
                || n.contains("portal") || n.contains("bedrock") || n.contains("obsidian")
                || n.contains("glass") || n.contains("leave") || n.contains("log") || n.contains("stem")
                || n.contains("plank") || n.contains("wool") || n.contains("concrete")
                || n.contains("torch") || n.contains("lantern") || n.contains("glow") || n.contains("sea_lantern")
                || n.contains("ladder") || n.contains("rail") || n.contains("sign") || n.contains("banner")
                || n.contains("door") || n.contains("trapdoor") || n.contains("fence")
                || n.contains("slab") || n.contains("stair") || n.contains("carpet") || n.contains("button")
                || n.contains("lever") || n.contains("piston") || n.contains("observer") || n.contains("dispenser")
                || n.contains("dropper") || n.contains("furnace") || n.contains("smoker") || n.contains("blast")
                || n.contains("crafting") || n.contains("anvil") || n.contains("enchant") || n.contains("brewing")
                || n.contains("beacon") || n.contains("conduit") || n.contains("scaffolding")
                || n.contains("water") || n.contains("lava") || n.contains("kelp") || n.contains("seagrass")
                || n.contains("crop") || n.contains("wheat") || n.contains("carrot") || n.contains("potato")
                || n.contains("amethyst") || n.contains("budding") || n.contains("cluster")
                || n.contains("sculk_sensor") || n.contains("sculk_shrieker") || n.contains("sculk_catalyst")
                || n.contains("copper_bulb") || n.contains("crafter") || n.contains("decorated_pot")
                || n.contains("flower") || n.contains("sapling") || n.contains("mushroom")
                || n.contains("vine") || n.contains("moss_carpet") || n.contains("hanging_roots");
    }

    /**
     * Name-based only — never call canOcclude() here (mixin re-enters this method).
     */
    public static boolean shouldHideForWallVision(BlockState st) {
        if (st == null || st.isAir()) return false;
        try {
            if (!st.getFluidState().isEmpty()) return false;
        } catch (Throwable ignored) {}
        try {
            String n = st.getBlock().toString().toLowerCase();
            if (isKeepVisible(n)) return false;

            // Full terrain column including deepslate / cave fillers
            return n.contains("stone") || n.contains("deepslate") || n.contains("dirt")
                    || n.contains("grass_block") || n.contains("grass") || n.contains("gravel") || n.contains("sand")
                    || n.contains("andesite") || n.contains("diorite") || n.contains("granite")
                    || n.contains("tuff") || n.contains("calcite") || n.contains("netherrack")
                    || n.contains("basalt") || n.contains("blackstone") || n.contains("end_stone")
                    || n.contains("terracotta") || n.contains("clay") || n.contains("mud")
                    || n.contains("snow") || n.contains("ice") || n.contains("packed_ice") || n.contains("blue_ice")
                    || n.contains("magma") || n.contains("soul_sand") || n.contains("soul_soil")
                    || n.contains("cobble") || n.contains("moss_block") || n.contains("dripstone")
                    || n.contains("sculk") || n.contains("smooth_basalt") || n.contains("packed_mud")
                    || n.contains("nylium") || n.contains("podzol") || n.contains("mycelium")
                    || n.contains("rooted_dirt") || n.contains("coarse_dirt") || n.contains("farmland")
                    || n.contains("infested") || n.contains("powder_snow")
                    || n.contains("nether_wart_block") || n.contains("warped_wart")
                    || n.contains("crimson_nylium") || n.contains("warped_nylium")
                    || n.contains("reinforced_deepslate");
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
        yaw = prevYaw = p.getYRot();
        pitch = prevPitch = p.getXRot();
        saved = true;
        activeCache = true;
        lastWall = false;
        p.setDeltaMovement(Vec3.ZERO);
        updateWallVision(mc, true);
    }

    @Override
    public void onDisable() {
        activeCache = false;
        boolean hadWall = wallVisionCache || lastWall;
        wallVisionCache = false;
        restoreGamma();
        lastWall = false;

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
        if (hadWall) ChunkMeshReload.request();
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
        if (len > 1e-6) { forward /= len; strafe /= len; }

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
            try {
                p.snapTo(originX, originY, originZ, originYaw, originPitch);
            } catch (Throwable t) {
                p.setPos(originX, originY, originZ);
                p.setYRot(originYaw);
                p.setXRot(originPitch);
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

        updateWallVision(mc, false);
    }

    private void updateWallVision(Minecraft mc, boolean force) {
        boolean want = false;
        if (wallVision.get() && activeCache) {
            if (!onlyInWall.get()) want = true;
            else want = isCameraInsideSolid(mc);
        }
        boolean changed = want != wallVisionCache;
        wallVisionCache = want;

        if (want && fullbright.get()) applyGamma(mc);
        else if (!want) restoreGamma();

        if (force || changed) {
            lastWall = want;
            // Immediate + delayed mesh rebuilds (fixes "need block updates")
            ChunkMeshReload.request();
        }
    }

    private boolean isCameraInsideSolid(Minecraft mc) {
        try {
            if (mc.level == null) return false;
            // Check feet of camera + a few offsets so partial blocks still count
            for (double dy = -0.2; dy <= 0.2; dy += 0.2) {
                for (double dx = -0.2; dx <= 0.2; dx += 0.2) {
                    for (double dz = -0.2; dz <= 0.2; dz += 0.2) {
                        BlockPos pos = BlockPos.containing(posX + dx, posY + dy, posZ + dz);
                        BlockState st = mc.level.getBlockState(pos);
                        if (st != null && !st.isAir() && st.getFluidState().isEmpty()) return true;
                    }
                }
            }
            return false;
        } catch (Throwable t) {
            return false;
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
