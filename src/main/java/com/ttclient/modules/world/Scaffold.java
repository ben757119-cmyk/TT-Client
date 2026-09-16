package com.ttclient.modules.world;

import com.ttclient.modules.Category;
import com.ttclient.modules.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public class Scaffold extends Module {
    public Scaffold() {
        super("Scaffold", "Place blocks under your feet", Category.WORLD);
    }

    @Override
    public void onTick() {
        Minecraft mc = mc();
        if (mc == null || mc.player == null || mc.level == null || mc.gameMode == null) return;
        LocalPlayer p = mc.player;
        BlockPos below = p.blockPosition().below();
        if (!mc.level.getBlockState(below).isAir()) return;

        int slot = -1;
        for (int i = 0; i < 9; i++) {
            ItemStack s = p.getInventory().getItem(i);
            if (!s.isEmpty() && s.getItem() instanceof BlockItem) {
                slot = i;
                break;
            }
        }
        if (slot < 0) return;
        p.getInventory().setSelectedSlot(slot);

        BlockHitResult hit = new BlockHitResult(
                Vec3.atCenterOf(below),
                Direction.UP,
                below,
                false
        );
        mc.gameMode.useItemOn(p, InteractionHand.MAIN_HAND, hit);
    }
}
