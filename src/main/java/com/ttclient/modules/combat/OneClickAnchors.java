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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RespawnAnchorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * When you place a respawn anchor, auto-charge it with glowstone then detonate.
 */
public class OneClickAnchors extends Module {
    public final NumberSetting charges = addSetting(new NumberSetting("Charges", "Glowstone charges before explode (1-4)", 4, 1, 4, 1));
    public final NumberSetting delay = addSetting(new NumberSetting("Delay", "Ticks between charge/explode actions", 1, 0, 10, 1));
    public final NumberSetting range = addSetting(new NumberSetting("Range", "Scan range for your placed anchors", 6.0, 2.0, 12.0, 0.5));
    public final BoolSetting onlyOwn = addSetting(new BoolSetting("OnlyOwn", "Only process anchors you just placed / look at", true));
    public final BoolSetting switchBack = addSetting(new BoolSetting("SwitchBack", "Restore hotbar slot after actions", true));

    private BlockPos target;
    private int cooldown;
    private int prevSlot = -1;

    public OneClickAnchors() {
        super("OneClickAnchors", "Place an anchor — auto charge + explode", Category.COMBAT);
    }

    @Override
    public void onDisable() {
        target = null;
        cooldown = 0;
        prevSlot = -1;
    }

    @Override
    public void onTick() {
        Minecraft mc = mc();
        if (mc == null || mc.player == null || mc.level == null || mc.gameMode == null) return;
        LocalPlayer p = mc.player;

        if (cooldown > 0) {
            cooldown--;
            return;
        }

        BlockPos looked = lookingAtAnchor(mc);
        if (looked != null) {
            target = looked;
        } else if (target == null || !isAnchor(mc, target)) {
            target = findNearbyAnchor(mc, p);
        }

        if (target == null) return;
        if (!isAnchor(mc, target)) {
            target = null;
            return;
        }

        double distSq = p.distanceToSqr(target.getX() + 0.5, target.getY() + 0.5, target.getZ() + 0.5);
        double r = range.get();
        if (distSq > r * r) {
            target = null;
            return;
        }

        int c = getCharges(mc.level.getBlockState(target));
        int want = charges.getInt();

        if (prevSlot < 0 && switchBack.get()) {
            prevSlot = p.getInventory().getSelectedSlot();
        }

        if (c < want) {
            if (doCharge(mc, p, target)) {
                cooldown = delay.getInt();
            } else {
                if (c >= 1 && doExplode(mc, p, target)) {
                    finish(p);
                }
            }
            return;
        }

        if (c >= 1) {
            if (doExplode(mc, p, target)) {
                finish(p);
            }
        }
    }

    private void finish(LocalPlayer p) {
        target = null;
        cooldown = delay.getInt();
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

    private BlockPos findNearbyAnchor(Minecraft mc, LocalPlayer p) {
        BlockPos base = p.blockPosition();
        int reach = onlyOwn.get() ? 3 : (int) Math.ceil(range.get());
        double maxD = onlyOwn.get() ? 4.0 * 4.0 : range.get() * range.get();
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
                if (s.isEmpty()) {
                    InvUtils.selectHotbar(p, i);
                    break;
                }
                if (!s.is(Items.GLOWSTONE) && !s.is(Items.RESPAWN_ANCHOR)) {
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
