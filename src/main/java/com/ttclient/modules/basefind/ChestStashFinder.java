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

public class ChestStashFinder extends Module {
    public final NumberSetting minChests = addSetting(new NumberSetting(
            "MinChests", "Min storage blocks in 16x16", 8, 3, 40, 1));
    public final NumberSetting range = addSetting(new NumberSetting("Range", "Scan range", 80, 32, 160, 8));
    public final NumberSetting interval = addSetting(new NumberSetting("Interval", "Ticks between scans", 25, 10, 100, 5));
    public final BoolSetting barrels = addSetting(new BoolSetting("Barrels", "Count barrels", true));
    public final BoolSetting ender = addSetting(new BoolSetting("EnderChests", "Count ender chests", true));
    public final BoolSetting tracers = addSetting(new BoolSetting("Tracers", "Draw tracers", true));
    public final BoolSetting notify = addSetting(new BoolSetting("Notify", "Notify on new find", true));

    private final List<BasefindHits.Hit> hits = new ArrayList<>();
    private final Set<Long> reported = new HashSet<>();
    private int timer;

    public ChestStashFinder() {
        super("ChestStash", "Dense chests / barrels (stashes)", Category.BASEFIND);
    }

    @Override public void onEnable() { timer = 0; reported.clear(); hits.clear(); }

    @Override
    public void onTick() {
        if (++timer < interval.getInt()) return;
        timer = 0;
        hits.clear();
        Map<Long, Integer> cells = new HashMap<>();
        Map<Long, BlockPos> sample = new HashMap<>();
        ChunkScan.forEachBlock(mc(), range.getInt(), range.getInt(), (pos, st) -> {
            if (!isStorage(st)) return;
            long cell = (((long) (pos.getX() >> 4)) << 32) ^ ((pos.getZ() >> 4) & 0xffffffffL);
            cells.merge(cell, 1, Integer::sum);
            sample.putIfAbsent(cell, pos.immutable());
        });
        int need = minChests.getInt();
        for (var e : cells.entrySet()) {
            if (e.getValue() < need) continue;
            BlockPos pos = sample.get(e.getKey());
            hits.add(new BasefindHits.Hit(pos, "Stash x" + e.getValue(), 0xFFFFAA00));
            if (notify.get() && reported.add(e.getKey())) {
                try { NotificationManager.push("ChestStash", e.getValue() + " storage @ "
                        + pos.getX() + " " + pos.getY() + " " + pos.getZ()); } catch (Throwable ignored) {}
            }
        }
    }

    private boolean isStorage(BlockState st) {
        try {
            if (st.is(Blocks.CHEST) || st.is(Blocks.TRAPPED_CHEST)) return true;
            if (barrels.get() && st.is(Blocks.BARREL)) return true;
            if (ender.get() && st.is(Blocks.ENDER_CHEST)) return true;
        } catch (Throwable ignored) {}
        String n = st.getBlock().toString().toLowerCase();
        if (n.contains("chest") && !n.contains("ender")) return true;
        if (barrels.get() && n.contains("barrel")) return true;
        return ender.get() && n.contains("ender_chest");
    }

    @Override public void onRender2D(GuiGraphicsExtractor g, float partialTick) {
        BasefindHits.render(g, hits, tracers.get());
    }
}
