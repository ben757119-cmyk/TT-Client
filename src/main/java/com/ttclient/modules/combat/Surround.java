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
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** Place obsidian around your feet (basic surround). */
public class Surround extends Module {
    public final NumberSetting delay = addSetting(new NumberSetting("Delay", "Ticks between places", 1, 0, 10, 1));
    public final BoolSetting center = addSetting(new BoolSetting("OnlyGround", "Only when on ground", true));
    public final BoolSetting disableComplete = addSetting(new BoolSetting("AutoDisable", "Disable when done", true));

    private int timer;
    private static final Direction[] HORIZ = { Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST };

    public Surround() {
        super("Surround", "Obsidian surround around feet", Category.COMBAT);
    }

    @Override
    public void onTick() {
        Minecraft mc = mc();
        if (mc == null || mc.player == null || mc.level == null || mc.gameMode == null) return;
        LocalPlayer p = mc.player;
        if (center.get() && !p.onGround()) return;
        if (timer > 0) { timer--; return; }

        int slot = InvUtils.findHotbar(p, Items.OBSIDIAN);
        if (slot < 0) return;

        BlockPos feet = p.blockPosition();
        boolean placed = false;
        boolean allFilled = true;

        for (Direction d : HORIZ) {
            BlockPos pos = feet.relative(d);
            if (!mc.level.getBlockState(pos).isAir() && !mc.level.getBlockState(pos).canBeReplaced()) continue;
            allFilled = false;
            if (tryPlace(mc, p, slot, pos)) {
                placed = true;
                timer = delay.getInt();
                break;
            }
        }

        // Floor under feet if air (e.g. on edge)
        BlockPos below = feet.below();
        if (mc.level.getBlockState(below).isAir()) {
            allFilled = false;
            if (!placed && tryPlace(mc, p, slot, below)) {
                placed = true;
                timer = delay.getInt();
            }
        }

        if (disableComplete.get() && allFilled && !placed) {
            setEnabled(false);
        }
    }

    private boolean tryPlace(Minecraft mc, LocalPlayer p, int slot, BlockPos pos) {
        // Find a neighbor face to click against
        for (Direction d : Direction.values()) {
            BlockPos support = pos.relative(d);
            if (mc.level.getBlockState(support).isAir()) continue;
            if (mc.level.getBlockState(support).is(Blocks.AIR)) continue;

            int prev = p.getInventory().getSelectedSlot();
            InvUtils.selectHotbar(p, slot);
            Vec3 hit = Vec3.atCenterOf(support);
            BlockHitResult bhr = new BlockHitResult(hit, d.getOpposite(), support, false);
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
