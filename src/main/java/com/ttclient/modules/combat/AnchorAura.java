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
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RespawnAnchorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Place / charge / detonate respawn anchors near players.
 * Placement targets air beside the enemy (not inside them).
 * Range expands automatically when Rage is enabled.
 */
public class AnchorAura extends Module {
    public final NumberSetting range = addSetting(new NumberSetting("Range", "Base range", 5.0, 2.0, 256.0, 0.5));
    public final NumberSetting delay = addSetting(new NumberSetting("Delay", "Ticks between actions", 2, 0, 20, 1));
    public final BoolSetting place = addSetting(new BoolSetting("Place", "Place anchors near target", true));
    public final BoolSetting charge = addSetting(new BoolSetting("Charge", "Charge with glowstone", true));
    public final BoolSetting explode = addSetting(new BoolSetting("Explode", "Detonate charged anchors", true));
    public final BoolSetting useRage = addSetting(new BoolSetting("UseRageRange", "Use Rage reach when Rage is on", true));
    public final NumberSetting charges = addSetting(new NumberSetting("Charges", "Glowstone charges before explode (1-4)", 4, 1, 4, 1));

    private int timer;

    public AnchorAura() {
        super("AnchorAura", "Place/charge/explode respawn anchors", Category.COMBAT);
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

    @Override
    public void onTick() {
        Minecraft mc = mc();
        if (mc == null || mc.player == null || mc.level == null || mc.gameMode == null) return;
        if (timer > 0) { timer--; return; }

        LocalPlayer p = mc.player;
        double r = effectiveRange();

        // 1) Existing anchors: charge then explode
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

            // Still charging path if below want
            if (c > 0 && c < want && explode.get() && !charge.get()) {
                if (doExplode(mc, p, existing)) {
                    timer = delay.getInt();
                    return;
                }
            }
        }

        // 2) Place new anchor near nearest enemy
        if (place.get()) {
            if (doPlace(mc, p, r)) {
                timer = delay.getInt();
            }
        }
    }

    private boolean doCharge(Minecraft mc, LocalPlayer p, BlockPos anchor) {
        int gs = InvUtils.findHotbar(p, Items.GLOWSTONE);
        if (gs < 0) return false;
        int prev = p.getInventory().getSelectedSlot();
        InvUtils.selectHotbar(p, gs);
        boolean ok = clickBlock(mc, p, anchor);
        InvUtils.selectHotbar(p, prev);
        return ok;
    }

    private boolean doExplode(Minecraft mc, LocalPlayer p, BlockPos anchor) {
        // Must NOT be holding glowstone or it will charge instead
        int prev = p.getInventory().getSelectedSlot();
        if (p.getMainHandItem().is(Items.GLOWSTONE)) {
            // Switch to a non-glowstone slot if possible
            for (int i = 0; i < 9; i++) {
                if (!p.getInventory().getItem(i).is(Items.GLOWSTONE)) {
                    InvUtils.selectHotbar(p, i);
                    break;
                }
            }
        }
        boolean ok = clickBlock(mc, p, anchor);
        InvUtils.selectHotbar(p, prev);
        return ok;
    }

    private boolean doPlace(Minecraft mc, LocalPlayer p, double r) {
        int slot = InvUtils.findHotbar(p, Items.RESPAWN_ANCHOR);
        if (slot < 0) return false;

        Player target = nearestEnemy(mc, p, r);
        if (target == null) return false;

        BlockPos feet = target.blockPosition();
        // Prefer positions around the target, not inside them
        BlockPos[] candidates = {
                feet.north(), feet.south(), feet.east(), feet.west(),
                feet.north().above(), feet.south().above(), feet.east().above(), feet.west().above(),
                feet.above(2),
                feet.north().north(), feet.south().south(), feet.east().east(), feet.west().west(),
                feet.below() // only if air (unlikely)
        };

        // Sort-ish: try closer solid-adjacent air first
        for (BlockPos pos : candidates) {
            if (!isReplaceable(mc, pos)) continue;
            if (p.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > r * r) continue;
            // Don't place inside our own feet
            if (pos.equals(p.blockPosition()) || pos.equals(p.blockPosition().above())) continue;

            if (tryPlaceAgainstNeighbor(mc, p, slot, pos)) return true;
        }

        // Fallback: scan a small cube around target
        for (int dx = -2; dx <= 2; dx++) {
            for (int dy = 0; dy <= 2; dy++) {
                for (int dz = -2; dz <= 2; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) continue;
                    BlockPos pos = feet.offset(dx, dy, dz);
                    if (!isReplaceable(mc, pos)) continue;
                    if (p.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > r * r) continue;
                    if (tryPlaceAgainstNeighbor(mc, p, slot, pos)) return true;
                }
            }
        }
        return false;
    }

    private boolean isReplaceable(Minecraft mc, BlockPos pos) {
        BlockState st = mc.level.getBlockState(pos);
        return st.isAir() || st.canBeReplaced();
    }

    private boolean tryPlaceAgainstNeighbor(Minecraft mc, LocalPlayer p, int slot, BlockPos placePos) {
        // Prefer DOWN support (floor), then sides, then up
        Direction[] order = {
                Direction.DOWN, Direction.NORTH, Direction.SOUTH,
                Direction.EAST, Direction.WEST, Direction.UP
        };

        for (Direction towardSupport : order) {
            BlockPos support = placePos.relative(towardSupport);
            BlockState ss = mc.level.getBlockState(support);
            if (ss.isAir() || ss.canBeReplaced()) continue;
            // Click the support face that points toward placePos
            Direction face = towardSupport.getOpposite();

            int prev = p.getInventory().getSelectedSlot();
            InvUtils.selectHotbar(p, slot);

            // Hit vec on the face center
            Vec3 hit = Vec3.atCenterOf(support).add(
                    face.getStepX() * 0.5,
                    face.getStepY() * 0.5,
                    face.getStepZ() * 0.5
            );
            BlockHitResult bhr = new BlockHitResult(hit, face, support, false);
            try {
                var result = mc.gameMode.useItemOn(p, InteractionHand.MAIN_HAND, bhr);
                p.swing(InteractionHand.MAIN_HAND);
                InvUtils.selectHotbar(p, prev);
                // Consider success if the place pos is no longer replaceable OR interaction didn't fail hard
                if (!isReplaceable(mc, placePos)) return true;
                if (result != null) return true;
            } catch (Throwable t) {
                InvUtils.selectHotbar(p, prev);
            }
        }
        return false;
    }

    private boolean clickBlock(Minecraft mc, LocalPlayer p, BlockPos pos) {
        // Try each face so interaction registers
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
        int ri = (int) Math.ceil(Math.min(r, 64)); // scan cap for perf; still hits far if chunk loaded
        // If rage range is huge, still only scan reasonable area around player + expand a bit
        ri = Math.max(ri, (int) Math.ceil(Math.min(r, 16)));
        if (r > 16) ri = (int) Math.ceil(Math.min(r, 48));

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
