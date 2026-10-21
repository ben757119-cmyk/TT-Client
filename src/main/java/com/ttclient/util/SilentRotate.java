package com.ttclient.util;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * Silent rotations: body/head turn for 3rd-person + server packets,
 * without moving the client's camera (POV stays where you look).
 */
public final class SilentRotate {
    private SilentRotate() {}

    /** Last combat target for Target HUD / auras. */
    public static volatile Entity lastTarget;
    public static volatile long lastTargetMs;

    public static void markTarget(Entity e) {
        if (e == null) return;
        lastTarget = e;
        lastTargetMs = System.currentTimeMillis();
    }

    public static Entity currentTarget(long maxAgeMs) {
        Entity e = lastTarget;
        if (e == null || !e.isAlive()) return null;
        if (System.currentTimeMillis() - lastTargetMs > maxAgeMs) return null;
        return e;
    }

    public static float[] anglesTo(LocalPlayer p, Entity target) {
        Vec3 eye = p.getEyePosition(1.0f);
        Vec3 aim = target.getBoundingBox().getCenter();
        double dx = aim.x - eye.x;
        double dy = aim.y - eye.y;
        double dz = aim.z - eye.z;
        double dist = Math.sqrt(dx * dx + dz * dz);
        float yaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
        float pitch = (float) (-Math.toDegrees(Math.atan2(dy, dist)));
        pitch = Math.max(-90f, Math.min(90f, pitch));
        return new float[]{yaw, pitch};
    }

    /**
     * Server + body only. Camera (getYRot/getXRot) unchanged so first-person POV stays still.
     * In third person the model faces the target.
     */
    public static void lookSilent(LocalPlayer p, Entity target) {
        if (p == null || target == null) return;
        float[] a = anglesTo(p, target);
        applyBody(p, a[0], a[1]);
        sendRot(p, a[0], a[1]);
        markTarget(target);
    }

    public static void lookSilent(LocalPlayer p, float yaw, float pitch) {
        if (p == null) return;
        pitch = Math.max(-90f, Math.min(90f, pitch));
        applyBody(p, yaw, pitch);
        sendRot(p, yaw, pitch);
    }

    private static void applyBody(LocalPlayer p, float yaw, float pitch) {
        try {
            p.yHeadRot = yaw;
            p.yBodyRot = yaw;
            // smooth-ish for render
            try { p.yHeadRotO = yaw; } catch (Throwable ignored) {}
            try { p.yBodyRotO = yaw; } catch (Throwable ignored) {}
        } catch (Throwable ignored) {}
    }

    private static void sendRot(LocalPlayer p, float yaw, float pitch) {
        if (p.connection == null) return;
        boolean onGround = true;
        try { onGround = p.onGround(); } catch (Throwable ignored) {}
        boolean horiz = false;
        try { horiz = p.horizontalCollision; } catch (Throwable ignored) {}

        try {
            p.connection.send(new ServerboundMovePlayerPacket.Rot(yaw, pitch, onGround, horiz));
            return;
        } catch (Throwable ignored) {}
        try {
            p.connection.send(new ServerboundMovePlayerPacket.Rot(yaw, pitch, onGround));
            return;
        } catch (Throwable ignored) {}
        try {
            Class<?> rotCl = Class.forName("net.minecraft.network.protocol.game.ServerboundMovePlayerPacket$Rot");
            for (var c : rotCl.getConstructors()) {
                Class<?>[] pt = c.getParameterTypes();
                Object pkt = null;
                try {
                    if (pt.length == 4 && pt[0] == float.class) {
                        pkt = c.newInstance(yaw, pitch, onGround, horiz);
                    } else if (pt.length == 3 && pt[0] == float.class) {
                        pkt = c.newInstance(yaw, pitch, onGround);
                    }
                } catch (Throwable ignored2) {}
                if (pkt != null) {
                    p.connection.send((net.minecraft.network.protocol.Packet<?>) pkt);
                    return;
                }
            }
        } catch (Throwable ignored) {}
    }
}
