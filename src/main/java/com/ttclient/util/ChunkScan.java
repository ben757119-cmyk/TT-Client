package com.ttclient.util;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.function.BiConsumer;

/** Iterate block states in loaded chunks around the player. */
public final class ChunkScan {
    private ChunkScan() {}

    public static void forEachBlock(Minecraft mc, int range, int yPad, BiConsumer<BlockPos, BlockState> consumer) {
        if (mc == null || mc.level == null || mc.player == null) return;
        BlockPos o = mc.player.blockPosition();
        int cR = (range >> 4) + 1;
        int cx0 = o.getX() >> 4;
        int cz0 = o.getZ() >> 4;
        int minY = Math.max(mc.level.getMinY(), o.getY() - yPad);
        int maxY = Math.min(mc.level.getMaxY(), o.getY() + yPad);
        double r2 = (double) range * range;

        for (int dcx = -cR; dcx <= cR; dcx++) {
            for (int dcz = -cR; dcz <= cR; dcz++) {
                LevelChunk chunk = mc.level.getChunkSource().getChunk(cx0 + dcx, cz0 + dcz, false);
                if (chunk == null) continue;
                int x0 = chunk.getPos().getMinBlockX();
                int z0 = chunk.getPos().getMinBlockZ();
                for (int y = minY; y <= maxY; y++) {
                    for (int x = 0; x < 16; x++) {
                        for (int z = 0; z < 16; z++) {
                            BlockPos pos = new BlockPos(x0 + x, y, z0 + z);
                            if (pos.distSqr(o) > r2) continue;
                            consumer.accept(pos, chunk.getBlockState(pos));
                        }
                    }
                }
            }
        }
    }
}
