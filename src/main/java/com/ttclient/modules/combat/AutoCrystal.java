package com.ttclient.modules.combat;

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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** Place/break end crystals near players (client-side CPVP). */
public class AutoCrystal extends Module {
    public final NumberSetting range = addSetting(new NumberSetting("Range", "Place/break range", 4.5, 2, 6, 0.1));
    public final NumberSetting delay = addSetting(new NumberSetting("Delay", "Ticks between actions", 2, 0, 10, 1));
    public final BoolSetting place = addSetting(new BoolSetting("Place", "Place crystals", true));
    public final BoolSetting breakCrystal = addSetting(new BoolSetting("Break", "Break crystals", true));
    public final BoolSetting playersOnly = addSetting(new BoolSetting("Players", "Only near other players", true));
    public final NumberSetting minDamageHint = addSetting(new NumberSetting("MinYDiff", "Prefer crystals near player feet", 2, 0, 6, 1));

    private int timer;

    public AutoCrystal() {
        super("AutoCrystal", "Auto place & break end crystals", Category.COMBAT);
    }

    @Override
    public void onTick() {
        Minecraft mc = mc();
        if (mc == null || mc.player == null || mc.level == null || mc.gameMode == null) return;
        if (timer > 0) { timer--; return; }

        LocalPlayer p = mc.player;
        double r = range.get();

        if (breakCrystal.get() && doBreak(mc, p, r)) {
            timer = delay.getInt();
            return;
        }
        if (place.get() && doPlace(mc, p, r)) {
            timer = delay.getInt();
        }
    }

    private boolean doBreak(Minecraft mc, LocalPlayer p, double r) {
        EndCrystal best = null;
        double bestD = r * r;
        AABB box = p.getBoundingBox().inflate(r);
        for (Entity e : mc.level.getEntities(p, box)) {
            if (!(e instanceof EndCrystal crystal) || !crystal.isAlive()) continue;
            double d = p.distanceToSqr(crystal);
            if (d < bestD) {
                bestD = d;
                best = crystal;
            }
        }
        if (best == null) return false;
        mc.gameMode.attack(p, best);
        p.swing(InteractionHand.MAIN_HAND);
        return true;
    }

    private boolean doPlace(Minecraft mc, LocalPlayer p, double r) {
        int slot = InvUtils.findHotbar(p, Items.END_CRYSTAL);
        if (slot < 0) return false;

        Player target = nearestPlayer(mc, p, r + 2);
        if (playersOnly.get() && target == null) return false;

        BlockPos base = findPlaceBase(mc, p, target, r);
        if (base == null) return false;

        // Don't place if crystal already there
        AABB crystalBox = new AABB(base.above()).inflate(0.1);
        for (Entity e : mc.level.getEntities(p, crystalBox)) {
            if (e instanceof EndCrystal) return false;
        }

        int prev = p.getInventory().getSelectedSlot();
        InvUtils.selectHotbar(p, slot);

        Vec3 hit = Vec3.atCenterOf(base);
        BlockHitResult bhr = new BlockHitResult(hit, Direction.UP, base, false);
        try {
            mc.gameMode.useItemOn(p, InteractionHand.MAIN_HAND, bhr);
            p.swing(InteractionHand.MAIN_HAND);
        } catch (Throwable ignored) {}

        InvUtils.selectHotbar(p, prev);
        return true;
    }

    private Player nearestPlayer(Minecraft mc, LocalPlayer self, double r) {
        Player best = null;
        double bestD = r * r;
        for (Player pl : mc.level.players()) {
            if (pl == self || !pl.isAlive()) continue;
            double d = self.distanceToSqr(pl);
            if (d < bestD) { bestD = d; best = pl; }
        }
        return best;
    }

    private BlockPos findPlaceBase(Minecraft mc, LocalPlayer self, Player target, double r) {
        BlockPos center = target != null ? target.blockPosition() : self.blockPosition();
        BlockPos best = null;
        double bestScore = Double.MAX_VALUE;
        int rad = (int) Math.ceil(Math.min(r, 4));

        for (int dx = -rad; dx <= rad; dx++) {
            for (int dz = -rad; dz <= rad; dz++) {
                for (int dy = -1; dy <= 2; dy++) {
                    BlockPos pos = center.offset(dx, dy, dz);
                    if (self.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > r * r) continue;
                    BlockState st = mc.level.getBlockState(pos);
                    if (!st.is(Blocks.OBSIDIAN) && !st.is(Blocks.BEDROCK)) continue;
                    BlockPos above = pos.above();
                    if (!mc.level.getBlockState(above).isAir()) continue;
                    if (!mc.level.getBlockState(above.above()).isAir()) continue;

                    double score = target != null
                            ? target.distanceToSqr(pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5)
                            : self.distanceToSqr(pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5);
                    if (score < bestScore) {
                        bestScore = score;
                        best = pos;
                    }
                }
            }
        }
        return best;
    }
}
