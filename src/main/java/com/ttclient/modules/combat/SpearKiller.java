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
 * Spear Killer — every spear hit packs extreme relative velocity so charge damage is huge.
 * Vanilla spear charge scales with relative speed vs the target.
 */
public class SpearKiller extends Module {
    public final NumberSetting range = addSetting(new NumberSetting(
            "Range", "Auto-target range", 6.0, 2.0, 64.0, 0.1));
    public final BoolSetting useRage = addSetting(new BoolSetting(
            "UseRageRange", "Use Rage reach when Rage is on", true));
    public final NumberSetting velocity = addSetting(new NumberSetting(
            "Velocity", "Relative speed injected toward target (blocks/sec)", 100.0, 20.0, 250.0, 5.0));
    public final NumberSetting hits = addSetting(new NumberSetting(
            "Hits", "Attacks per cycle (more = more dmg)", 4, 1, 12, 1));
    public final NumberSetting cooldown = addSetting(new NumberSetting(
            "Cooldown", "Ticks between attack cycles", 1, 0, 15, 1));
    public final BoolSetting onlyPlayers = addSetting(new BoolSetting(
            "OnlyPlayers", "Only target players", true));
    public final BoolSetting autoSwitch = addSetting(new BoolSetting(
            "AutoSwitch", "Switch to spear in hotbar", true));
    public final BoolSetting switchBack = addSetting(new BoolSetting(
            "SwitchBack", "Restore slot when off / no target", true));
    public final BoolSetting autoAttack = addSetting(new BoolSetting(
            "AutoAttack", "Auto-attack targets in range", true));
    public final BoolSetting onManualHit = addSetting(new BoolSetting(
            "BoostManual", "Also boost when you click-attack with a spear", true));
    public final BoolSetting holdCharge = addSetting(new BoolSetting(
            "HoldCharge", "Keep spear charged between hits", true));

    private int timer;
    private int prevSlot = -1;
    private boolean keyForced;

    public SpearKiller() {
        super("SpearKiller", "Spear hits deal massive charge damage", Category.COMBAT);
    }

    @Override
    public void onDisable() {
        releaseUse();
        restoreSlot();
        timer = 0;
        prevSlot = -1;
    }

    @Override
    public void onTick() {
        Minecraft mc = mc();
        if (mc == null || mc.player == null || mc.level == null || mc.gameMode == null) {
            releaseUse();
            return;
        }
        LocalPlayer p = mc.player;

        if (timer > 0) timer--;

        boolean manual = false;
        try {
            manual = onManualHit.get() && mc.options.keyAttack.isDown();
        } catch (Throwable ignored) {}

        LivingEntity target = findTarget(mc, p);

        if (manual) {
            LivingEntity cross = crosshairLiving(mc);
            if (cross != null) target = cross;
        }

        if (target == null) {
            releaseUse();
            if (switchBack.get() && !manual) restoreSlot();
            return;
        }

        if (!ensureSpear(p)) {
            releaseUse();
            return;
        }

        lookAt(p, target);
        injectVelocity(p, target);

        if (holdCharge.get()) {
            forceUse(mc, p);
        }

        boolean shouldSwing = (autoAttack.get() || manual) && timer <= 0;
        if (!shouldSwing) return;

        try {
            if (p.isUsingItem()) {
                p.releaseUsingItem();
            }
        } catch (Throwable ignored) {}

        int n = Math.max(1, hits.getInt());
        for (int i = 0; i < n; i++) {
            injectVelocity(p, target);
            try {
                mc.gameMode.attack(p, target);
                p.swing(InteractionHand.MAIN_HAND);
            } catch (Throwable ignored) {}
        }

        if (holdCharge.get()) {
            forceUse(mc, p);
        }

        timer = Math.max(0, cooldown.getInt());
    }

    private LivingEntity crosshairLiving(Minecraft mc) {
        try {
            Entity e = mc.crosshairPickEntity;
            if (e instanceof LivingEntity living && living.isAlive()) return living;
        } catch (Throwable ignored) {}
        try {
            HitResult hr = mc.hitResult;
            if (hr != null && hr.getType() == HitResult.Type.ENTITY) {
                Entity e = ((EntityHitResult) hr).getEntity();
                if (e instanceof LivingEntity living && living.isAlive()) return living;
            }
        } catch (Throwable ignored) {}
        return null;
    }

    private double effectiveRange() {
        double r = range.get();
        if (useRage.get() && TTClientClient.modules != null) {
            Rage rage = TTClientClient.modules.getModule(Rage.class);
            if (rage != null && rage.isEnabled()) {
                try {
                    r = Math.max(r, Math.min(rage.reach.get(), 64.0));
                } catch (Throwable ignored) {}
            }
        }
        return r;
    }

    private LivingEntity findTarget(Minecraft mc, LocalPlayer self) {
        double r = effectiveRange();
        LivingEntity best = null;
        double bestD = r * r;
        AABB box = self.getBoundingBox().inflate(r);
        for (Entity e : mc.level.getEntities(self, box)) {
            if (!(e instanceof LivingEntity living) || !living.isAlive() || living == self) continue;
            if (onlyPlayers.get() && !(living instanceof Player)) continue;
            double d = self.distanceToSqr(living);
            if (d <= bestD) {
                bestD = d;
                best = living;
            }
        }
        return best;
    }

    private boolean ensureSpear(LocalPlayer p) {
        if (isSpear(p.getMainHandItem())) return true;
        if (!autoSwitch.get()) return false;

        int slot = findSpearHotbar(p);
        if (slot < 0) return false;

        if (prevSlot < 0) {
            try {
                prevSlot = p.getInventory().getSelectedSlot();
            } catch (Throwable t) {
                prevSlot = 0;
            }
        }
        InvUtils.selectHotbar(p, slot);
        return isSpear(p.getMainHandItem());
    }

    private void restoreSlot() {
        Minecraft mc = mc();
        if (mc == null || mc.player == null) return;
        if (switchBack.get() && prevSlot >= 0 && prevSlot <= 8) {
            InvUtils.selectHotbar(mc.player, prevSlot);
        }
        prevSlot = -1;
    }

    private void injectVelocity(LocalPlayer p, LivingEntity target) {
        try {
            Vec3 from = p.getEyePosition();
            Vec3 to = target.position().add(0, target.getBbHeight() * 0.45, 0);
            Vec3 dir = to.subtract(from);
            double len = dir.length();
            if (len < 1.0e-4) return;
            dir = dir.scale(1.0 / len);

            double perTick = velocity.get() / 20.0;
            Vec3 boost = dir.scale(perTick);
            p.setDeltaMovement(boost.x, boost.y, boost.z);
        } catch (Throwable ignored) {}
    }

    private void lookAt(LocalPlayer p, LivingEntity target) {
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

    private void forceUse(Minecraft mc, LocalPlayer p) {
        try {
            mc.options.keyUse.setDown(true);
            keyForced = true;
            if (!p.isUsingItem()) {
                mc.gameMode.useItem(p, InteractionHand.MAIN_HAND);
            }
        } catch (Throwable ignored) {}
    }

    private void releaseUse() {
        if (!keyForced) return;
        try {
            Minecraft mc = mc();
            if (mc != null) {
                mc.options.keyUse.setDown(false);
                if (mc.player != null && mc.player.isUsingItem()) {
                    mc.player.releaseUsingItem();
                }
            }
        } catch (Throwable ignored) {}
        keyForced = false;
    }

    public static boolean isSpear(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        try {
            return stack.getItem().toString().toLowerCase().contains("spear");
        } catch (Throwable t) {
            return false;
        }
    }

    public static int findSpearHotbar(LocalPlayer p) {
        for (int i = 0; i < 9; i++) {
            if (isSpear(p.getInventory().getItem(i))) return i;
        }
        return -1;
    }
}
