package com.ttclient.modules.combat;

import com.ttclient.modules.Category;
import com.ttclient.modules.Module;
import com.ttclient.settings.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class AimAssist extends Module {
    public final NumberSetting range = addSetting(new NumberSetting("Range", "Target range", 4.5, 2.0, 6.0, 0.1));
    public final NumberSetting speed = addSetting(new NumberSetting("Speed", "Aim speed", 0.35, 0.05, 1.0, 0.05));

    public AimAssist() {
        super("AimAssist", "Soft aim toward nearest target", Category.COMBAT);
    }

    @Override
    public void onTick() {
        Minecraft mc = mc();
        if (mc == null || mc.player == null || mc.level == null) return;
        LocalPlayer p = mc.player;
        double r = range.get();
        LivingEntity best = null;
        double bestDist = r * r;
        for (Entity e : mc.level.getEntities(p, p.getBoundingBox().inflate(r))) {
            if (!(e instanceof LivingEntity living) || !living.isAlive() || living == p) continue;
            if (!(living instanceof Player) && !(living instanceof Monster)) continue;
            double d = p.distanceToSqr(living);
            if (d < bestDist) {
                bestDist = d;
                best = living;
            }
        }
        if (best == null) return;
        Vec3 eye = p.getEyePosition();
        Vec3 target = best.getEyePosition().subtract(eye).normalize();
        float yaw = (float) (Math.toDegrees(Math.atan2(-target.x, target.z)));
        float pitch = (float) (Math.toDegrees(-Math.asin(target.y)));
        float s = speed.getFloat();
        p.setYRot(lerpAngle(p.getYRot(), yaw, s));
        p.setXRot(lerp(p.getXRot(), pitch, s));
    }

    private static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    private static float lerpAngle(float a, float b, float t) {
        float diff = b - a;
        while (diff < -180) diff += 360;
        while (diff > 180) diff -= 360;
        return a + diff * t;
    }
}
