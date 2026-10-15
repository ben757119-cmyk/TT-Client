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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RespawnAnchorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** Place / charge / detonate respawn anchors near players (Nether/Overworld rules apply server-side). */
public class AnchorAura extends Module {
    public final NumberSetting range = addSetting(new NumberSetting("Range", "Range", 4.5, 2, 6, 0.1));
    public final NumberSetting delay = addSetting(new NumberSetting("Delay", "Action delay", 3, 0, 15, 1));
    public final BoolSetting place = addSetting(new BoolSetting("Place", "Place anchors", true));
    public final BoolSetting charge = addSetting(new BoolSetting("Charge", "Charge with glowstone", true));
    public final BoolSetting explode = addSetting(new BoolSetting("Explode", "Click charged anchors", true));

    private int timer;

    public AnchorAura() {
        super("AnchorAura", "Respawn anchor place/charge/explode", Category.COMBAT);
    }

    @Override
    public void onTick() {
        Minecraft mc = mc();
        if (mc == null || mc.player == null || mc.level == null || mc.gameMode == null) return;
        if (timer > 0) { timer--; return; }

        LocalPlayer p = mc.player;
        double r = range.get();

        // Prioritize exploding / charging existing anchors
        BlockPos anchor = findNearbyAnchor(mc, p, r);
        if (anchor != null) {
            BlockState st = mc.level.getBlockState(anchor);
            int charges = 0;
            try {
                if (st.hasProperty(RespawnAnchorBlock.CHARGE))
                    charges = st.getValue(RespawnAnchorBlock.CHARGE);
            } catch (Throwable ignored) {}

            if (charges > 0 && explode.get() && !p.getMainHandItem().is(Items.GLOWSTONE)) {
                clickBlock(mc, p, anchor);
                timer = delay.getInt();
                return;
            }
            if (charges < 4 && charge.get()) {
                int gs = InvUtils.findHotbar(p, Items.GLOWSTONE);
                if (gs >= 0) {
                    int prev = p.getInventory().getSelectedSlot();
                    InvUtils.selectHotbar(p, gs);
                    clickBlock(mc, p, anchor);
                    InvUtils.selectHotbar(p, prev);
                    timer = delay.getInt();
                    return;
                }
            }
        }

        if (place.get()) {
            int slot = InvUtils.findHotbar(p, Items.RESPAWN_ANCHOR);
            if (slot < 0) return;
            Player target = nearest(mc, p, r + 1);
            if (target == null) return;
            BlockPos at = target.blockPosition();
            if (!mc.level.getBlockState(at).isAir()) at = at.above();
            if (tryPlace(mc, p, slot, at)) timer = delay.getInt();
        }
    }

    private BlockPos findNearbyAnchor(Minecraft mc, LocalPlayer p, double r) {
        BlockPos o = p.blockPosition();
        int ri = (int) Math.ceil(r);
        BlockPos best = null;
        double bestD = r * r;
        for (int dx = -ri; dx <= ri; dx++)
            for (int dy = -2; dy <= 2; dy++)
                for (int dz = -ri; dz <= ri; dz++) {
                    BlockPos pos = o.offset(dx, dy, dz);
                    if (!mc.level.getBlockState(pos).is(Blocks.RESPAWN_ANCHOR)) continue;
                    double d = p.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
                    if (d < bestD) { bestD = d; best = pos; }
                }
        return best;
    }

    private Player nearest(Minecraft mc, LocalPlayer self, double r) {
        Player best = null;
        double bestD = r * r;
        for (Player pl : mc.level.players()) {
            if (pl == self || !pl.isAlive()) continue;
            double d = self.distanceToSqr(pl);
            if (d < bestD) { bestD = d; best = pl; }
        }
        return best;
    }

    private void clickBlock(Minecraft mc, LocalPlayer p, BlockPos pos) {
        BlockHitResult bhr = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
        try {
            mc.gameMode.useItemOn(p, InteractionHand.MAIN_HAND, bhr);
            p.swing(InteractionHand.MAIN_HAND);
        } catch (Throwable ignored) {}
    }

    private boolean tryPlace(Minecraft mc, LocalPlayer p, int slot, BlockPos pos) {
        for (Direction d : Direction.values()) {
            BlockPos support = pos.relative(d);
            if (mc.level.getBlockState(support).isAir()) continue;
            int prev = p.getInventory().getSelectedSlot();
            InvUtils.selectHotbar(p, slot);
            BlockHitResult bhr = new BlockHitResult(Vec3.atCenterOf(support), d.getOpposite(), support, false);
            try {
                mc.gameMode.useItemOn(p, InteractionHand.MAIN_HAND, bhr);
                p.swing(InteractionHand.MAIN_HAND);
                InvUtils.selectHotbar(p, prev);
                return true;
            } catch (Throwable t) {
                InvUtils.selectHotbar(p, prev);
            }
        }
        return false;
    }
}
