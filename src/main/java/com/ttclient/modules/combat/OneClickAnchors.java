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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RespawnAnchorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Place anchor → charge → explode. Strictly one sequence; slow enough to not spam. */
public class OneClickAnchors extends Module {
    public final NumberSetting charges = addSetting(new NumberSetting("Charges", "Glowstone charges before explode (1-4)", 1, 1, 4, 1));
    public final NumberSetting delay = addSetting(new NumberSetting("Delay", "Ticks between charge actions", 2, 0, 10, 1));
    public final NumberSetting explodeLock = addSetting(new NumberSetting("ExplodeLock", "Ticks to ignore anchors after detonate", 18, 6, 60, 1));
    public final NumberSetting range = addSetting(new NumberSetting("Range", "Max range", 5.0, 2.0, 16.0, 0.5));
    public final BoolSetting onlyLook = addSetting(new BoolSetting("OnlyLook", "Only process the anchor you are looking at", true));
    public final BoolSetting switchBack = addSetting(new BoolSetting("SwitchBack", "Restore hotbar after actions", true));

    private BlockPos target;
    private int cooldown;
    private int lockTicks;
    private int prevSlot = -1;
    private boolean working;

    public OneClickAnchors() {
        super("OneClickAnchors", "Place an anchor — auto charge + explode (slow, 1 at a time)", Category.COMBAT);
    }

    @Override
    public void onDisable() {
        target = null; cooldown = 0; lockTicks = 0; prevSlot = -1; working = false;
    }

    @Override
    public void onTick() {
        Minecraft mc = mc();
        if (mc == null || mc.player == null || mc.level == null || mc.gameMode == null) return;
        LocalPlayer p = mc.player;

        if (lockTicks > 0) { lockTicks--; target = null; working = false; return; }
        if (cooldown > 0) { cooldown--; return; }

        double r = range.get();
        BlockPos looked = lookingAtAnchor(mc);

        if (onlyLook.get()) {
            if (looked == null) { target = null; working = false; return; }
            if (!working || target == null || !target.equals(looked)) {
                target = looked;
                working = true;
            }
        } else {
            if (looked != null && !working) { target = looked; working = true; }
            else if (!working || target == null || !isAnchor(mc, target)) {
                working = false; target = null;
                target = findNearby(mc, p, r);
                if (target != null) working = true;
            }
        }

        if (target == null || !isAnchor(mc, target)) { target = null; working = false; return; }
        double distSq = p.distanceToSqr(target.getX() + 0.5, target.getY() + 0.5, target.getZ() + 0.5);
        if (distSq > r * r) { target = null; working = false; return; }

        int c = getCharges(mc.level.getBlockState(target));
        int want = Math.max(1, charges.getInt());
        if (prevSlot < 0 && switchBack.get()) prevSlot = p.getInventory().getSelectedSlot();

        if (c < want) {
            if (doCharge(mc, p, target)) { cooldown = Math.max(1, delay.getInt()); }
            else if (c >= 1 && doExplode(mc, p, target)) finish(p);
            return;
        }
        if (c >= 1 && doExplode(mc, p, target)) finish(p);
    }

    private void finish(LocalPlayer p) {
        target = null; working = false; cooldown = 0;
        lockTicks = Math.max(6, explodeLock.getInt());
        if (switchBack.get() && prevSlot >= 0) InvUtils.selectHotbar(p, prevSlot);
        prevSlot = -1;
    }

    private BlockPos lookingAtAnchor(Minecraft mc) {
        if (mc.hitResult == null || mc.hitResult.getType() != HitResult.Type.BLOCK) return null;
        BlockHitResult bhr = (BlockHitResult) mc.hitResult;
        BlockPos pos = bhr.getBlockPos();
        if (isAnchor(mc, pos)) return pos;
        BlockPos placed = pos.relative(bhr.getDirection());
        if (isAnchor(mc, placed)) return placed;
        return null;
    }

    private BlockPos findNearby(Minecraft mc, LocalPlayer p, double r) {
        BlockPos base = p.blockPosition();
        int reach = Math.min(3, (int) Math.ceil(r));
        double maxD = Math.min(r * r, 3.5 * 3.5);
        BlockPos best = null; double bestD = maxD;
        for (BlockPos pos : BlockPos.betweenClosed(base.offset(-reach, -1, -reach), base.offset(reach, 2, reach))) {
            if (!isAnchor(mc, pos)) continue;
            double d = p.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
            if (d < bestD) { bestD = d; best = pos.immutable(); }
        }
        return best;
    }

    private boolean isAnchor(Minecraft mc, BlockPos pos) {
        return mc.level.getBlockState(pos).is(Blocks.RESPAWN_ANCHOR);
    }

    private int getCharges(BlockState st) {
        try {
            if (st.hasProperty(RespawnAnchorBlock.CHARGE)) return st.getValue(RespawnAnchorBlock.CHARGE);
        } catch (Throwable ignored) {}
        return 0;
    }

    private boolean doCharge(Minecraft mc, LocalPlayer p, BlockPos anchor) {
        int gs = InvUtils.findHotbar(p, Items.GLOWSTONE);
        if (gs < 0) return false;
        InvUtils.selectHotbar(p, gs);
        return clickBlock(mc, p, anchor);
    }

    private boolean doExplode(Minecraft mc, LocalPlayer p, BlockPos anchor) {
        if (p.getMainHandItem().is(Items.GLOWSTONE) || p.getMainHandItem().is(Items.RESPAWN_ANCHOR)) {
            for (int i = 0; i < 9; i++) {
                ItemStack s = p.getInventory().getItem(i);
                if (s.isEmpty() || (!s.is(Items.GLOWSTONE) && !s.is(Items.RESPAWN_ANCHOR))) {
                    InvUtils.selectHotbar(p, i); break;
                }
            }
        }
        if (p.getMainHandItem().is(Items.GLOWSTONE) || p.getMainHandItem().is(Items.RESPAWN_ANCHOR)) return false;
        return clickBlock(mc, p, anchor);
    }

    private boolean clickBlock(Minecraft mc, LocalPlayer p, BlockPos pos) {
        Vec3 hit = Vec3.atCenterOf(pos).add(0, 0.45, 0);
        BlockHitResult bhr = new BlockHitResult(hit, Direction.UP, pos, false);
        try {
            mc.gameMode.useItemOn(p, InteractionHand.MAIN_HAND, bhr);
            p.swing(InteractionHand.MAIN_HAND);
            return true;
        } catch (Throwable t) { return false; }
    }
}
