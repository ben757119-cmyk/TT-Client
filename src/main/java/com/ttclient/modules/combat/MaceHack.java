package com.ttclient.modules.combat;

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

/**
 * Spoofs fall distance so mace hits can deal smash-style damage without falling.
 *
 * Fully effective in singleplayer / weak servers that trust client fallDistance.
 * Strict anti-cheats compute smash on the server and will ignore this.
 */
public class MaceHack extends Module {
    public final NumberSetting spoofFall = addSetting(new NumberSetting(
            "Fall", "Spoofed fall distance for smash", 22.0, 1.0, 50.0, 0.5));
    public final BoolSetting always = addSetting(new BoolSetting(
            "Always", "Keep fall spoofed while holding mace", true));
    public final BoolSetting onHit = addSetting(new BoolSetting(
            "OnHit", "Spoof right before you attack", true));
    public final BoolSetting autoSwing = addSetting(new BoolSetting(
            "Aura", "Also attack nearest player with spoofed smash", false));
    public final NumberSetting range = addSetting(new NumberSetting(
            "Range", "Aura range", 4.5, 2.0, 6.0, 0.1));
    public final NumberSetting cooldown = addSetting(new NumberSetting(
            "Cooldown", "Aura ticks between hits", 12, 0, 30, 1));

    private int timer;

    public MaceHack() {
        super("MaceHack", "Mace smash damage without falling", Category.COMBAT);
    }

    /** Apply spoofed fall distance for smash calculation. */
    public static void applySpoof(LocalPlayer p, float fall) {
        if (p == null) return;
        try {
            p.fallDistance = fall;
        } catch (Throwable ignored) {}
        try {
            p.setOnGround(false);
        } catch (Throwable ignored) {}
        // Nudge velocity downward so some validators see a falling state
        try {
            var v = p.getDeltaMovement();
            if (v.y > -0.08) {
                p.setDeltaMovement(v.x, Math.min(v.y, -0.1), v.z);
            }
        } catch (Throwable ignored) {}
    }

    public static boolean isActive() {
        try {
            var mods = com.ttclient.client.TTClientClient.modules;
            if (mods == null) return false;
            MaceHack h = mods.getModule(MaceHack.class);
            return h != null && h.isEnabled();
        } catch (Throwable t) {
            return false;
        }
    }

    public static float spoofAmount() {
        try {
            var mods = com.ttclient.client.TTClientClient.modules;
            if (mods == null) return 22f;
            MaceHack h = mods.getModule(MaceHack.class);
            if (h == null) return 22f;
            return (float) h.spoofFall.get();
        } catch (Throwable t) {
            return 22f;
        }
    }

    @Override
    public void onTick() {
        Minecraft mc = mc();
        if (mc == null || mc.player == null || mc.gameMode == null) return;
        LocalPlayer p = mc.player;

        boolean holdingMace = InvUtils.isMace(p.getMainHandItem());
        if (!holdingMace) {
            int m = InvUtils.findHotbarMace(p);
            if (m >= 0 && autoSwing.get()) {
                InvUtils.selectHotbar(p, m);
                holdingMace = true;
            }
        }

        if (!holdingMace) return;

        if (always.get()) {
            applySpoof(p, (float) spoofFall.get());
        }

        // Spoof when pressing attack (melee click)
        if (onHit.get() && mc.options != null && mc.options.keyAttack.isDown()) {
            applySpoof(p, (float) spoofFall.get());
        }

        // Optional built-in aura
        if (!autoSwing.get()) return;
        if (timer > 0) { timer--; return; }

        LivingEntity target = findTarget(mc, p);
        if (target == null) {
            // Crosshair entity
            try {
                HitResult hr = mc.hitResult;
                if (hr instanceof EntityHitResult ehr && ehr.getEntity() instanceof LivingEntity le && le.isAlive()) {
                    target = le;
                }
            } catch (Throwable ignored) {}
        }
        if (target == null) return;

        applySpoof(p, (float) spoofFall.get());
        try {
            mc.gameMode.attack(p, target);
            p.swing(InteractionHand.MAIN_HAND);
        } catch (Throwable ignored) {}
        // Keep spoof a moment after hit
        applySpoof(p, (float) spoofFall.get());
        timer = cooldown.getInt();
    }

    private LivingEntity findTarget(Minecraft mc, LocalPlayer p) {
        double r = range.get();
        LivingEntity best = null;
        double bestD = r * r;
        AABB box = p.getBoundingBox().inflate(r);
        for (Entity e : mc.level.getEntities(p, box)) {
            if (!(e instanceof Player pl) || pl == p || !pl.isAlive()) continue;
            double d = p.distanceToSqr(pl);
            if (d < bestD) {
                bestD = d;
                best = pl;
            }
        }
        return best;
    }
}
