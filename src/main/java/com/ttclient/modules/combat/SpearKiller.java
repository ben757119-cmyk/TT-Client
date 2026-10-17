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
import net.minecraft.world.phys.Vec3;

/**
 * Spear Killer — charge spear + inject relative velocity for max charge damage.
 *
 * Spear charge damage scales with relative velocity (blocks/sec × material multiplier).
 * This holds a charge and boosts client velocity toward the target so hits land hard.
 * Strongest in singleplayer / servers that trust client motion for spear charge.
 */
public class SpearKiller extends Module {
    public final NumberSetting range = addSetting(new NumberSetting(
            "Range", "Target range (spear sweet spot ~2–4.5)", 4.5, 2.0, 16.0, 0.1));
    public final BoolSetting useRage = addSetting(new BoolSetting(
            "UseRageRange", "Use Rage reach when Rage is on", true));
    public final NumberSetting velocity = addSetting(new NumberSetting(
            "Velocity", "Injected relative speed toward target (b/s)", 40.0, 5.0, 120.0, 1.0));
    public final NumberSetting cooldown = addSetting(new NumberSetting(
            "Cooldown", "Ticks between attack attempts", 2, 0, 20, 1));
    public final BoolSetting onlyPlayers = addSetting(new BoolSetting(
            "OnlyPlayers", "Only target players", true));
    public final BoolSetting autoSwitch = addSetting(new BoolSetting(
            "AutoSwitch", "Switch to a spear in hotbar", true));
    public final BoolSetting switchBack = addSetting(new BoolSetting(
            "SwitchBack", "Restore hotbar slot when disabled / no target", true));
    public final BoolSetting holdCharge = addSetting(new BoolSetting(
            "HoldCharge", "Hold use to keep spear charged", true));
    public final BoolSetting jabFallback = addSetting(new BoolSetting(
            "JabFallback", "Also jab-attack while charged", true));
    public final NumberSetting minDist = addSetting(new NumberSetting(
            "MinDist", "Min distance (spear deadzone ~2)", 1.8, 0.5, 3.5, 0.1));

    private int timer;
    private int prevSlot = -1;
    private boolean keyForced;

    public SpearKiller() {
        super("SpearKiller", "Charge spear + velocity for insta-kill hits", Category.COMBAT);
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

        LivingEntity target = findTarget(mc, p);
        if (target == null) {
            releaseUse();
            if (switchBack.get()) restoreSlot();
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

        if (timer > 0) return;

        if (jabFallback.get()) {
            try {
                mc.gameMode.attack(p, target);
                p.swing(InteractionHand.MAIN_HAND);
            } catch (Throwable ignored) {}
        }

        timer = Math.max(0, cooldown.getInt());
    }

    private double effectiveRange() {
        double r = range.get();
        if (useRage.get() && TTClientClient.modules != null) {
            Rage rage = TTClientClient.modules.getModule(Rage.class);
            if (rage != null && rage.isEnabled()) {
                r = Math.max(r, Math.min(rage.reach.get(), 64.0));
            }
        }
        return r;
    }

    private LivingEntity findTarget(Minecraft mc, LocalPlayer self) {
        double r = effectiveRange();
        double min = minDist.get();
        double minSq = min * min;
        LivingEntity best = null;
        double bestD = r * r;
        AABB box = self.getBoundingBox().inflate(r);
        for (Entity e : mc.level.getEntities(self, box)) {
            if (!(e instanceof LivingEntity living) || !living.isAlive() || living == self) continue;
            if (onlyPlayers.get() && !(living instanceof Player)) continue;
            double d = self.distanceToSqr(living);
            if (d < minSq) continue;
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
            prevSlot = p.getInventory().getSelectedSlot();
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
            Vec3 to = target.position().add(0, target.getBbHeight() * 0.5, 0);
            Vec3 dir = to.subtract(from);
            double len = dir.length();
            if (len < 1.0e-4) return;
            dir = dir.scale(1.0 / len);

            double bps = velocity.get();
            double perTick = bps / 20.0;
            Vec3 boost = dir.scale(perTick);

            p.setDeltaMovement(boost.x, Math.max(boost.y, -0.05), boost.z);
            p.hasImpulse = true;
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
            String n = stack.getItem().toString().toLowerCase();
            if (n.contains("spear")) return true;
            var key = stack.getItem().builtInRegistryHolder().key().location().getPath();
            return key != null && key.toLowerCase().contains("spear");
        } catch (Throwable t) {
            return stack.getItem().toString().toLowerCase().contains("spear");
        }
    }

    public static int findSpearHotbar(LocalPlayer p) {
        for (int i = 0; i < 9; i++) {
            if (isSpear(p.getInventory().getItem(i))) return i;
        }
        return -1;
    }
}
