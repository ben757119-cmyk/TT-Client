package com.ttclient.modules.player;

import com.ttclient.modules.Category;
import com.ttclient.modules.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

public class AutoTool extends Module {
    public AutoTool() {
        super("AutoTool", "Switch to best tool for the block", Category.PLAYER);
    }

    @Override
    public void onTick() {
        Minecraft mc = mc();
        if (mc == null || mc.player == null || mc.level == null || mc.options == null) return;
        if (!mc.options.keyAttack.isDown()) return;
        if (!(mc.hitResult instanceof BlockHitResult bhr) || mc.hitResult.getType() != HitResult.Type.BLOCK) return;

        LocalPlayer p = mc.player;
        BlockPos pos = bhr.getBlockPos();
        BlockState state = mc.level.getBlockState(pos);
        int best = -1;
        float bestSpeed = 1f;
        for (int i = 0; i < 9; i++) {
            ItemStack stack = p.getInventory().getItem(i);
            if (stack.isEmpty()) continue;
            float speed = stack.getDestroySpeed(state);
            if (speed > bestSpeed) {
                bestSpeed = speed;
                best = i;
            }
        }
        if (best >= 0) {
            p.getInventory().setSelectedSlot(best);
        }
    }
}
