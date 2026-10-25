package com.ttclient.modules.combat;

import com.ttclient.client.TTClientClient;
import com.ttclient.modules.Category;
import com.ttclient.modules.Module;
import com.ttclient.settings.BoolSetting;
import com.ttclient.settings.ModeSetting;
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

/**
 * KillAura with full attack timing control and attribute-style weapon swap.
 *
 * Timing modes:
 *   Cooldown - wait for weapon attack strength (CooldownPct)
 *   Custom   - fixed Delay ticks between hits
 *   Spam     - every tick (Delay can still throttle)
 *
 * WeaponSwap + SwapBack: select best weapon, attack, restore previous slot same tick.
 */
public class KillAura extends Module {
    public final NumberSetting range = addSetting(new NumberSetting(
            "Range", "Attack range", 4.2, 2.0, 6.0, 0.1));
    public final BoolSetting players = addSetting(new BoolSetting(
            "Players", "Target players", true));
    public final BoolSetting mobs = addSetting(new BoolSetting(
            "Mobs", "Target hostiles", false));
    public final BoolSetting rotate = addSetting(new BoolSetting(
            "Rotate", "Aim at target", true));
    public final BoolSetting silent = addSetting(new BoolSetting(
            "Silent", "Body rotate only - keep your POV", true));
    public final BoolSetting weaponSwap = addSetting(new BoolSetting(
            "WeaponSwap", "Switch to best weapon for the hit", true));
    public final BoolSetting swapBack = addSetting(new BoolSetting(
            "SwapBack", "Swap back after hit (attribute swap)", true));
    public final BoolSetting useRage = addSetting(new BoolSetting(
            "UseRageRange", "Use Rage reach when on", true));

    public final ModeSetting timing = addSetting(new ModeSetting(
            "Timing",
            "Cooldown = weapon charge, Custom = Delay ticks, Spam = every tick",
            "Cooldown",
            "Cooldown", "Custom", "Spam"));

    public final NumberSetting delay = addSetting(new NumberSetting(
            "Delay",
            "Ticks between hits for Custom / optional Spam throttle (0 = fastest)",
            0, 0, 40, 1));

    public final NumberSetting cooldownPct = addSetting(new NumberSetting(
            "CooldownPct",
            "Min weapon charge 0-1 when Timing is Cooldown",
            0.92, 0.1, 1.0, 0.01));

    private int tickWait;

    public KillAura() {
        super("KillAura", "Attack nearest target with timing + attribute swap", Category.COMBAT);
    }

    @Override
    public void onEnable() {
        tickWait = 0;
    }

    private double effectiveRange() {
        double r = range.get();
        if (useRage.get() && TTClientClient.modules != null) {
            Rage rage = TTClientClient.modules.getModule(Rage.class);
            if (rage != null && rage.isEnabled()) {
                r = Math.max(r, rage.reach.get());
            }
        }
        return r;
    }

    private boolean canAttack(LocalPlayer p) {
        if (tickWait > 0) {
            tickWait--;
            return false;
        }

        String t = timing.get();
        if ("Spam".equals(t)) {
            return true;
        }
        if ("Custom".equals(t)) {
            return true; // delay applied in afterAttack
        }
        // Cooldown
        float need = (float) cooldownPct.get();
        return p.getAttackStrengthScale(0.5f) >= need;
    }

    private void afterAttack() {
        String t = timing.get();
        int d = delay.getInt();
        if ("Custom".equals(t)) {
            tickWait = Math.max(0, d);
        } else if ("Spam".equals(t) && d > 0) {
            tickWait = d;
        }
        // Cooldown mode: weapon charge gates the next hit; no tickWait needed
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

        // Attribute swap path: best weapon for this hit only
        if (weaponSwap.get()) {
            int w = InvUtils.findBestWeapon(p);
            if (w >= 0 && w != prevSlot) {
                InvUtils.selectHotbar(p, w);
                didSwap = true;
            }
        }

        if (rotate.get()) {
            if (silent.get()) {
                SilentRotate.lookSilent(p, target);
            } else {
                lookAt(p, target);
            }
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
            } else {
                continue;
            }

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
