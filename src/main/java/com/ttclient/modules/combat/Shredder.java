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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Shredder — sword shred + mace attribute swap.
 * Every cycle: max-strength sword hits, then mace smash, swap back.
 * Uses Rage reach when Rage is on.
 */
public class Shredder extends Module {
    public final NumberSetting range = addSetting(new NumberSetting(
            "Range", "Attack range (Rage overrides when on)", 6.0, 3.0, 64.0, 0.1));
    public final BoolSetting useRage = addSetting(new BoolSetting(
            "UseRage", "Use Rage reach when Rage is enabled", true));
    public final NumberSetting hits = addSetting(new NumberSetting(
            "Hits", "Sword hits per cycle", 3, 1, 10, 1));
    public final NumberSetting maceHits = addSetting(new NumberSetting(
            "MaceHits", "Mace smash hits after sword", 1, 0, 5, 1));
    public final BoolSetting swapMace = addSetting(new BoolSetting(
            "SwapMace", "Attribute-swap to mace each cycle", true));
    public final BoolSetting onlyPlayers = addSetting(new BoolSetting(
            "OnlyPlayers", "Only target players", true));
    public final BoolSetting requireClick = addSetting(new BoolSetting(
            "RequireClick", "Only while holding attack", true));
    public final BoolSetting forceCrit = addSetting(new BoolSetting(
            "ForceCrit", "Fake fall for crit / smash", true));
    public final NumberSetting cooldown = addSetting(new NumberSetting(
            "Cooldown", "Ticks between cycles", 0, 0, 20, 1));

    private int timer;
    private int prevSlot = -1;

    public Shredder() {
        super("Shredder", "Sword shred + mace swap — max dmg every hit", Category.COMBAT);
    }

    @Override
    public void onDisable() {
        restore();
        timer = 0;
    }

    @Override
    public void onTick() {
        Minecraft mc = mc();
        if (mc == null || mc.player == null || mc.level == null || mc.gameMode == null) return;
        LocalPlayer p = mc.player;

        if (timer > 0) { timer--; return; }

        boolean clicking = false;
        try { clicking = mc.options.keyAttack.isDown(); } catch (Throwable ignored) {}
        if (requireClick.get() && !clicking) return;

        LivingEntity target = resolveTarget(mc, p);
        if (target == null) return;

        int swordSlot = findSword(p);
        int maceSlot = InvUtils.findHotbarMace(p);
        if (swordSlot < 0 && maceSlot < 0) return;

        if (prevSlot < 0) {
            try { prevSlot = p.getInventory().getSelectedSlot(); }
            catch (Throwable t) { prevSlot = 0; }
        }

        lookAt(p, target);

        if (swordSlot >= 0) {
            InvUtils.selectHotbar(p, swordSlot);
            if (forceCrit.get()) forceCritMotion(p);
            int n = Math.max(1, hits.getInt());
            for (int i = 0; i < n; i++) {
                try {
                    try { p.attackStrengthTicker = 1000; } catch (Throwable ignored) {}
                    mc.gameMode.attack(p, target);
                    p.swing(InteractionHand.MAIN_HAND);
                } catch (Throwable ignored) {}
            }
        }

        if (swapMace.get() && maceSlot >= 0 && maceHits.getInt() > 0) {
            InvUtils.selectHotbar(p, maceSlot);
            if (forceCrit.get()) {
                try {
                    p.setDeltaMovement(p.getDeltaMovement().x, -2.8, p.getDeltaMovement().z);
                    p.fallDistance = 12.0f;
                } catch (Throwable ignored) {}
            }
            for (int i = 0; i < maceHits.getInt(); i++) {
                try {
                    try { p.attackStrengthTicker = 1000; } catch (Throwable ignored) {}
                    mc.gameMode.attack(p, target);
                    p.swing(InteractionHand.MAIN_HAND);
                } catch (Throwable ignored) {}
            }
            if (swordSlot >= 0) InvUtils.selectHotbar(p, swordSlot);
        }

        timer = cooldown.getInt();
    }

    private LivingEntity resolveTarget(Minecraft mc, LocalPlayer p) {
        LivingEntity cross = crosshair(mc);
        if (cross != null) return cross;

        double r = effectiveRange();
        LivingEntity best = null;
        double bestD = r * r;
        AABB box = p.getBoundingBox().inflate(r);
        for (Entity e : mc.level.getEntities(p, box)) {
            if (!(e instanceof LivingEntity living) || !living.isAlive() || living == p) continue;
            if (onlyPlayers.get() && !(living instanceof Player)) continue;
            if (!inView(p, living, 50.0)) continue;
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

    private static boolean inView(LocalPlayer p, LivingEntity t, double maxDeg) {
        try {
            Vec3 look = p.getLookAngle().normalize();
            Vec3 to = t.position().add(0, t.getBbHeight() * 0.5, 0).subtract(p.getEyePosition()).normalize();
            double dot = look.dot(to);
            double ang = Math.toDegrees(Math.acos(Math.max(-1, Math.min(1, dot))));
            return ang <= maxDeg;
        } catch (Throwable t2) {
            return true;
        }
    }

    private static void forceCritMotion(LocalPlayer p) {
        try {
            if (p.onGround()) {
                p.setDeltaMovement(p.getDeltaMovement().x, 0.32, p.getDeltaMovement().z);
            }
            p.fallDistance = Math.max(p.fallDistance, 0.5f);
        } catch (Throwable ignored) {}
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

    private void restore() {
        if (prevSlot < 0) return;
        Minecraft mc = mc();
        if (mc != null && mc.player != null) InvUtils.selectHotbar(mc.player, prevSlot);
        prevSlot = -1;
    }

    private static int findSword(LocalPlayer p) {
        int best = -1;
        int score = -1;
        for (int i = 0; i < 9; i++) {
            ItemStack s = p.getInventory().getItem(i);
            if (s.isEmpty()) continue;
            String n = s.getItem().toString().toLowerCase();
            if (!n.contains("sword")) continue;
            int sc = n.contains("netherite") ? 4 : n.contains("diamond") ? 3 : n.contains("iron") ? 2 : 1;
            if (sc > score) { score = sc; best = i; }
        }
        return best;
    }
}
