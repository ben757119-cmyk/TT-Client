package com.ttclient.modules.player;

import com.ttclient.modules.Category;
import com.ttclient.modules.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

public class AutoMine extends Module {
    public AutoMine() {
        super("AutoMine", "Hold attack on the block you're looking at", Category.PLAYER);
    }

    @Override
    public void onTick() {
        Minecraft mc = mc();
        if (mc == null || mc.player == null || mc.gameMode == null || mc.options == null) return;
        if (!(mc.hitResult instanceof BlockHitResult bhr)) return;
        if (mc.hitResult.getType() != HitResult.Type.BLOCK) return;
        mc.options.keyAttack.setDown(true);
        mc.gameMode.startDestroyBlock(bhr.getBlockPos(), bhr.getDirection());
        mc.gameMode.continueDestroyBlock(bhr.getBlockPos(), bhr.getDirection());
    }

    @Override
    public void onDisable() {
        Minecraft mc = mc();
        if (mc == null || mc.options == null || mc.gameMode == null) return;
        mc.options.keyAttack.setDown(false);
        mc.gameMode.stopDestroyBlock();
    }
}
