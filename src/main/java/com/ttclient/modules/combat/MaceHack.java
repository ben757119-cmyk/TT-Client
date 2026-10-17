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

/**
 * MaceHack — every mace hit is a smash, even on the ground.
 * Hearts: how hard each click feels (spoofed fall + multi-hit).
 */
public class MaceHack extends Module {
    public final NumberSetting hearts = addSetting(new NumberSetting(
            "Hearts", "Target damage per click (hearts)", 10.0, 1.0, 50.0, 0.5));
    public final NumberSetting fall = addSetting(new NumberSetting(
            "Fall", "Spoofed fall distance (smash scale)", 25.0, 1.0, 80.0, 0.5));
    public final BoolSetting autoFall = addSetting(new BoolSetting(
            "AutoFallFromHearts", "Scale fall from Hearts setting", true));
    public final BoolSetting always = addSetting(new BoolSetting(
            "AlwaysSpoof", "Keep fall spoofed while holding mace", true));
    public final BoolSetting onClick = addSetting(new BoolSetting(
            "OnClick", "Smash when you left-click with mace", true));
    public final BoolSetting multiHit = addSetting(new BoolSetting(
            "MultiHit", "Extra hits to reach Hearts", true));
    public final BoolSetting onlyPlayers = addSetting(new BoolSetting(
            "OnlyPlayers", "Only players", true));
    public final NumberSetting range = addSetting(new NumberSetting(
            "Range", "Target range", 6.0, 2.0, 64.0, 0.1));
    public final BoolSetting useRage = addSetting(new BoolSetting(
            "UseRage", "Use Rage reach when on", true));
    public final BoolSetting aura = addSetting(new BoolSetting(
            "Aura", "Auto-attack nearest while holding mace", false));
    public final NumberSetting cooldown = addSetting(new NumberSetting(
            "Cooldown", "Ticks between aura cycles", 2, 0, 30, 1));

    private int timer;

    public MaceHack() {
        super("MaceHack", "Mace smash without falling — set hearts per hit", Category.COMBAT);
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
            if (mods == null) return 25f;
            MaceHack h = mods.getModule(MaceHack.class);
            if (h == null || !h.isEnabled()) return 25f;
            return h.fallValue();
        } catch (Throwable t) {
            return 25f;
        }
    }

    public static void applySpoof(LocalPlayer p, float fallDist) {
        if (p == null) return;
        try { p.fallDistance = fallDist; } catch (Throwable ignored) {}
        try { p.setOnGround(false); } catch (Throwable ignored) {}
        try {
            var v = p.getDeltaMovement();
            p.setDeltaMovement(v.x, Math.min(v.y, -0.5), v.z);
        } catch (Throwable ignored) {}
    }

    private float fallValue() {
        if (autoFall.get()) {
            double h = hearts.get();
            return (float) Math.min(80.0, Math.max(fall.get(), h * 2.5));
        }
        return (float) fall.get();
    }

    private int hitCount() {
        if (!multiHit.get()) return 1;
        int n = (int) Math.ceil(hearts.get());
        return Math.max(1, Math.min(n, 25));
    }

    @Override
    public void onTick() {
        Minecraft mc = mc();
        if (mc == null || mc.player == null || mc.level == null || mc.gameMode == null) return;
        LocalPlayer p = mc.player;

        if (!InvUtils.isMace(p.getMainHandItem())) return;

        float f = fallValue();
        if (always.get()) applySpoof(p, f);

        boolean clicking = false;
        try { clicking = mc.options.keyAttack.isDown(); } catch (Throwable ignored) {}

        if (onClick.get() && clicking) {
            LivingEntity target = resolveTarget(mc, p);
            if (target != null) {
                smash(mc, p, target, f);
                return;
            }
        }

        if (aura.get()) {
            if (timer > 0) { timer--; return; }
            LivingEntity target = resolveTarget(mc, p);
            if (target != null) {
                smash(mc, p, target, f);
                timer = cooldown.getInt();
            }
        }
    }

    private void smash(Minecraft mc, LocalPlayer p, LivingEntity target, float fallDist) {
        lookAt(p, target);
        int n = hitCount();
        for (int i = 0; i < n; i++) {
            applySpoof(p, fallDist);
            try {
                mc.gameMode.attack(p, target);
                p.swing(InteractionHand.MAIN_HAND);
            } catch (Throwable ignored) {}
        }
        applySpoof(p, fallDist);
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
            if (to.lengthSqr() < 1.0e-6) continue;
            if (look.dot(to.normalize()) < 0.35) continue;
            double d = p.distanceToSqr(living);
            if (d <= bestD) { bestD = d; best = living; }
        }
        return best;
    }

    private double effectiveRange() {
        double r = range.get();
        if (useRage.get() && TTClientClient.modules != null) {
            Rage rage = TTClientClient.modules.getModule(Rage.class);
            if (rage != null && rage.isEnabled()) {
                try { r = Math.max(r, Math.min(rage.reach.get(), 64.0)); }
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
            if (hr instanceof EntityHitResult ehr) {
                Entity e = ehr.getEntity();
                if (e instanceof LivingEntity living && living.isAlive()) {
                    if (onlyPlayers.get() && !(living instanceof Player)) return null;
                    return living;
                }
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
