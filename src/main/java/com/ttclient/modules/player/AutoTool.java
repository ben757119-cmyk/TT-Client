package com.ttclient.modules.player;

import com.ttclient.modules.Category;
import com.ttclient.modules.Module;
import com.ttclient.settings.BoolSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

public class AutoTool extends Module {
    public final BoolSetting switchBack = addSetting(new BoolSetting(
            "SwitchBack", "Return to previous slot when not mining", false));

    private int previousSlot = -1;

    public AutoTool() {
        super("AutoTool", "Select the best hotbar tool for the block you look at", Category.PLAYER);
    }

    @Override
    public void onEnable() {
        previousSlot = -1;
    }

    @Override
    public void onDisable() {
        restorePreviousSlot();
        previousSlot = -1;
    }

    @Override
    public void onTick() {
        // Defense in depth — ModuleManager also gates on isEnabled()
        if (!isEnabled()) {
            previousSlot = -1;
            return;
        }

        Minecraft mc = mc();
        if (mc == null || mc.player == null || mc.level == null || mc.hitResult == null || mc.options == null) {
            return;
        }

        // Only while holding attack on a block
        if (!mc.options.keyAttack.isDown()) {
            if (switchBack.get()) restorePreviousSlot();
            return;
        }

        if (mc.hitResult.getType() != HitResult.Type.BLOCK || !(mc.hitResult instanceof BlockHitResult bhr)) {
            if (switchBack.get()) restorePreviousSlot();
            return;
        }

        BlockPos pos = bhr.getBlockPos();
        BlockState state = mc.level.getBlockState(pos);
        if (state.isAir()) return;

        var inv = mc.player.getInventory();
        int current = inv.getSelectedSlot();
        float bestSpeed = inv.getItem(current).getDestroySpeed(state);
        int bestSlot = current;

        for (int i = 0; i < 9; i++) {
            ItemStack stack = inv.getItem(i);
            if (stack.isEmpty()) continue;
            float speed = stack.getDestroySpeed(state);
            if (speed > bestSpeed) {
                bestSpeed = speed;
                bestSlot = i;
            }
        }

        if (bestSlot != current) {
            if (previousSlot < 0) previousSlot = current;
            inv.setSelectedSlot(bestSlot);
        }
    }

    private void restorePreviousSlot() {
        if (previousSlot < 0) return;
        Minecraft mc = mc();
        if (mc == null || mc.player == null) {
            previousSlot = -1;
            return;
        }
        try {
            mc.player.getInventory().setSelectedSlot(previousSlot);
        } catch (Throwable ignored) {}
        previousSlot = -1;
    }
}
