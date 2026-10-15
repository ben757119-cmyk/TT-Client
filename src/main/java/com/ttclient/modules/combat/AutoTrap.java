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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** Place obsidian around / above nearest player (basic trap). */
public class AutoTrap extends Module {
    public final NumberSetting range = addSetting(new NumberSetting("Range", "Target range", 5, 2, 7, 0.5));
    public final NumberSetting delay = addSetting(new NumberSetting("Delay", "Place delay", 1, 0, 10, 1));

    private int timer;

    public AutoTrap() {
        super("AutoTrap", "Obsidian trap nearest player", Category.COMBAT);
    }

    @Override
    public void onTick() {
        Minecraft mc = mc();
        if (mc == null || mc.player == null || mc.level == null || mc.gameMode == null) return;
        if (timer > 0) { timer--; return; }

        LocalPlayer self = mc.player;
        int slot = InvUtils.findHotbar(self, Items.OBSIDIAN);
        if (slot < 0) return;

        Player target = null;
        double best = range.get() * range.get();
        for (Player pl : mc.level.players()) {
            if (pl == self || !pl.isAlive()) continue;
            double d = self.distanceToSqr(pl);
            if (d < best) { best = d; target = pl; }
        }
        if (target == null) return;

        BlockPos feet = target.blockPosition();
        BlockPos[] spots = {
                feet.north(), feet.south(), feet.east(), feet.west(),
                feet.above().north(), feet.above().south(), feet.above().east(), feet.above().west(),
                feet.above(2)
        };

        for (BlockPos pos : spots) {
            if (!mc.level.getBlockState(pos).isAir() && !mc.level.getBlockState(pos).canBeReplaced()) continue;
            if (tryPlace(mc, self, slot, pos)) {
                timer = delay.getInt();
                return;
            }
        }
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
