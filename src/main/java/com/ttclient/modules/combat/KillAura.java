package com.ttclient.modules.combat;

import com.ttclient.client.TTClientClient;
import com.ttclient.modules.Category;
import com.ttclient.modules.Module;
import com.ttclient.settings.BoolSetting;
import com.ttclient.settings.NumberSetting;
import com.ttclient.util.InvUtils;
import com.ttclient.util.SilentRotate;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

public class KillAura extends Module {
    public final NumberSetting range = addSetting(new NumberSetting("Range", "Attack range", 4.2, 2.0, 6.0, 0.1));
    public final BoolSetting players = addSetting(new BoolSetting("Players", "Target players", true));
    public final BoolSetting mobs = addSetting(new BoolSetting("Mobs", "Target hostiles", false));
    public final BoolSetting rotate = addSetting(new BoolSetting("Rotate", "Aim at target", true));
    public final BoolSetting silent = addSetting(new BoolSetting("Silent", "Body rotate only - keep your POV", true));
    public final BoolSetting weaponSwap = addSetting(new BoolSetting("WeaponSwap", "Switch to best weapon for the hit", true));
    public final BoolSetting swapBack = addSetting(new BoolSetting("SwapBack", "Swap back after hit for attribute swap", true));
    public final BoolSetting useRage = addSetting(new BoolSetting("UseRageRange", "Use Rage reach when on", true));

    public final BoolSetting spam = addSetting(new BoolSetting("Spam", "Ignore weapon cooldown and attack as fast as Delay allows", false));
    public final NumberSetting delay = addSetting(new NumberSetting("Delay", "Ticks between hits (0 with Spam off = weapon cooldown only)", 0, 0, 40, 1));

    private int tickWait;

    public KillAura() {
        super("KillAura", "Attack nearest target on cooldown", Category.COMBAT);
        addCombatSafetyToggles();
    }

    @Override
    public void onEnable() {
        tickWait = 0;
    }

    private double effectiveRange() {
        double r = range.get();
        if (useRage.get() && TTClientClient.modules != null) {
            Rage rage = TTClientClient.modules.getModule(Rage.class);
            if (rage != null && rage.isEnabled()) r = Math.max(r, rage.reach.get());
        }
        return r;
    }

    private boolean canAttack(LocalPlayer p) {
        if (tickWait > 0) {
            tickWait--;
            return false;
        }
        if (spam.get()) {
            return true;
        }
        return p.getAttackStrengthScale(0.5f) >= 0.92f;
    }

    private void afterAttack() {
        int d = delay.getInt();
        if (spam.get()) {
            tickWait = Math.max(0, d);
        } else if (d > 0) {
            tickWait = d;
        }
    }

    @Override
    public void onTick() {
        Minecraft mc = mc();
        if (mc == null || mc.player == null || mc.level == null || mc.gameMode == null) return;
        if (InvUtils.hasScreen(mc)) return;

        LocalPlayer p = mc.player;
        if (!canAttack(p)) return;

        LivingEntity target = findTargetFiltered(mc, p, effectiveRange());
        if (target == null) return;

        int prevSlot = p.getInventory().getSelectedSlot();
        boolean didSwap = false;

        if (weaponSwap.get()) {
            int w = InvUtils.findBestWeapon(p);
            if (w >= 0 && w != prevSlot) {
                InvUtils.selectHotbar(p, w);
                didSwap = true;
            }
        }

        if (rotate.get()) {
            if (silent.get()) SilentRotate.lookSilent(p, target);
            else lookAt(p, target);
        } else {
            SilentRotate.markTarget(target);
        }

        mc.gameMode.attack(p, target);
        p.swing(InteractionHand.MAIN_HAND);
        afterAttack();

        if (didSwap && swapBack.get()) {
            InvUtils.selectHotbar(p, prevSlot);
        }
    }

    public static void lookAt(LocalPlayer p, Entity target) {
        float[] a = SilentRotate.anglesTo(p, target);
        p.setYRot(a[0]);
        p.setXRot(a[1]);
        p.yHeadRot = a[0];
        p.yBodyRot = a[0];
        SilentRotate.markTarget(target);
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
