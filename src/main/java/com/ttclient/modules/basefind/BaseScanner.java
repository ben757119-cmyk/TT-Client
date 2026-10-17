package com.ttclient.modules.basefind;

import com.ttclient.client.NotificationManager;
import com.ttclient.modules.Category;
import com.ttclient.modules.Module;
import com.ttclient.settings.BoolSetting;
import com.ttclient.settings.NumberSetting;
import com.ttclient.util.BasefindHits;
import com.ttclient.util.ChunkScan;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class BaseScanner extends Module {
    public final NumberSetting minScore = addSetting(new NumberSetting("MinScore", "Minimum base score to report", 25, 10, 100, 5));
    public final NumberSetting range = addSetting(new NumberSetting("Range", "Scan range", 96, 32, 160, 8));
    public final NumberSetting interval = addSetting(new NumberSetting("Interval", "Ticks between scans", 40, 15, 120, 5));
    public final BoolSetting tracers = addSetting(new BoolSetting("Tracers", "Draw tracers", true));
    public final BoolSetting notify = addSetting(new BoolSetting("Notify", "Notify on find", true));
    private final List<BasefindHits.Hit> hits = new ArrayList<>();
    private final Set<Long> reported = new HashSet<>();
    private int timer;

    public BaseScanner() { super("BaseScanner", "Combined base score (storage/farms/utility)", Category.BASEFIND); }
    @Override public void onEnable() { timer = 0; reported.clear(); hits.clear(); }

    @Override
    public void onTick() {
        if (++timer < interval.getInt()) return;
        timer = 0;
        hits.clear();
        Map<Long, Integer> score = new HashMap<>();
        Map<Long, BlockPos> sample = new HashMap<>();
        ChunkScan.forEachBlock(mc(), range.getInt(), range.getInt(), (pos, st) -> {
            int s = scoreBlock(st);
            if (s <= 0) return;
            long cell = (((long) (pos.getX() >> 4)) << 32) ^ ((pos.getZ() >> 4) & 0xffffffffL);
            score.merge(cell, s, Integer::sum);
            sample.putIfAbsent(cell, pos.immutable());
        });
        int need = minScore.getInt();
        for (var e : score.entrySet()) {
            if (e.getValue() < need) continue;
            BlockPos pos = sample.get(e.getKey());
            hits.add(new BasefindHits.Hit(pos, "Base " + e.getValue(), 0xFFFF3333));
            if (notify.get() && reported.add(e.getKey())) {
                try { NotificationManager.push("BaseScanner", "Score " + e.getValue() + " @ " + pos.getX() + " " + pos.getY() + " " + pos.getZ()); } catch (Throwable ignored) {}
            }
        }
    }

    private static int scoreBlock(BlockState st) {
        try {
            if (st.is(Blocks.CHEST) || st.is(Blocks.TRAPPED_CHEST)) return 3;
            if (st.is(Blocks.BARREL)) return 3;
            if (st.is(Blocks.ENDER_CHEST)) return 4;
            if (st.is(Blocks.HOPPER)) return 2;
            if (st.is(Blocks.FURNACE) || st.is(Blocks.BLAST_FURNACE) || st.is(Blocks.SMOKER)) return 2;
            if (st.is(Blocks.CRAFTING_TABLE)) return 1;
            if (st.is(Blocks.ENCHANTING_TABLE)) return 5;
            if (st.is(Blocks.ANVIL) || st.is(Blocks.CHIPPED_ANVIL) || st.is(Blocks.DAMAGED_ANVIL)) return 3;
            if (st.is(Blocks.BEACON)) return 15;
            if (st.is(Blocks.SPAWNER)) return 12;
            if (st.is(Blocks.LECTERN)) return 2;
            if (st.is(Blocks.BREWING_STAND)) return 3;
            if (st.is(Blocks.LODESTONE)) return 6;
            if (st.is(Blocks.RESPAWN_ANCHOR)) return 5;
            if (st.is(Blocks.NETHER_PORTAL)) return 2;
        } catch (Throwable ignored) {}
        String n = st.getBlock().toString().toLowerCase();
        if (n.contains("shulker_box")) return 6;
        if (n.contains("bed")) return 4;
        if (n.contains("chest")) return 3;
        if (n.contains("hopper")) return 2;
        if (n.contains("furnace") || n.contains("smoker")) return 2;
        return 0;
    }

    @Override public void onRender2D(GuiGraphicsExtractor g, float pt) { BasefindHits.render(g, hits, tracers.get()); }
}
