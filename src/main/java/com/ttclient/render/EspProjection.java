package com.ttclient.render;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;

/**
 * Simple world→screen projection for ESP overlays (26.2-safe, no custom shaders).
 */
public final class EspProjection {
    private EspProjection() {}

    /**
     * @param out length-2 array: [screenX, screenY]
     * @return false if point is behind the camera
     */
    public static boolean worldToScreen(double x, double y, double z, double[] out) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.getWindow() == null || mc.gameRenderer == null) return false;

        Camera cam;
        try {
            cam = mc.gameRenderer.mainCamera();
        } catch (Throwable t) {
            try {
                cam = (Camera) mc.gameRenderer.getClass().getMethod("getMainCamera").invoke(mc.gameRenderer);
            } catch (Throwable t2) {
                return false;
            }
        }
        if (cam == null) return false;

        Vec3 cpos = cam.position();
        double dx = x - cpos.x;
        double dy = y - cpos.y;
        double dz = z - cpos.z;

        float yaw = cam.yRot();
        float pitch = cam.xRot();

        double yawRad = Math.toRadians(yaw);
        double pitchRad = Math.toRadians(pitch);

        // Rotate by -yaw around Y, then -pitch around X
        double cosYaw = Math.cos(-yawRad);
        double sinYaw = Math.sin(-yawRad);
        double x1 = dx * cosYaw + dz * sinYaw;
        double z1 = -dx * sinYaw + dz * cosYaw;

        double cosPitch = Math.cos(-pitchRad);
        double sinPitch = Math.sin(-pitchRad);
        double y1 = dy * cosPitch - z1 * sinPitch;
        double z2 = dy * sinPitch + z1 * cosPitch;

        // In Minecraft camera space, looking down -Z; points in front have z2 < 0 after this rotation convention.
        // Using distance-style projection: treat -z2 as depth.
        double depth = -z2;
        if (depth <= 0.05) return false;

        int sw = mc.getWindow().getGuiScaledWidth();
        int sh = mc.getWindow().getGuiScaledHeight();
        double fov = 70.0;
        try {
            // Approximate FOV from options if present
            if (mc.options != null) {
                Object fovObj = mc.options.getClass().getMethod("fov").invoke(mc.options);
                if (fovObj instanceof Number n) fov = n.doubleValue();
            }
        } catch (Throwable ignored) {}

        double scale = (sh / 2.0) / Math.tan(Math.toRadians(fov / 2.0));
        double sx = sw / 2.0 + (x1 / depth) * scale;
        double sy = sh / 2.0 - (y1 / depth) * scale;

        out[0] = sx;
        out[1] = sy;
        return true;
    }
}
