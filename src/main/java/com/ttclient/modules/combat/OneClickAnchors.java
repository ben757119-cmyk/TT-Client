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

/**
 * Place a respawn anchor → charge with glowstone → detonate.
 * Only one anchor sequence at a time; post-explode lock stops hold-place spam.
 */
public class OneClickAnchors extends Module {
    public final NumberSetting charges = addSetting(new NumberSetting("Charges", "Glowstone charges before explode (1-4)", 1, 1, 4, 1));
    public final NumberSetting delay = addSetting(new NumberSetting("Delay", "Ticks between charge actions", 0, 0, 10, 1));
    public final NumberSetting explodeLock = addSetting(new NumberSetting("ExplodeLock", "Ticks to ignore new anchors after detonate", 8, 2, 40, 1));
    public final NumberSetting range = addSetting(new NumberSetting("Range", "Scan range when Rage is off", 6.0, 2.0, 64.0, 0.5));
    public final BoolSetting useRage = addSetting(new BoolSetting("UseRageRange", "Use Rage reach when Rage is enabled", true));
    public final BoolSetting onlyOwn = addSetting(new BoolSetting("OnlyOwn", "Prefer looked-at / very nearby anchors", true));
    public final BoolSetting switchBack = addSetting(new BoolSetting("SwitchBack", "Restore hotbar slot after actions", true));

    private BlockPos target;
    private int cooldown;
    private int lockTicks;
    private int prevSlot = -1;
    private boolean working;

    public OneClickAnchors() {
        super("OneClickAnchors", "Place an anchor — auto charge + explode (1 at a time)", Category.COMBAT);
    }

    @Override
    public void onDisable() {
        target = null;
        cooldown = 0;
        lockTicks = 0;
        prevSlot = -1;
        working = false;
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
        LocalPlayer p = mc.player;

        if (lockTicks > 0) {
            lockTicks--;
            target = null;
            working = false;
            return;
        }

        if (cooldown > 0) {
            cooldown--;
            return;
        }

        double r = effectiveRange();

        // Prefer what you're looking at; only pick a new target when idle
        BlockPos looked = lookingAtAnchor(mc);
        if (looked != null && !working) {
            target = looked;
            working = true;
        } else if (!working || target == null || !isAnchor(mc, target)) {
            if (working && (target == null || !isAnchor(mc, target))) {
                // sequence ended without explode
                working = false;
                target = null;
            }
            if (!working) {
                target = findNearbyAnchor(mc, p, r);
                if (target != null) working = true;
            }
        }

        if (target == null) return;
        if (!isAnchor(mc, target)) {
            target = null;
            working = false;
            return;
        }

        double distSq = p.distanceToSqr(target.getX() + 0.5, target.getY() + 0.5, target.getZ() + 0.5);
        if (distSq > r * r) {
            target = null;
            working = false;
            return;
        }

        int c = getCharges(mc.level.getBlockState(target));
        int want = Math.max(1, charges.getInt());

        if (prevSlot < 0 && switchBack.get()) {
            prevSlot = p.getInventory().getSelectedSlot();
        }

        if (c < want) {
            if (doCharge(mc, p, target)) {
                cooldown = delay.getInt();
            } else if (c >= 1 && doExplode(mc, p, target)) {
                finish(p);
            }
            return;
        }

        if (c >= 1 && doExplode(mc, p, target)) {
            finish(p);
        }
    }

    private void finish(LocalPlayer p) {
        target = null;
        working = false;
        cooldown = 0;
        lockTicks = Math.max(2, explodeLock.getInt());
        if (switchBack.get() && prevSlot >= 0) {
            InvUtils.selectHotbar(p, prevSlot);
        }
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

    private BlockPos findNearbyAnchor(Minecraft mc, LocalPlayer p, double r) {
        BlockPos base = p.blockPosition();
        // tight when OnlyOwn — stops scanning a pile of held-place anchors
        int reach = onlyOwn.get() ? Math.min(3, (int) Math.ceil(r)) : Math.min(8, (int) Math.ceil(r));
        double maxD = onlyOwn.get() ? Math.min(r * r, 4.5 * 4.5) : r * r;
        BlockPos best = null;
        double bestD = maxD;
        for (BlockPos pos : BlockPos.betweenClosed(base.offset(-reach, -2, -reach), base.offset(reach, 3, reach))) {
            if (!isAnchor(mc, pos)) continue;
            double d = p.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
            if (d < bestD) {
                bestD = d;
                best = pos.immutable();
            }
        }
        return best;
    }

    private boolean isAnchor(Minecraft mc, BlockPos pos) {
        return mc.level.getBlockState(pos).is(Blocks.RESPAWN_ANCHOR);
    }

    private int getCharges(BlockState st) {
        try {
            if (st.hasProperty(RespawnAnchorBlock.CHARGE)) {
                return st.getValue(RespawnAnchorBlock.CHARGE);
            }
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
                    InvUtils.selectHotbar(p, i);
                    break;
                }
            }
        }
        if (p.getMainHandItem().is(Items.GLOWSTONE) || p.getMainHandItem().is(Items.RESPAWN_ANCHOR)) {
            return false;
        }
        return clickBlock(mc, p, anchor);
    }

    private boolean clickBlock(Minecraft mc, LocalPlayer p, BlockPos pos) {
        Vec3 hit = Vec3.atCenterOf(pos).add(0, 0.45, 0);
        BlockHitResult bhr = new BlockHitResult(hit, Direction.UP, pos, false);
        try {
            mc.gameMode.useItemOn(p, InteractionHand.MAIN_HAND, bhr);
            p.swing(InteractionHand.MAIN_HAND);
            return true;
        } catch (Throwable t) {
            return false;
        }
    }
}
