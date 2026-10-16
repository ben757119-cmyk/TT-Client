package com.ttclient.modules.combat;

import com.ttclient.client.TTClientClient;
import com.ttclient.gui.ClickGUIScreen;
import com.ttclient.modules.Category;
import com.ttclient.modules.Module;
import com.ttclient.settings.BoolSetting;
import com.ttclient.settings.NumberSetting;
import com.ttclient.util.InvUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** All-in-one CPVP: aura + aim + d-tap + crits + crystals + totem + weapon swap. */
public class Destroyer extends Module {
    public final NumberSetting range = addSetting(new NumberSetting("Range", "Attack range", 4.5, 2.5, 8.0, 0.1));
    public final NumberSetting crystalRange = addSetting(new NumberSetting("CrystalRange", "Crystal range", 5.0, 2.0, 6.0, 0.1));
    public final BoolSetting rotate = addSetting(new BoolSetting("Rotate", "Aim at target", true));
    public final BoolSetting weaponSwap = addSetting(new BoolSetting("WeaponSwap", "Best sword/mace", true));
    public final BoolSetting dTap = addSetting(new BoolSetting("DTap", "Sprint-reset / W-tap after hits", true));
    public final BoolSetting crits = addSetting(new BoolSetting("Crits", "Jump crit when grounded", true));
    public final BoolSetting breakCrystals = addSetting(new BoolSetting("BreakCrystals", "Punch crystals", true));
    public final BoolSetting placeCrystals = addSetting(new BoolSetting("PlaceCrystals", "Place crystals by target", true));
    public final BoolSetting autoTotem = addSetting(new BoolSetting("AutoTotem", "Totem offhand", true));
    public final BoolSetting keepSprint = addSetting(new BoolSetting("KeepSprint", "Sprint when not tapping", true));
    public final BoolSetting useRage = addSetting(new BoolSetting("UseRageRange", "Use Rage reach", true));
    public final NumberSetting totemDelay = addSetting(new NumberSetting("TotemDelay", "Totem swap delay", 2, 1, 15, 1));
    public final NumberSetting tapTicks = addSetting(new NumberSetting("TapTicks", "D-tap hold length", 2, 1, 6, 1));

    private int totemCd;
    private int tapLeft;
    private int placeCd;
    private boolean wasForward;

    public Destroyer() {
        super("Destroyer", "Full CPVP stack — aura, d-tap, crits, crystals, totem", Category.COMBAT);
    }

    @Override
    public void onDisable() {
        totemCd = 0;
        tapLeft = 0;
        placeCd = 0;
        restoreForward();
    }

    private double effectiveRange() {
        double r = range.get();
        if (useRage.get() && TTClientClient.modules != null) {
            Rage rage = TTClientClient.modules.getModule(Rage.class);
            if (rage != null && rage.isEnabled()) r = Math.max(r, Math.min(rage.reach.get(), 8.0));
        }
        return r;
    }

    private boolean shouldPause(Minecraft mc) {
        try {
            var s = mc.gui.screen();
            if (s == null) return false;
            if (s instanceof ClickGUIScreen) return true;
            String n = s.getClass().getName().toLowerCase();
            return n.contains("container") || n.contains("chest") || n.contains("inventory")
                    || n.contains("furnace") || n.contains("shulker") || n.contains("hopper")
                    || n.contains("anvil") || n.contains("beacon") || n.contains("brewing");
        } catch (Throwable t) {
            return false;
        }
    }

    @Override
    public void onTick() {
        Minecraft mc = mc();
        if (mc == null || mc.player == null || mc.level == null || mc.gameMode == null) return;
        if (shouldPause(mc)) return;

        LocalPlayer p = mc.player;

        if (tapLeft > 0) {
            tapLeft--;
            try {
                if (mc.options != null) {
                    mc.options.keyUp.setDown(false);
                    p.setSprinting(false);
                }
            } catch (Throwable ignored) {}
            if (tapLeft == 0) restoreForward();
        } else if (keepSprint.get() && mc.options != null && mc.options.keyUp.isDown()) {
            p.setSprinting(true);
        }

        if (autoTotem.get()) {
            if (totemCd > 0) totemCd--;
            else if (!p.getOffhandItem().is(Items.TOTEM_OF_UNDYING)) {
                if (InvUtils.equipTotem(p)) totemCd = totemDelay.getInt();
            }
        }

        double r = effectiveRange();

        if (breakCrystals.get()) {
            EndCrystal crystal = nearestCrystal(mc, p, crystalRange.get());
            if (crystal != null && p.getAttackStrengthScale(0.5f) >= 0.8f) {
                if (rotate.get()) lookAt(p, crystal);
                mc.gameMode.attack(p, crystal);
                p.swing(InteractionHand.MAIN_HAND);
                afterHit(mc, p);
                return;
            }
        }

        if (placeCrystals.get() && placeCd <= 0) {
            if (tryPlaceCrystal(mc, p, r)) placeCd = 4;
        } else if (placeCd > 0) placeCd--;

        Player target = nearestPlayer(mc, p, r);
        if (target == null) return;

        if (weaponSwap.get()) {
            int w = InvUtils.findBestWeapon(p);
            if (w >= 0) InvUtils.selectHotbar(p, w);
        }

        if (rotate.get()) lookAt(p, target);

        if (p.getAttackStrengthScale(0.5f) < 0.92f) return;

        if (crits.get() && p.onGround() && !p.isInWater() && !p.onClimbable()) {
            try { p.jumpFromGround(); } catch (Throwable ignored) {}
        }

        mc.gameMode.attack(p, target);
        p.swing(InteractionHand.MAIN_HAND);
        afterHit(mc, p);
    }

    private void afterHit(Minecraft mc, LocalPlayer p) {
        if (!dTap.get() || mc.options == null) return;
        wasForward = mc.options.keyUp.isDown();
        mc.options.keyUp.setDown(false);
        p.setSprinting(false);
        tapLeft = tapTicks.getInt();
    }

    private void restoreForward() {
        Minecraft mc = mc();
        if (mc == null || mc.options == null) return;
        if (wasForward) {
            try { mc.options.keyUp.setDown(true); } catch (Throwable ignored) {}
        }
        wasForward = false;
    }

    private boolean tryPlaceCrystal(Minecraft mc, LocalPlayer p, double r) {
        int slot = InvUtils.findHotbar(p, Items.END_CRYSTAL);
        if (slot < 0) return false;
        Player target = nearestPlayer(mc, p, r + 1);
        if (target == null) return false;

        BlockPos base = null;
        BlockPos feet = target.blockPosition();
        BlockPos[] candidates = {
                feet.below(), feet.north().below(), feet.south().below(),
                feet.east().below(), feet.west().below(),
                feet.north(), feet.south(), feet.east(), feet.west()
        };
        for (BlockPos c : candidates) {
            if (!canPlaceCrystal(mc, c)) continue;
            if (p.distanceToSqr(c.getX() + 0.5, c.getY() + 1, c.getZ() + 0.5) > crystalRange.get() * crystalRange.get())
                continue;
            base = c;
            break;
        }
        if (base == null) return false;

        AABB box = new AABB(base.above()).inflate(0.2);
        for (Entity e : mc.level.getEntities(p, box)) {
            if (e instanceof EndCrystal) return false;
        }

        int prev = p.getInventory().getSelectedSlot();
        InvUtils.selectHotbar(p, slot);
        Vec3 hit = Vec3.atCenterOf(base).add(0, 0.5, 0);
        BlockHitResult bhr = new BlockHitResult(hit, Direction.UP, base, false);
        try {
            mc.gameMode.useItemOn(p, InteractionHand.MAIN_HAND, bhr);
            p.swing(InteractionHand.MAIN_HAND);
        } catch (Throwable ignored) {}
        InvUtils.selectHotbar(p, prev);
        return true;
    }

    private boolean canPlaceCrystal(Minecraft mc, BlockPos base) {
        BlockState st = mc.level.getBlockState(base);
        String n = st.getBlock().toString().toLowerCase();
        boolean obby = n.contains("obsidian") || n.contains("bedrock");
        if (!obby) return false;
        return mc.level.getBlockState(base.above()).isAir();
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
