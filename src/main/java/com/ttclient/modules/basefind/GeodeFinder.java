package com.ttclient.modules.basefind;

import com.ttclient.client.NotificationManager;
import com.ttclient.modules.Category;
import com.ttclient.modules.Module;
import com.ttclient.settings.BoolSetting;
import com.ttclient.settings.NumberSetting;
import com.ttclient.util.BasefindHits;
import com.ttclient.util.ChunkScan;
import net.minecraft.client.Minecraft;
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

public class GeodeFinder extends Module {
    public final NumberSetting minClusters = addSetting(new NumberSetting(
            "MinClusters", "Min amethyst blocks in cell", 12, 4, 40, 1));
    public final NumberSetting range = addSetting(new NumberSetting("Range", "Scan range", 96, 32, 160, 8));
    public final NumberSetting interval = addSetting(new NumberSetting("Interval", "Ticks between scans", 35, 10, 100, 5));
    public final BoolSetting tracers = addSetting(new BoolSetting("Tracers", "Draw tracers", true));
    public final BoolSetting notify = addSetting(new BoolSetting("Notify", "Notify on new find", true));
    public final BoolSetting onlyGrown = addSetting(new BoolSetting("OnlyGrown", "Prefer full AMETHYST_CLUSTER", false));

    private final List<BasefindHits.Hit> hits = new ArrayList<>();
    private final Set<Long> reported = new HashSet<>();
    private int timer;

    public GeodeFinder() {
        super("GeodeFinder", "Amethyst geode / grown cluster density", Category.BASEFIND);
    }

    @Override public void onEnable() { timer = 0; reported.clear(); hits.clear(); }

    @Override
    public void onTick() {
        if (++timer < interval.getInt()) return;
        timer = 0;
        hits.clear();
        Minecraft mc = mc();
        if (mc == null || mc.level == null) return;
        Map<Long, Integer> cellCount = new HashMap<>();
        Map<Long, BlockPos> cellSample = new HashMap<>();
        ChunkScan.forEachBlock(mc, range.getInt(), 48, (pos, st) -> {
            if (!isAmethyst(st)) return;
            if (onlyGrown.get() && !isFullCluster(st)) return;
            long cell = (((long) (pos.getX() >> 4) & 0xfffff) << 40)
                    | (((long) (pos.getZ() >> 4) & 0xfffff) << 20)
                    | ((long) (pos.getY() >> 4) & 0xfffff);
            cellCount.merge(cell, 1, Integer::sum);
            cellSample.putIfAbsent(cell, pos.immutable());
        });
        int need = minClusters.getInt();
        for (var e : cellCount.entrySet()) {
            if (e.getValue() < need) continue;
            BlockPos pos = cellSample.get(e.getKey());
            hits.add(new BasefindHits.Hit(pos, "Geode x" + e.getValue(), 0xFFCC66FF));
            if (notify.get() && reported.add(e.getKey())) {
                try { NotificationManager.push("GeodeFinder", "Amethyst density " + e.getValue()
                        + " @ " + pos.getX() + " " + pos.getY() + " " + pos.getZ()); } catch (Throwable ignored) {}
            }
        }
    }

    private static boolean isFullCluster(BlockState st) {
        try { return st.is(Blocks.AMETHYST_CLUSTER); }
        catch (Throwable t) { return st.getBlock().toString().toLowerCase().contains("amethyst_cluster"); }
    }

    private static boolean isAmethyst(BlockState st) {
        try {
            return st.is(Blocks.AMETHYST_CLUSTER) || st.is(Blocks.LARGE_AMETHYST_BUD)
                    || st.is(Blocks.MEDIUM_AMETHYST_BUD) || st.is(Blocks.SMALL_AMETHYST_BUD)
                    || st.is(Blocks.BUDDING_AMETHYST) || st.is(Blocks.AMETHYST_BLOCK);
        } catch (Throwable t) {
            return st.getBlock().toString().toLowerCase().contains("amethyst");
        }
    }

    @Override public void onRender2D(GuiGraphicsExtractor g, float partialTick) {
        BasefindHits.render(g, hits, tracers.get());
    }
}
