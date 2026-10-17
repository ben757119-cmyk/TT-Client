package com.ttclient.util;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.function.BiConsumer;

/**
 * Lightweight block iteration for basefind.
 * Hard limits prevent freezes when several scanners run after joining a server.
 */
public final class ChunkScan {
    /** Max blocks inspected per scan call (hard budget). */
    public static final int DEFAULT_BUDGET = 25_000;
    /** Skip scanning until the local player has been alive this many ticks. */
    public static final int JOIN_GRACE_TICKS = 80;

    private static long lastScanNanos;

    private ChunkScan() {}

    public static boolean canScan(Minecraft mc) {
        if (mc == null || mc.level == null || mc.player == null) return false;
        try {
            // Avoid hammering during chunk flood on login
            if (mc.player.tickCount < JOIN_GRACE_TICKS) return false;
        } catch (Throwable ignored) {}
        // Stagger scanners — at most one full scan every 50ms
        long now = System.nanoTime();
        if (now - lastScanNanos < 50_000_000L) return false;
        lastScanNanos = now;
        return true;
    }

    public static void forEachBlock(Minecraft mc, int range, int yPad, BiConsumer<BlockPos, BlockState> consumer) {
        forEachBlock(mc, range, yPad, 2, DEFAULT_BUDGET, consumer);
    }

    /**
     * @param step sample every N blocks (2 = 1/8 volume, 3 = ~1/27)
     * @param budget max blocks to read this call
     */
    public static void forEachBlock(Minecraft mc, int range, int yPad, int step, int budget,
                                    BiConsumer<BlockPos, BlockState> consumer) {
        if (!canScan(mc)) return;
        if (step < 1) step = 1;
        if (budget < 1000) budget = 1000;
        // Cap range so a mis-set slider cannot melt the client
        if (range > 96) range = 96;
        if (yPad > 96) yPad = 96;

        BlockPos o = mc.player.blockPosition();
        int cR = (range >> 4) + 1;
        int cx0 = o.getX() >> 4;
        int cz0 = o.getZ() >> 4;
        int minY = Math.max(mc.level.getMinY(), o.getY() - yPad);
        int maxY = Math.min(mc.level.getMaxY(), o.getY() + yPad);
        double r2 = (double) range * range;

        int checked = 0;
        for (int dcx = -cR; dcx <= cR; dcx++) {
            for (int dcz = -cR; dcz <= cR; dcz++) {
                LevelChunk chunk = mc.level.getChunkSource().getChunk(cx0 + dcx, cz0 + dcz, false);
                if (chunk == null) continue;
                int x0 = chunk.getPos().getMinBlockX();
                int z0 = chunk.getPos().getMinBlockZ();
                for (int y = minY; y <= maxY; y += step) {
                    for (int x = 0; x < 16; x += step) {
                        for (int z = 0; z < 16; z += step) {
                            if (++checked > budget) return;
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
