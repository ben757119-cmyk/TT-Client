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
 * Place / charge / detonate respawn anchors.
 * Tracks anchors near the *target* so far-range (Rage) still charges/explodes after place.
 */
public class AnchorAura extends Module {
    public final NumberSetting range = addSetting(new NumberSetting("Range", "Base range", 5.0, 2.0, 256.0, 0.5));
    public final NumberSetting delay = addSetting(new NumberSetting("Delay", "Ticks between actions", 2, 0, 20, 1));
    public final BoolSetting place = addSetting(new BoolSetting("Place", "Place anchors near target", true));
    public final BoolSetting charge = addSetting(new BoolSetting("Charge", "Charge with glowstone", true));
    public final BoolSetting explode = addSetting(new BoolSetting("Explode", "Detonate charged anchors", true));
    public final BoolSetting useRage = addSetting(new BoolSetting("UseRageRange", "Use Rage reach when Rage is on", true));
    public final BoolSetting keepShield = addSetting(new BoolSetting("KeepShield", "Hold/use shield while AnchorAura runs", true));
    public final BoolSetting safeAnchor = addSetting(new BoolSetting("SafeAnchor", "Place glowstone between you and close anchors", true));
    public final NumberSetting safeDist = addSetting(new NumberSetting("SafeDist", "Max distance to place safety glowstone", 5.0, 1.0, 12.0, 0.5));
    public final NumberSetting charges = addSetting(new NumberSetting("Charges", "Glowstone charges before explode (1-4)", 4, 1, 4, 1));

    private int timer;
    private BlockPos pendingExplode;
    /** Last anchor we placed or interacted with — preferred for charge/explode when far. */
    private BlockPos trackedAnchor;

    public AnchorAura() {
        super("AnchorAura", "Place/charge/explode anchors (safe + shield)", Category.COMBAT);
    }

    @Override
    public void onDisable() {
        pendingExplode = null;
        trackedAnchor = null;
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
        if (st == null) return;
        InvUtils.selectHotbar(p, st.hotbarSlot);
        if (!keepShield.get()) return;
        try {
            if (st.offhandShield) {
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
        Player target = nearestEnemy(mc, p, r);

        // Validate tracked anchor still exists
        if (trackedAnchor != null && !mc.level.getBlockState(trackedAnchor).is(Blocks.RESPAWN_ANCHOR)) {
            trackedAnchor = null;
        }

        // Finish delayed safe explode
        if (pendingExplode != null && explode.get()) {
            BlockPos pos = pendingExplode;
            pendingExplode = null;
            if (mc.level.getBlockState(pos).is(Blocks.RESPAWN_ANCHOR)) {
                if (doExplode(mc, p, pos)) {
                    timer = delay.getInt();
                    return;
                }
            }
        }

        BlockPos existing = findBestAnchor(mc, p, target, r);
        if (existing != null) {
            trackedAnchor = existing;
            int c = getCharges(mc.level.getBlockState(existing));
            int want = charges.getInt();

            // Always prefer finishing charge/explode on existing before placing more
            if (c < want && charge.get()) {
                if (doCharge(mc, p, existing)) {
                    timer = delay.getInt();
                    return;
                }
            }

            boolean readyToPop = c >= 1 && c >= want && explode.get();
            if (!readyToPop && c > 0 && explode.get() && !charge.get()) readyToPop = true;

            if (readyToPop) {
                double dist = Math.sqrt(p.distanceToSqr(
                        existing.getX() + 0.5, existing.getY() + 0.5, existing.getZ() + 0.5));

                if (safeAnchor.get() && dist <= safeDist.get()) {
                    if (placeSafetyGlowstone(mc, p, existing)) {
                        pendingExplode = existing;
                        timer = Math.max(1, delay.getInt());
                        return;
                    }
                }

                if (doExplode(mc, p, existing)) {
                    timer = delay.getInt();
                    return;
                }
            }

            // Anchor exists but not ready / failed interact — don't spam place more if one is tracked
            if (c < want && charge.get()) {
                timer = delay.getInt(); // wait and retry charge
                return;
            }
            if (readyToPop) {
                timer = delay.getInt();
                return;
            }
        }

        if (place.get() && target != null) {
            BlockPos placed = doPlaceReturn(mc, p, r, target);
            if (placed != null) {
                trackedAnchor = placed;
                timer = delay.getInt();
            }
        }
    }

    /** Prefer tracked → near target → near player. */
    private BlockPos findBestAnchor(Minecraft mc, LocalPlayer p, Player target, double r) {
        if (trackedAnchor != null && mc.level.getBlockState(trackedAnchor).is(Blocks.RESPAWN_ANCHOR)) {
            double d = p.distanceToSqr(trackedAnchor.getX() + 0.5, trackedAnchor.getY() + 0.5, trackedAnchor.getZ() + 0.5);
            if (d <= r * r + 4) return trackedAnchor;
        }

        BlockPos best = null;
        double bestScore = Double.MAX_VALUE;

        // Search around target first (this is what was missing when far away)
        if (target != null) {
            BlockPos found = scanAnchorsAround(mc, target.blockPosition(), 6, r, p);
            if (found != null) {
                double d = p.distanceToSqr(found.getX() + 0.5, found.getY() + 0.5, found.getZ() + 0.5);
                if (d <= r * r + 4) return found;
            }
        }

        // Search around player
        best = scanAnchorsAround(mc, p.blockPosition(), (int) Math.min(48, Math.ceil(r)), r, p);
        if (best != null) return best;

        // Wide scan around target again with larger radius
        if (target != null) {
            best = scanAnchorsAround(mc, target.blockPosition(), 10, r, p);
        }
        return best;
    }

    private BlockPos scanAnchorsAround(Minecraft mc, BlockPos origin, int ri, double maxRange, LocalPlayer p) {
        BlockPos best = null;
        double bestD = maxRange * maxRange + 16;
        for (int dx = -ri; dx <= ri; dx++) {
            for (int dy = -3; dy <= 4; dy++) {
                for (int dz = -ri; dz <= ri; dz++) {
                    BlockPos pos = origin.offset(dx, dy, dz);
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

    private boolean placeSafetyGlowstone(Minecraft mc, LocalPlayer p, BlockPos anchor) {
        if (hasCoverBetween(mc, p, anchor)) return true;

        int gs = InvUtils.findHotbar(p, Items.GLOWSTONE);
        if (gs < 0) return false;

        Vec3 from = p.getEyePosition(1.0f);
        Vec3 to = Vec3.atCenterOf(anchor);
        Vec3 delta = to.subtract(from);
        double len = delta.length();
        if (len < 0.5) return false;
        Vec3 dir = delta.scale(1.0 / len);

        BlockPos best = null;
        int steps = (int) Math.ceil(len * 2);
        for (int i = 1; i <= steps; i++) {
            double t = Math.min(0.55, Math.max(0.25, i / (double) steps));
            Vec3 pt = from.add(dir.scale(len * t));
            BlockPos bp = BlockPos.containing(pt);
            if (bp.equals(anchor)) continue;
            if (!isReplaceable(mc, bp)) {
                if (!mc.level.getBlockState(bp).is(Blocks.RESPAWN_ANCHOR)) return true;
                continue;
            }
            best = bp;
            double dPlayer = p.distanceToSqr(bp.getX() + 0.5, bp.getY() + 0.5, bp.getZ() + 0.5);
            if (dPlayer >= 0.8 && dPlayer <= 9.0) break;
        }

        if (best == null) {
            Direction facing = Direction.getApproximateNearest(dir.x, 0, dir.z);
            best = p.blockPosition().relative(facing);
            if (!isReplaceable(mc, best)) best = best.above();
            if (!isReplaceable(mc, best)) return hasCoverBetween(mc, p, anchor);
        }

        ShieldState st = new ShieldState(p);
        boolean placed = tryPlaceAgainstNeighbor(mc, p, gs, best);
        restoreShield(mc, p, st);
        return placed || hasCoverBetween(mc, p, anchor);
    }

    private boolean hasCoverBetween(Minecraft mc, LocalPlayer p, BlockPos anchor) {
        Vec3 from = p.getEyePosition(1.0f);
        Vec3 to = Vec3.atCenterOf(anchor);
        Vec3 delta = to.subtract(from);
        double len = delta.length();
        if (len < 0.5) return false;
        Vec3 dir = delta.scale(1.0 / len);
        int steps = (int) Math.ceil(len * 2);
        for (int i = 1; i < steps; i++) {
            BlockPos bp = BlockPos.containing(from.add(dir.scale(len * (i / (double) steps))));
            if (bp.equals(anchor)) continue;
            BlockState st = mc.level.getBlockState(bp);
            if (!st.isAir() && !st.canBeReplaced() && !st.is(Blocks.RESPAWN_ANCHOR)) return true;
        }
        return false;
    }

    private boolean doCharge(Minecraft mc, LocalPlayer p, BlockPos anchor) {
        int gs = InvUtils.findHotbar(p, Items.GLOWSTONE);
        if (gs < 0) return false;
        ShieldState st = new ShieldState(p);
        InvUtils.selectHotbar(p, gs);
        boolean ok = clickBlock(mc, p, anchor);
        restoreShield(mc, p, st);
        if (ok) trackedAnchor = anchor;
        return ok;
    }

    private boolean doExplode(Minecraft mc, LocalPlayer p, BlockPos anchor) {
        ShieldState st = new ShieldState(p);

        if (p.getMainHandItem().is(Items.GLOWSTONE) || p.getInventory().getItem(st.hotbarSlot).is(Items.GLOWSTONE)) {
            for (int i = 0; i < 9; i++) {
                ItemStack s = p.getInventory().getItem(i);
                if (!s.is(Items.GLOWSTONE) && !isShield(s)) {
                    InvUtils.selectHotbar(p, i);
                    break;
                }
            }
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

    /** @return position where anchor was placed, or null */
    private BlockPos doPlaceReturn(Minecraft mc, LocalPlayer p, double r, Player target) {
        int slot = InvUtils.findHotbar(p, Items.RESPAWN_ANCHOR);
        if (slot < 0) return null;

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
            // Don't place if an anchor is already adjacent to target
            if (tryPlaceAgainstNeighbor(mc, p, slot, pos)) {
                restoreShield(mc, p, st);
                // Confirm block appeared (client prediction)
                if (mc.level.getBlockState(pos).is(Blocks.RESPAWN_ANCHOR) || !isReplaceable(mc, pos)) {
                    return pos;
                }
                return pos; // still track intended place pos
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
                        return pos;
                    }
                }
            }
        }

        restoreShield(mc, p, st);
        return null;
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
