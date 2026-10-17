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
 * When YOU hit someone (attack key on an entity) while holding a spear:
 * charged multi spear hits then optional mace smash — pop totem then kill.
 */
public class SpearKiller extends Module {
    public final NumberSetting range = addSetting(new NumberSetting(
            "Range", "Max target distance", 8.0, 2.0, 64.0, 0.1));
    public final BoolSetting useRage = addSetting(new BoolSetting(
            "UseRageRange", "Use Rage reach when on", true));
    public final NumberSetting velocity = addSetting(new NumberSetting(
            "Velocity", "Charge velocity (b/s)", 180.0, 40.0, 400.0, 10.0));
    public final NumberSetting spearHits = addSetting(new NumberSetting(
            "SpearHits", "Spear attacks per real hit", 6, 1, 16, 1));
    public final BoolSetting maceFinish = addSetting(new BoolSetting(
            "MaceFinish", "Swap to mace for smash after spear", true));
    public final NumberSetting maceHits = addSetting(new NumberSetting(
            "MaceHits", "Mace smash attacks", 2, 0, 8, 1));
    public final BoolSetting onlyPlayers = addSetting(new BoolSetting(
            "OnlyPlayers", "Only players", true));
    public final BoolSetting requireSpear = addSetting(new BoolSetting(
            "RequireSpear", "Only when holding a spear", true));
    public final BoolSetting autoAttack = addSetting(new BoolSetting(
            "AutoAttack", "Also auto-attack nearby (off = only your hits)", false));

    private int prevSlot = -1;

    public SpearKiller() {
        super("SpearKiller", "Your spear hits pop totem then kill", Category.COMBAT);
    }

    @Override
    public void onDisable() {
        restoreSlot();
        prevSlot = -1;
    }

    @Override
    public void onTick() {
        Minecraft mc = mc();
        if (mc == null || mc.player == null || mc.level == null || mc.gameMode == null) return;
        LocalPlayer p = mc.player;

        boolean attacking = false;
        try { attacking = mc.options.keyAttack.isDown(); } catch (Throwable ignored) {}

        LivingEntity target = null;
        if (attacking) target = crosshairLiving(mc);
        if (target == null && autoAttack.get()) target = findTarget(mc, p);
        if (target == null) return;
        if (!attacking && !autoAttack.get()) return;

        if (requireSpear.get() && !isSpear(p.getMainHandItem())) {
            if (!(maceFinish.get() && isMace(p.getMainHandItem()) && attacking)) return;
        }

        lookAt(p, target);

        int spearSlot = findSpearHotbar(p);
        int maceSlot = findMaceHotbar(p);
        int startSlot = -1;
        try { startSlot = p.getInventory().getSelectedSlot(); } catch (Throwable ignored) {}

        if (spearSlot >= 0) {
            if (prevSlot < 0 && startSlot >= 0) prevSlot = startSlot;
            InvUtils.selectHotbar(p, spearSlot);
            injectVelocity(p, target);
            try {
                if (!p.isUsingItem()) mc.gameMode.useItem(p, InteractionHand.MAIN_HAND);
            } catch (Throwable ignored) {}
            try {
                if (p.isUsingItem()) p.releaseUsingItem();
            } catch (Throwable ignored) {}
            int n = Math.max(1, spearHits.getInt());
            for (int i = 0; i < n; i++) {
                injectVelocity(p, target);
                try {
                    mc.gameMode.attack(p, target);
                    p.swing(InteractionHand.MAIN_HAND);
                } catch (Throwable ignored) {}
            }
        }

        if (maceFinish.get() && maceSlot >= 0 && maceHits.getInt() > 0) {
            InvUtils.selectHotbar(p, maceSlot);
            try {
                p.setDeltaMovement(0, -2.5, 0);
                p.fallDistance = 10.0f;
            } catch (Throwable ignored) {}
            for (int i = 0; i < maceHits.getInt(); i++) {
                try {
                    mc.gameMode.attack(p, target);
                    p.swing(InteractionHand.MAIN_HAND);
                } catch (Throwable ignored) {}
            }
            if (spearSlot >= 0) InvUtils.selectHotbar(p, spearSlot);
            else if (startSlot >= 0) InvUtils.selectHotbar(p, startSlot);
        }
    }

    private LivingEntity crosshairLiving(Minecraft mc) {
        try {
            Entity e = mc.crosshairPickEntity;
            if (e instanceof LivingEntity living && living.isAlive()) {
                if (onlyPlayers.get() && !(living instanceof Player)) return null;
                return living;
            }
        } catch (Throwable ignored) {}
        try {
            HitResult hr = mc.hitResult;
            if (hr != null && hr.getType() == HitResult.Type.ENTITY) {
                Entity e = ((EntityHitResult) hr).getEntity();
                if (e instanceof LivingEntity living && living.isAlive()) {
                    if (onlyPlayers.get() && !(living instanceof Player)) return null;
                    return living;
                }
            }
        } catch (Throwable ignored) {}
        return null;
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

    private LivingEntity findTarget(Minecraft mc, LocalPlayer self) {
        double r = effectiveRange();
        LivingEntity best = null;
        double bestD = r * r;
        AABB box = self.getBoundingBox().inflate(r);
        for (Entity e : mc.level.getEntities(self, box)) {
            if (!(e instanceof LivingEntity living) || !living.isAlive() || living == self) continue;
            if (onlyPlayers.get() && !(living instanceof Player)) continue;
            double d = self.distanceToSqr(living);
            if (d <= bestD) { bestD = d; best = living; }
        }
        return best;
    }

    private void restoreSlot() {
        Minecraft mc = mc();
        if (mc == null || mc.player == null) return;
        if (prevSlot >= 0 && prevSlot <= 8) InvUtils.selectHotbar(mc.player, prevSlot);
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
            double perTick = Math.min(velocity.get() / 20.0, 20.0);
            p.setDeltaMovement(dir.scale(perTick));
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

    private static boolean isSpear(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        try { return stack.getItem().toString().toLowerCase().contains("spear"); }
        catch (Throwable t) { return false; }
    }

    private static boolean isMace(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        try { return stack.getItem().toString().toLowerCase().contains("mace"); }
        catch (Throwable t) { return false; }
    }

    private static int findSpearHotbar(LocalPlayer p) {
        for (int i = 0; i < 9; i++) if (isSpear(p.getInventory().getItem(i))) return i;
        return -1;
    }

    private static int findMaceHotbar(LocalPlayer p) {
        for (int i = 0; i < 9; i++) if (isMace(p.getInventory().getItem(i))) return i;
        return -1;
    }
}
