package com.ttclient.render;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;

/**
 * World → GUI-scale screen projection using Minecraft camera axes.
 * Yaw 0 = south (+Z); pitch positive looks down.
 */
public final class EspProjection {
    private EspProjection() {}

    /**
     * @param out length-2: [guiX, guiY]
     * @return false if behind camera or projection failed
     */
    public static boolean worldToScreen(double x, double y, double z, double[] out) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.getWindow() == null || mc.gameRenderer == null) return false;

        Camera cam = getCamera(mc);
        if (cam == null) return false;

        Vec3 cpos = cam.position();
        double dx = x - cpos.x;
        double dy = y - cpos.y;
        double dz = z - cpos.z;

        // Prefer camera basis vectors when available (most accurate)
        double[] basis = tryCameraBasis(cam);
        double rightX, rightY, rightZ, upX, upY, upZ, fwdX, fwdY, fwdZ;

        if (basis != null) {
            rightX = basis[0]; rightY = basis[1]; rightZ = basis[2];
            upX = basis[3]; upY = basis[4]; upZ = basis[5];
            fwdX = basis[6]; fwdY = basis[7]; fwdZ = basis[8];
        } else {
            // Manual basis from yaw/pitch (Minecraft convention)
            float yawDeg = cam.yRot();
            float pitchDeg = cam.xRot();
            double yaw = Math.toRadians(yawDeg);
            double pitch = Math.toRadians(pitchDeg);

            double cosYaw = Math.cos(yaw);
            double sinYaw = Math.sin(yaw);
            double cosPitch = Math.cos(pitch);
            double sinPitch = Math.sin(pitch);

            // Forward (look vector)
            fwdX = -sinYaw * cosPitch;
            fwdY = -sinPitch;
            fwdZ = cosYaw * cosPitch;

            // Right (horizontal)
            rightX = cosYaw;
            rightY = 0.0;
            rightZ = sinYaw;

            // Up = right × forward
            upX = rightY * fwdZ - rightZ * fwdY;
            upY = rightZ * fwdX - rightX * fwdZ;
            upZ = rightX * fwdY - rightY * fwdX;

            // Normalize up (right and forward already unit-ish)
            double ul = Math.sqrt(upX * upX + upY * upY + upZ * upZ);
            if (ul > 1e-6) {
                upX /= ul; upY /= ul; upZ /= ul;
            }
        }

        double depth = dx * fwdX + dy * fwdY + dz * fwdZ;
        if (depth <= 0.08) return false;

        double px = dx * rightX + dy * rightY + dz * rightZ;
        double py = dx * upX + dy * upY + dz * upZ;

        int sw = mc.getWindow().getGuiScaledWidth();
        int sh = mc.getWindow().getGuiScaledHeight();

        double fov = getFovDegrees(mc);
        // Vertical FOV projection into GUI pixels
        double halfH = sh * 0.5;
        double scale = halfH / Math.tan(Math.toRadians(fov * 0.5));

        out[0] = sw * 0.5 + (px / depth) * scale;
        out[1] = sh * 0.5 - (py / depth) * scale;

        // Cull extreme off-screen points (keep some margin for boxes)
        if (out[0] < -sw || out[0] > sw * 2 || out[1] < -sh || out[1] > sh * 2) return false;
        return true;
    }

    private static Camera getCamera(Minecraft mc) {
        try {
            return mc.gameRenderer.mainCamera();
        } catch (Throwable t) {
            try {
                return (Camera) mc.gameRenderer.getClass().getMethod("getMainCamera").invoke(mc.gameRenderer);
            } catch (Throwable t2) {
                return null;
            }
        }
    }

    /** @return {rightXYZ, upXYZ, forwardXYZ} or null */
    private static double[] tryCameraBasis(Camera cam) {
        try {
            Object left = null;
            Object up = null;
            Object fwd = null;
            // 26.x naming variants
            for (String n : new String[]{"leftVector", "getLeftVector", "left"}) {
                try { left = cam.getClass().getMethod(n).invoke(cam); if (left != null) break; } catch (Throwable ignored) {}
            }
            for (String n : new String[]{"upVector", "getUpVector", "up"}) {
                try { up = cam.getClass().getMethod(n).invoke(cam); if (up != null) break; } catch (Throwable ignored) {}
            }
            for (String n : new String[]{"forwardVector", "getForwardVector", "getLookVector", "forwards", "forward"}) {
                try { fwd = cam.getClass().getMethod(n).invoke(cam); if (fwd != null) break; } catch (Throwable ignored) {}
            }
            if (left == null || up == null || fwd == null) return null;

            // left vector points left; screen +X is right = -left
            double lx = getComp(left, 0), ly = getComp(left, 1), lz = getComp(left, 2);
            double ux = getComp(up, 0), uy = getComp(up, 1), uz = getComp(up, 2);
            double fx = getComp(fwd, 0), fy = getComp(fwd, 1), fz = getComp(fwd, 2);

            return new double[]{
                    -lx, -ly, -lz, // right
                    ux, uy, uz,
                    fx, fy, fz
            };
        } catch (Throwable t) {
            return null;
        }
    }

    private static double getComp(Object vec, int i) {
        try {
            if (vec instanceof org.joml.Vector3fc v) {
                return i == 0 ? v.x() : i == 1 ? v.y() : v.z();
            }
        } catch (Throwable ignored) {}
        try {
            String[] names = i == 0 ? new String[]{"x", "x()"} : i == 1 ? new String[]{"y", "y()"} : new String[]{"z", "z()"};
            for (String n : names) {
                try {
                    if (n.endsWith("()")) {
                        Object o = vec.getClass().getMethod(n.substring(0, n.length() - 2)).invoke(vec);
                        if (o instanceof Number num) return num.doubleValue();
                    } else {
                        var f = vec.getClass().getField(n);
                        return f.getDouble(vec);
                    }
                } catch (Throwable ignored) {}
            }
        } catch (Throwable ignored) {}
        return 0;
    }

    private static double getFovDegrees(Minecraft mc) {
        double fov = 70.0;
        try {
            if (mc.options != null) {
                Object o = null;
                try { o = mc.options.getClass().getMethod("fov").invoke(mc.options); } catch (Throwable ignored) {}
                if (o == null) {
                    try { o = mc.options.getClass().getMethod("getFov").invoke(mc.options); } catch (Throwable ignored) {}
                }
                // OptionInstance-like
                if (o != null && !(o instanceof Number)) {
                    try {
                        Object v = o.getClass().getMethod("get").invoke(o);
                        if (v instanceof Number n) return n.doubleValue();
                    } catch (Throwable ignored) {}
                }
                if (o instanceof Number n) return n.doubleValue();
            }
        } catch (Throwable ignored) {}
        return fov;
    }
}
