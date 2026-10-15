package com.ttclient.modules.combat;

import com.ttclient.modules.Category;
import com.ttclient.modules.Module;
import com.ttclient.settings.NumberSetting;
import com.ttclient.util.InvUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** Fill 1x1 holes near you with obsidian. */
public class HoleFill extends Module {
    public final NumberSetting range = addSetting(new NumberSetting("Range", "Scan range", 4, 2, 6, 0.5));
    public final NumberSetting delay = addSetting(new NumberSetting("Delay", "Place delay ticks", 2, 0, 10, 1));

    private int timer;

    public HoleFill() {
        super("HoleFill", "Fill nearby holes with obsidian", Category.COMBAT);
    }

    @Override
    public void onTick() {
        Minecraft mc = mc();
        if (mc == null || mc.player == null || mc.level == null || mc.gameMode == null) return;
        if (timer > 0) { timer--; return; }

        LocalPlayer p = mc.player;
        int slot = InvUtils.findHotbar(p, Items.OBSIDIAN);
        if (slot < 0) return;

        int r = (int) Math.ceil(range.get());
        BlockPos origin = p.blockPosition();

        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                for (int dy = -1; dy <= 1; dy++) {
                    BlockPos pos = origin.offset(dx, dy, dz);
                    if (pos.equals(origin)) continue;
                    if (!isHole(mc, pos)) continue;
                    if (tryPlace(mc, p, slot, pos)) {
                        timer = delay.getInt();
                        return;
                    }
                }
            }
        }
    }

    private boolean isHole(Minecraft mc, BlockPos pos) {
        if (!mc.level.getBlockState(pos).isAir()) return false;
        if (!mc.level.getBlockState(pos.above()).isAir()) return false;
        BlockState down = mc.level.getBlockState(pos.below());
        if (down.isAir() || down.canBeReplaced()) return false;
        int solid = 0;
        for (Direction d : new Direction[]{Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST}) {
            BlockState s = mc.level.getBlockState(pos.relative(d));
            if (!s.isAir() && !s.canBeReplaced()) solid++;
        }
        return solid >= 3;
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
