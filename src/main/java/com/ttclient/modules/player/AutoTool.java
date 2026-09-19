package com.ttclient.modules.player;

import com.ttclient.modules.Category;
import com.ttclient.modules.Module;
import com.ttclient.settings.BoolSetting;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

public class AutoTool extends Module {
    public final BoolSetting switchBack = addSetting(new BoolSetting("SwitchBack", "Return to previous slot when not mining", false));

    private int previousSlot = -1;

    public AutoTool() {
        super("AutoTool", "Select the best hotbar tool for the block you look at", Category.PLAYER);
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.level == null || mc.hitResult == null) return;
        if (mc.hitResult.getType() != HitResult.Type.BLOCK) {
            if (switchBack.get() && previousSlot >= 0) {
                mc.player.getInventory().setSelectedSlot(previousSlot);
                previousSlot = -1;
            }
            return;
        }
        if (!mc.options.keyAttack.isDown()) return;

        BlockPos pos = ((BlockHitResult) mc.hitResult).getBlockPos();
        BlockState state = mc.level.getBlockState(pos);
        if (state.isAir()) return;

        int bestSlot = -1;
        float bestSpeed = mc.player.getMainHandItem().getDestroySpeed(state);
        var inv = mc.player.getInventory();
        for (int i = 0; i < 9; i++) {
            ItemStack stack = inv.getItem(i);
            if (stack.isEmpty()) continue;
            float speed = stack.getDestroySpeed(state);
            if (speed > bestSpeed) {
                bestSpeed = speed;
                bestSlot = i;
            }
        }
        if (bestSlot >= 0 && bestSlot != inv.getSelectedSlot()) {
            if (previousSlot < 0) previousSlot = inv.getSelectedSlot();
            inv.setSelectedSlot(bestSlot);
        }
    }

    @Override
    public void onDisable() {
        if (switchBack.get() && previousSlot >= 0 && mc.player != null) {
            mc.player.getInventory().setSelectedSlot(previousSlot);
        }
        previousSlot = -1;
    }
}
