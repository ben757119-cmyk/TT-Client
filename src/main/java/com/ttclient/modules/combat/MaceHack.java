package com.ttclient.modules.combat;

import com.ttclient.client.TTClientClient;
import com.ttclient.modules.Category;
import com.ttclient.modules.Module;
import com.ttclient.settings.BoolSetting;
import com.ttclient.settings.NumberSetting;
import com.ttclient.util.InvUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class MaceHack extends Module {
    public final NumberSetting hearts = addSetting(new NumberSetting(
            "Hearts", "Smash hits per burst", 8.0, 1.0, 30.0, 1.0));
    public final NumberSetting fall = addSetting(new NumberSetting(
            "Fall", "Spoofed fall distance", 40.0, 5.0, 100.0, 1.0));
    public final BoolSetting always = addSetting(new BoolSetting(
            "AlwaysSpoof", "Spoof fall every tick while holding mace", true));
    public final BoolSetting onlyPlayers = addSetting(new BoolSetting(
            "OnlyPlayers", "Only players", true));
    public final NumberSetting range = addSetting(new NumberSetting(
            "Range", "Target range", 6.0, 2.0, 64.0, 0.1));
    public final BoolSetting useRage = addSetting(new BoolSetting(
            "UseRage", "Use Rage reach when on", true));
    public final BoolSetting auto = addSetting(new BoolSetting(
            "Auto", "Keep smashing nearest target", true));

    private int remaining;
    private int targetId = -1;
    private boolean wasClick;

    public MaceHack() {
        super("MaceHack", "Mace smash on the ground — paced multi-hit", Category.COMBAT);
    }

    public static boolean isActive() {
        try {
            var mods = TTClientClient.modules;
            if (mods == null) return false;
            MaceHack h = mods.getModule(MaceHack.class);
            return h != null && h.isEnabled();
        } catch (Throwable t) {
            return false;
        }
    }

    public static float spoofAmount() {
        try {
            var mods = TTClientClient.modules;
            if (mods == null) return 40f;
            MaceHack h = mods.getModule(MaceHack.class);
            if (h == null || !h.isEnabled()) return 40f;
            return (float) h.fall.get().doubleValue();
        } catch (Throwable t) {
            return 40f;
        }
    }

    public static void applySpoof(LocalPlayer p, float fallDist) {
        if (p == null) return;
        setFall(p, fallDist);
        try { p.setOnGround(false); } catch (Throwable ignored) {}
        try {
            var v = p.getDeltaMovement();
            p.setDeltaMovement(v.x, -1.2, v.z);
        } catch (Throwable ignored) {}
    }

    private static void setFall(LocalPlayer p, float fallDist) {
        try {
            var m = LivingEntity.class.getMethod("setFallDistance", float.class);
            m.invoke(p, fallDist);
            return;
        } catch (Throwable ignored) {}
        try {
            for (Class<?> c = p.getClass(); c != null && c != Object.class; c = c.getSuperclass()) {
                for (var f : c.getDeclaredFields()) {
                    if (f.getType() != float.class) continue;
                    if (f.getName().toLowerCase().contains("fall")) {
                        f.setAccessible(true);
                        f.setFloat(p, fallDist);
                        return;
                    }
                }
            }
        } catch (Throwable ignored) {}
    }

    @Override
    public void onDisable() {
        remaining = 0;
        targetId = -1;
        wasClick = false;
    }

    @Override
    public void onTick() {
        Minecraft mc = mc();
        if (mc == null || mc.player == null || mc.level == null || mc.gameMode == null) return;
        LocalPlayer p = mc.player;

        if (!InvUtils.isMace(p.getMainHandItem())) {
            int slot = InvUtils.findHotbarMace(p);
            if (slot >= 0 && (auto.get() || remaining > 0)) {
                InvUtils.selectHotbar(p, slot);
            } else {
                remaining = 0;
                return;
            }
        }

        float f = (float) fall.get().doubleValue();
        if (always.get() || remaining > 0) applySpoof(p, f);

        boolean click = false;
        try { click = mc.options.keyAttack.isDown(); } catch (Throwable ignored) {}
        boolean edge = click && !wasClick;
        wasClick = click;

        if (edge || (auto.get() && remaining <= 0)) {
            LivingEntity t = resolveTarget(mc, p);
            if (t != null) {
                targetId = t.getId();
                remaining = Math.max(1, (int) Math.round(hearts.get().doubleValue()));
            }
        }

        if (remaining <= 0) return;

        LivingEntity target = findById(mc, targetId);
        if (target == null || !target.isAlive()) {
            target = resolveTarget(mc, p);
            if (target == null) { remaining = 0; return; }
            targetId = target.getId();
        }

        applySpoof(p, f);
        lookAt(p, target);
        try { mc.gameMode.attack(p, target); } catch (Throwable ignored) {}
        try { p.swing(InteractionHand.MAIN_HAND); } catch (Throwable ignored) {}
        applySpoof(p, f);
        remaining--;
    }

    private LivingEntity findById(Minecraft mc, int id) {
        if (id < 0) return null;
        try {
            Entity e = mc.level.getEntity(id);
            if (e instanceof LivingEntity living && living.isAlive()) return living;
        } catch (Throwable ignored) {}
        return null;
    }

    private LivingEntity resolveTarget(Minecraft mc, LocalPlayer p) {
        LivingEntity cross = crosshair(mc);
        if (cross != null) return cross;
        double r = effectiveRange();
        LivingEntity best = null;
        double bestD = r * r;
        AABB box = p.getBoundingBox().inflate(r);
        Vec3 look = p.getLookAngle();
        Vec3 eye = p.getEyePosition();
        for (Entity e : mc.level.getEntities(p, box)) {
            if (!(e instanceof LivingEntity living) || !living.isAlive() || living == p) continue;
            if (onlyPlayers.get() && !(living instanceof Player)) continue;
            Vec3 to = living.position().add(0, living.getBbHeight() * 0.5, 0).subtract(eye);
            if (to.lengthSqr() < 1e-6) continue;
            if (look.dot(to.normalize()) < 0.25) continue;
            double d = p.distanceToSqr(living);
            if (d <= bestD) { bestD = d; best = living; }
        }
        return best;
    }

    private double effectiveRange() {
        double r = range.get().doubleValue();
        if (useRage.get() && TTClientClient.modules != null) {
            Rage rage = TTClientClient.modules.getModule(Rage.class);
            if (rage != null && rage.isEnabled()) {
                try { r = Math.max(r, Math.min(rage.reach.get().doubleValue(), 64.0)); }
                catch (Throwable ignored) {}
            }
        }
        return r;
    }

    private LivingEntity crosshair(Minecraft mc) {
        try {
            Entity e = mc.crosshairPickEntity;
            if (e instanceof LivingEntity living && living.isAlive()) {
                if (onlyPlayers.get() && !(living instanceof Player)) return null;
                return living;
            }
        } catch (Throwable ignored) {}
        try {
            HitResult hr = mc.hitResult;
            if (hr instanceof EntityHitResult ehr && ehr.getEntity() instanceof LivingEntity living && living.isAlive()) {
                if (onlyPlayers.get() && !(living instanceof Player)) return null;
                return living;
            }
        } catch (Throwable ignored) {}
        return null;
    }

    private static void lookAt(LocalPlayer p, LivingEntity target) {
        try {
            Vec3 eye = p.getEyePosition();
            Vec3 aim = target.position().add(0, target.getBbHeight() * 0.5, 0);
            Vec3 d = aim.subtract(eye);
            double horiz = Math.sqrt(d.x * d.x + d.z * d.z);
            float yaw = (float) (Math.toDegrees(Math.atan2(d.z, d.x)) - 90.0);
            float pitch = (float) (-Math.toDegrees(Math.atan2(d.y, horiz)));
            p.setYRot(yaw);
            p.setXRot(pitch);
            p.yRotO = yaw;
            p.xRotO = pitch;
        } catch (Throwable ignored) {}
    }
}
