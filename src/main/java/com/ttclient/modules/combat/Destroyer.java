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
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Single toggle: auto totem + aim + weapon + hit players (+ crystal break). */
public class Destroyer extends Module {
    public final NumberSetting range = addSetting(new NumberSetting("Range", "Attack range", 4.5, 2.5, 6.0, 0.1));
    public final NumberSetting crystalRange = addSetting(new NumberSetting("CrystalRange", "Break crystals in range", 5.0, 2.0, 6.0, 0.1));
    public final BoolSetting rotate = addSetting(new BoolSetting("Rotate", "Snap aim to target", true));
    public final BoolSetting weaponSwap = addSetting(new BoolSetting("WeaponSwap", "Use best sword/mace", true));
    public final BoolSetting breakCrystals = addSetting(new BoolSetting("BreakCrystals", "Punch nearby crystals", true));
    public final BoolSetting autoTotem = addSetting(new BoolSetting("AutoTotem", "Keep totem in offhand", true));
    public final NumberSetting totemDelay = addSetting(new NumberSetting("TotemDelay", "Ticks between totem swaps", 4, 1, 15, 1));

    private int totemCd;

    public Destroyer() {
        super("Destroyer", "One toggle — aim, hit, totem, crystal break", Category.COMBAT);
    }

    @Override
    public void onDisable() {
        totemCd = 0;
    }

    @Override
    public void onTick() {
        Minecraft mc = mc();
        if (mc == null || mc.player == null || mc.level == null || mc.gameMode == null) return;
        if (InvUtils.hasScreen(mc)) return;

        LocalPlayer p = mc.player;

        if (autoTotem.get()) {
            if (totemCd > 0) totemCd--;
            else if (!p.getOffhandItem().is(Items.TOTEM_OF_UNDYING)) {
                int slot = InvUtils.findItemSlot(p, Items.TOTEM_OF_UNDYING);
                if (slot >= 0 && InvUtils.swapToOffhand(p, slot)) {
                    totemCd = totemDelay.getInt();
                }
            }
        }

        if (breakCrystals.get()) {
            EndCrystal crystal = nearestCrystal(mc, p, crystalRange.get());
            if (crystal != null) {
                if (rotate.get()) lookAt(p, crystal);
                if (p.getAttackStrengthScale(0.5f) >= 0.85f) {
                    mc.gameMode.attack(p, crystal);
                    p.swing(InteractionHand.MAIN_HAND);
                    return;
                }
            }
        }

        Player target = nearestPlayer(mc, p, range.get());
        if (target == null) return;

        if (weaponSwap.get()) {
            int w = InvUtils.findBestWeapon(p);
            if (w >= 0) InvUtils.selectHotbar(p, w);
        }

        if (rotate.get()) lookAt(p, target);

        if (p.getAttackStrengthScale(0.5f) >= 0.92f) {
            mc.gameMode.attack(p, target);
            p.swing(InteractionHand.MAIN_HAND);
        }
    }

    private static void lookAt(LocalPlayer p, Entity target) {
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

    private Player nearestPlayer(Minecraft mc, LocalPlayer self, double r) {
        Player best = null;
        double bestD = r * r;
        for (Player pl : mc.level.players()) {
            if (pl == self || !pl.isAlive()) continue;
            double d = self.distanceToSqr(pl);
            if (d <= bestD) {
                bestD = d;
                best = pl;
            }
        }
        return best;
    }

    private EndCrystal nearestCrystal(Minecraft mc, LocalPlayer self, double r) {
        EndCrystal best = null;
        double bestD = r * r;
        AABB box = self.getBoundingBox().inflate(r);
        for (Entity e : mc.level.getEntities(self, box)) {
            if (!(e instanceof EndCrystal c) || !c.isAlive()) continue;
            double d = self.distanceToSqr(c);
            if (d < bestD) {
                bestD = d;
                best = c;
            }
        }
        return best;
    }
}
