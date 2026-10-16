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
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class KillAura extends Module {
    public final NumberSetting range = addSetting(new NumberSetting("Range", "Attack range", 4.2, 2.0, 6.0, 0.1));
    public final BoolSetting players = addSetting(new BoolSetting("Players", "Target players", true));
    public final BoolSetting mobs = addSetting(new BoolSetting("Mobs", "Target hostiles", false));
    public final BoolSetting rotate = addSetting(new BoolSetting("Rotate", "Look at target", true));
    public final BoolSetting weaponSwap = addSetting(new BoolSetting("WeaponSwap", "Switch to best weapon", true));
    public final BoolSetting useRage = addSetting(new BoolSetting("UseRageRange", "Use Rage reach when on", true));
    public final NumberSetting wallBypass = addSetting(new NumberSetting("HurtTime", "Skip target if hurtTime above", 10, 0, 20, 1));

    public KillAura() {
        super("KillAura", "Attack nearest target on cooldown", Category.COMBAT);
    }

    private double effectiveRange() {
        double r = range.get();
        if (useRage.get() && TTClientClient.modules != null) {
            Rage rage = TTClientClient.modules.getModule(Rage.class);
            if (rage != null && rage.isEnabled()) r = Math.max(r, rage.reach.get());
        }
        return r;
    }

    @Override
    public void onTick() {
        Minecraft mc = mc();
        if (mc == null || mc.player == null || mc.level == null || mc.gameMode == null) return;
        if (mc.screen != null) return;

        LocalPlayer p = mc.player;
        if (p.getAttackStrengthScale(0.5f) < 0.92f) return;

        LivingEntity target = findTarget(mc, p, effectiveRange());
        if (target == null) return;

        if (weaponSwap.get()) {
            int w = InvUtils.findBestWeapon(p);
            if (w >= 0) InvUtils.selectHotbar(p, w);
        }

        if (rotate.get()) lookAt(p, target);

        mc.gameMode.attack(p, target);
        p.swing(InteractionHand.MAIN_HAND);
    }

    public static void lookAt(LocalPlayer p, Entity target) {
        Vec3 eye = p.getEyePosition(1.0f);
        Vec3 aim = target.getBoundingBox().getCenter();
        double dx = aim.x - eye.x;
        double dy = aim.y - eye.y;
        double dz = aim.z - eye.z;
        double dist = Math.sqrt(dx * dx + dz * dz);
        float yaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
        float pitch = (float) (-Math.toDegrees(Math.atan2(dy, dist)));
        p.setYRot(yaw);
        p.setXRot(Math.max(-90f, Math.min(90f, pitch)));
        p.yHeadRot = yaw;
        p.yBodyRot = yaw;
    }

    public static LivingEntity findTarget(Minecraft mc, LocalPlayer p, double r) {
        AABB box = p.getBoundingBox().inflate(r);
        LivingEntity best = null;
        double bestDist = r * r;

        for (Entity e : mc.level.getEntities(p, box)) {
            if (!(e instanceof LivingEntity living) || !living.isAlive()) continue;
            if (living == p) continue;
            if (living.isInvisible() && living instanceof Player) continue;
            double d = p.distanceToSqr(living);
            if (d > bestDist) continue;
            // Prefer lower health players
            double score = d;
            if (living instanceof Player) score -= 50;
            if (living.getHealth() < 10) score -= 10;
            if (best == null || score < bestDist) {
                // store score in bestDist for ranking
                bestDist = score;
                best = living;
            }
        }
        return best;
    }

    public LivingEntity findTargetFiltered(Minecraft mc, LocalPlayer p, double r) {
        AABB box = p.getBoundingBox().inflate(r);
        LivingEntity best = null;
        double bestScore = Double.MAX_VALUE;

        for (Entity e : mc.level.getEntities(p, box)) {
            if (!(e instanceof LivingEntity living) || !living.isAlive()) continue;
            if (living == p) continue;
            if (living instanceof Player) {
                if (!players.get()) continue;
            } else if (living instanceof Monster) {
                if (!mobs.get()) continue;
            } else continue;

            if (living.hurtTime > wallBypass.getInt()) continue;

            double d = p.distanceToSqr(living);
            if (d > r * r) continue;
            double score = d;
            if (living instanceof Player) score -= 100;
            score += living.getHealth();
            if (score < bestScore) {
                bestScore = score;
                best = living;
            }
        }
        return best;
    }
}
