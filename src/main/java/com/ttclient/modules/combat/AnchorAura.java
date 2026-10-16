package com.ttclient.modules.combat;

import com.ttclient.client.TTClientClient;
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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RespawnAnchorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Place / charge / detonate respawn anchors near players.
 * KeepShield: briefly use main-hand for anchors, then restore slot and re-block.
 */
public class AnchorAura extends Module {
    public final NumberSetting range = addSetting(new NumberSetting("Range", "Base range", 5.0, 2.0, 256.0, 0.5));
    public final NumberSetting delay = addSetting(new NumberSetting("Delay", "Ticks between actions", 2, 0, 20, 1));
    public final BoolSetting place = addSetting(new BoolSetting("Place", "Place anchors near target", true));
    public final BoolSetting charge = addSetting(new BoolSetting("Charge", "Charge with glowstone", true));
    public final BoolSetting explode = addSetting(new BoolSetting("Explode", "Detonate charged anchors", true));
    public final BoolSetting useRage = addSetting(new BoolSetting("UseRageRange", "Use Rage reach when Rage is on", true));
    public final BoolSetting keepShield = addSetting(new BoolSetting("KeepShield", "Hold/use shield while AnchorAura runs", true));
    public final NumberSetting charges = addSetting(new NumberSetting("Charges", "Glowstone charges before explode (1-4)", 4, 1, 4, 1));

    private int timer;

    public AnchorAura() {
        super("AnchorAura", "Place/charge/explode anchors (shield-friendly)", Category.COMBAT);
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

    private static boolean isShield(ItemStack stack) {
        return stack != null && !stack.isEmpty() && stack.is(Items.SHIELD);
    }

    /** Snapshot shield state before we touch the hotbar. */
    private static final class ShieldState {
        final int hotbarSlot;
        final boolean blocking;
        final boolean offhandShield;
        final boolean mainhandShield;

        ShieldState(LocalPlayer p) {
            hotbarSlot = p.getInventory().getSelectedSlot();
            blocking = p.isBlocking();
            offhandShield = isShield(p.getOffhandItem());
            mainhandShield = isShield(p.getMainHandItem());
        }
    }

    private void restoreShield(Minecraft mc, LocalPlayer p, ShieldState st) {
        if (!keepShield.get() || st == null) {
            if (st != null) InvUtils.selectHotbar(p, st.hotbarSlot);
            return;
        }

        // Always put their hotbar back
        InvUtils.selectHotbar(p, st.hotbarSlot);

        // Prefer offhand shield — re-press use so blocking resumes
        try {
            if (st.offhandShield) {
                // If they were blocking or holding use, re-start offhand use
                if (st.blocking || (mc.options != null && mc.options.keyUse.isDown())) {
                    mc.gameMode.useItem(p, InteractionHand.OFF_HAND);
                }
            } else if (st.mainhandShield) {
                if (st.blocking || (mc.options != null && mc.options.keyUse.isDown())) {
                    mc.gameMode.useItem(p, InteractionHand.MAIN_HAND);
                }
            }
        } catch (Throwable ignored) {}
    }

    @Override
    public void onTick() {
        Minecraft mc = mc();
        if (mc == null || mc.player == null || mc.level == null || mc.gameMode == null) return;
        if (timer > 0) { timer--; return; }

        LocalPlayer p = mc.player;
        double r = effectiveRange();

        // While KeepShield is on and player is actively blocking, still allow actions
        // (we only briefly swap main hand).

        BlockPos existing = findNearbyAnchor(mc, p, r);
        if (existing != null) {
            int c = getCharges(mc.level.getBlockState(existing));
            int want = charges.getInt();

            if (c < want && charge.get()) {
                if (doCharge(mc, p, existing)) {
                    timer = delay.getInt();
                    return;
                }
            }

            if (c >= 1 && c >= want && explode.get()) {
                if (doExplode(mc, p, existing)) {
                    timer = delay.getInt();
                    return;
                }
            }

            if (c > 0 && c < want && explode.get() && !charge.get()) {
                if (doExplode(mc, p, existing)) {
                    timer = delay.getInt();
                    return;
                }
            }
        }

        if (place.get()) {
            if (doPlace(mc, p, r)) {
                timer = delay.getInt();
            }
        }
    }

    private boolean doCharge(Minecraft mc, LocalPlayer p, BlockPos anchor) {
        int gs = InvUtils.findHotbar(p, Items.GLOWSTONE);
        if (gs < 0) return false;

        ShieldState st = new ShieldState(p);
        InvUtils.selectHotbar(p, gs);
        boolean ok = clickBlock(mc, p, anchor);
        restoreShield(mc, p, st);
        return ok;
    }

    private boolean doExplode(Minecraft mc, LocalPlayer p, BlockPos anchor) {
        ShieldState st = new ShieldState(p);

        // Must not hold glowstone or it charges instead
        if (p.getMainHandItem().is(Items.GLOWSTONE) || p.getInventory().getItem(st.hotbarSlot).is(Items.GLOWSTONE)) {
            for (int i = 0; i < 9; i++) {
                ItemStack s = p.getInventory().getItem(i);
                if (!s.is(Items.GLOWSTONE) && !isShield(s)) {
                    InvUtils.selectHotbar(p, i);
                    break;
                }
            }
            // If every slot is glowstone/shield, still try current non-gs
            if (p.getMainHandItem().is(Items.GLOWSTONE)) {
                for (int i = 0; i < 9; i++) {
                    if (!p.getInventory().getItem(i).is(Items.GLOWSTONE)) {
                        InvUtils.selectHotbar(p, i);
                        break;
                    }
                }
            }
        }

        boolean ok = clickBlock(mc, p, anchor);
        restoreShield(mc, p, st);
        return ok;
    }

    private boolean doPlace(Minecraft mc, LocalPlayer p, double r) {
        int slot = InvUtils.findHotbar(p, Items.RESPAWN_ANCHOR);
        if (slot < 0) return false;

        Player target = nearestEnemy(mc, p, r);
        if (target == null) return false;

        BlockPos feet = target.blockPosition();
        BlockPos[] candidates = {
                feet.north(), feet.south(), feet.east(), feet.west(),
                feet.north().above(), feet.south().above(), feet.east().above(), feet.west().above(),
                feet.above(2),
                feet.north().north(), feet.south().south(), feet.east().east(), feet.west().west()
        };

        ShieldState st = new ShieldState(p);

        for (BlockPos pos : candidates) {
            if (!isReplaceable(mc, pos)) continue;
            if (p.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > r * r) continue;
            if (pos.equals(p.blockPosition()) || pos.equals(p.blockPosition().above())) continue;

            if (tryPlaceAgainstNeighbor(mc, p, slot, pos)) {
                restoreShield(mc, p, st);
                return true;
            }
        }

        for (int dx = -2; dx <= 2; dx++) {
            for (int dy = 0; dy <= 2; dy++) {
                for (int dz = -2; dz <= 2; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) continue;
                    BlockPos pos = feet.offset(dx, dy, dz);
                    if (!isReplaceable(mc, pos)) continue;
                    if (p.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > r * r) continue;
                    if (tryPlaceAgainstNeighbor(mc, p, slot, pos)) {
                        restoreShield(mc, p, st);
                        return true;
                    }
                }
            }
        }

        restoreShield(mc, p, st);
        return false;
    }

    private boolean isReplaceable(Minecraft mc, BlockPos pos) {
        BlockState st = mc.level.getBlockState(pos);
        return st.isAir() || st.canBeReplaced();
    }

    private boolean tryPlaceAgainstNeighbor(Minecraft mc, LocalPlayer p, int slot, BlockPos placePos) {
        Direction[] order = {
                Direction.DOWN, Direction.NORTH, Direction.SOUTH,
                Direction.EAST, Direction.WEST, Direction.UP
        };

        for (Direction towardSupport : order) {
            BlockPos support = placePos.relative(towardSupport);
            BlockState ss = mc.level.getBlockState(support);
            if (ss.isAir() || ss.canBeReplaced()) continue;
            Direction face = towardSupport.getOpposite();

            InvUtils.selectHotbar(p, slot);

            Vec3 hit = Vec3.atCenterOf(support).add(
                    face.getStepX() * 0.5,
                    face.getStepY() * 0.5,
                    face.getStepZ() * 0.5
            );
            BlockHitResult bhr = new BlockHitResult(hit, face, support, false);
            try {
                var result = mc.gameMode.useItemOn(p, InteractionHand.MAIN_HAND, bhr);
                p.swing(InteractionHand.MAIN_HAND);
                if (!isReplaceable(mc, placePos)) return true;
                if (result != null) return true;
            } catch (Throwable ignored) {}
        }
        return false;
    }

    private boolean clickBlock(Minecraft mc, LocalPlayer p, BlockPos pos) {
        for (Direction face : Direction.values()) {
            Vec3 hit = Vec3.atCenterOf(pos).add(
                    face.getStepX() * 0.5,
                    face.getStepY() * 0.5,
                    face.getStepZ() * 0.5
            );
            BlockHitResult bhr = new BlockHitResult(hit, face, pos, false);
            try {
                mc.gameMode.useItemOn(p, InteractionHand.MAIN_HAND, bhr);
                p.swing(InteractionHand.MAIN_HAND);
                return true;
            } catch (Throwable ignored) {}
        }
        return false;
    }

    private int getCharges(BlockState st) {
        try {
            if (st.hasProperty(RespawnAnchorBlock.CHARGE)) {
                return st.getValue(RespawnAnchorBlock.CHARGE);
            }
        } catch (Throwable ignored) {}
        return 0;
    }

    private BlockPos findNearbyAnchor(Minecraft mc, LocalPlayer p, double r) {
        BlockPos o = p.blockPosition();
        int ri = (int) Math.ceil(Math.min(r, 48));
        ri = Math.max(ri, 8);

        BlockPos best = null;
        double bestD = r * r;
        for (int dx = -ri; dx <= ri; dx++) {
            for (int dy = -3; dy <= 4; dy++) {
                for (int dz = -ri; dz <= ri; dz++) {
                    BlockPos pos = o.offset(dx, dy, dz);
                    if (!mc.level.getBlockState(pos).is(Blocks.RESPAWN_ANCHOR)) continue;
                    double d = p.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
                    if (d <= bestD) {
                        bestD = d;
                        best = pos;
                    }
                }
            }
        }
        return best;
    }

    private Player nearestEnemy(Minecraft mc, LocalPlayer self, double r) {
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
}
