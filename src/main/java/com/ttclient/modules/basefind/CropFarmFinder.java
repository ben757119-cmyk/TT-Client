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
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CropFarmFinder extends Module {
    public final NumberSetting minCrops = addSetting(new NumberSetting("MinGrown", "Min grown crops in 16x16", 24, 8, 80, 2));
    public final NumberSetting range = addSetting(new NumberSetting("Range", "Scan range", 80, 32, 160, 8));
    public final NumberSetting interval = addSetting(new NumberSetting("Interval", "Ticks between scans", 30, 10, 100, 5));
    public final BoolSetting tracers = addSetting(new BoolSetting("Tracers", "Draw tracers", true));
    public final BoolSetting notify = addSetting(new BoolSetting("Notify", "Notify on find", true));
    private final List<BasefindHits.Hit> hits = new ArrayList<>();
    private final Set<Long> reported = new HashSet<>();
    private int timer;

    public CropFarmFinder() { super("CropFarm", "Dense fully-grown crops (auto farms)", Category.BASEFIND); }
    @Override public void onEnable() { timer = 0; reported.clear(); hits.clear(); }

    @Override
    public void onTick() {
        if (++timer < interval.getInt()) return;
        timer = 0;
        hits.clear();
        Map<Long, Integer> cells = new HashMap<>();
        Map<Long, BlockPos> sample = new HashMap<>();
        ChunkScan.forEachBlock(mc(), range.getInt(), 24, (pos, st) -> {
            if (!isGrownCrop(st)) return;
            long cell = (((long) (pos.getX() >> 4)) << 32) ^ ((pos.getZ() >> 4) & 0xffffffffL);
            cells.merge(cell, 1, Integer::sum);
            sample.putIfAbsent(cell, pos.immutable());
        });
        int need = minCrops.getInt();
        for (var e : cells.entrySet()) {
            if (e.getValue() < need) continue;
            BlockPos pos = sample.get(e.getKey());
            hits.add(new BasefindHits.Hit(pos, "Farm x" + e.getValue(), 0xFF66FF33));
            if (notify.get() && reported.add(e.getKey())) {
                try { NotificationManager.push("CropFarm", e.getValue() + " grown crops @ " + pos.getX() + " " + pos.getY() + " " + pos.getZ()); } catch (Throwable ignored) {}
            }
        }
    }

    private static boolean isGrownCrop(BlockState st) {
        try {
            if (st.getBlock() instanceof CropBlock crop) return crop.isMaxAge(st);
        } catch (Throwable ignored) {}
        String n = st.getBlock().toString().toLowerCase();
        if (n.contains("wheat") || n.contains("carrots") || n.contains("potatoes")
                || n.contains("beetroots") || n.contains("nether_wart")) {
            try {
                for (var p : st.getProperties()) {
                    if (p.getName().equals("age")) {
                        Comparable<?> v = st.getValue(p);
                        if (v instanceof Integer i) return i >= 3;
                    }
                }
            } catch (Throwable ignored) {}
        }
        try { return st.is(Blocks.NETHER_WART); } catch (Throwable ignored) {}
        return false;
    }

    @Override public void onRender2D(GuiGraphicsExtractor g, float pt) { BasefindHits.render(g, hits, tracers.get()); }
}
