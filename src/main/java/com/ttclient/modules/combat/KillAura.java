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
 * KillAura with attack timing control and optional attribute-style weapon swap.
 */
public class KillAura extends Module {
    public final NumberSetting range = addSetting(new NumberSetting("Range", "Attack range", 4.2, 2.0, 6.0, 0.1));
    public final BoolSetting players = addSetting(new BoolSetting("Players", "Target players", true));
    public final BoolSetting mobs = addSetting(new BoolSetting("Mobs", "Target hostiles", false));
    public final BoolSetting rotate = addSetting(new BoolSetting("Rotate", "Aim at target", true));
    public final BoolSetting silent = addSetting(new BoolSetting("Silent", "Body rotate only — keep your POV", true));
    public final BoolSetting weaponSwap = addSetting(new BoolSetting("WeaponSwap", "Switch to best weapon for the hit", true));
    public final BoolSetting swapBack = addSetting(new BoolSetting("SwapBack", "Swap back after hit (attribute swap)", true));
    public final BoolSetting useRage = addSetting(new BoolSetting("UseRageRange", "Use Rage reach when on", true));

    /** Cooldown = weapon attack strength · Custom = Delay ticks · Spam = every tick */
    public final ModeSetting timing = addSetting(new ModeSetting(
            "Timing",
            "When to attack: weapon cooldown, custom delay, or every tick",
            "Cooldown",
            "Cooldown", "Custom", "Spam"));

    /** Only used when Timing = Custom. 0 ≈ every tick, 10 ≈ 0.5s at 20 TPS. */
    public final NumberSetting delay = addSetting(new NumberSetting(
            "Delay",
            "Ticks between hits when Timing is Custom (0 = fastest)",
            10, 0, 40, 1));

    /** Min attack-strength scale when Timing = Cooldown (1.0 = full cooldown). */
    public final NumberSetting cooldown = addSetting(new NumberSetting(
            "CooldownPct",
            "Min weapon charge 0–1 when Timing is Cooldown",
            0.92, 0.1, 1.0, 0.01));

    private int tickWait;

    public KillAura() {
        super("KillAura", "Attack nearest target on cooldown",
                Category.COMBAT);
    }

    @Override
    public void onEnable() {
        tickWait = 0;
        super.onEnable();
    }

    private double effectiveRange() {
        double r = range.get();
        if (useRage.get() && TTClientClient.modules != null) {
            Rage rage = TTClientClient.modules.getModule(Rage.class);
            if (rage != null && rage.isEnabled()) r = Math.max(r, rage.reach.get());
        }
        return r;
    }

    /** @return true if we are allowed to attack this tick */
    private boolean canAttack(LocalPlayer p) {
        String t = timing.get();
        if ("Spam".equals(t)) {
            return true;
        }
        if ("Custom".equals(t)) {
            if (tickWait > 0) {
                tickWait--;
                return false;
            }
            return true;
        }
        // Cooldown — respect held weapon attack strength
        float need = (float) cooldown.get();
        return p.getAttackStrengthScale(0.5f) >= need;
    }

    private void afterAttack() {
        if ("Custom".equals(timing.get())) {
            tickWait = Math.max(0, delay.getInt());
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
