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

public class LecternHallFinder extends Module {
    public final NumberSetting minLecterns = addSetting(new NumberSetting("MinLecterns", "Min lecterns in 16x16", 6, 3, 30, 1));
    public final NumberSetting range = addSetting(new NumberSetting("Range", "Scan range", 80, 32, 160, 8));
    public final NumberSetting interval = addSetting(new NumberSetting("Interval", "Ticks between scans", 30, 10, 100, 5));
    public final BoolSetting tracers = addSetting(new BoolSetting("Tracers", "Draw tracers", true));
    public final BoolSetting notify = addSetting(new BoolSetting("Notify", "Notify on find", true));
    private final List<BasefindHits.Hit> hits = new ArrayList<>();
    private final Set<Long> reported = new HashSet<>();
    private int timer;

    public LecternHallFinder() { super("LecternHall", "Dense lecterns (villager trading halls)", Category.BASEFIND); }
    @Override public void onEnable() { timer = 0; reported.clear(); hits.clear(); }

    @Override
    public void onTick() {
        if (++timer < interval.getInt()) return;
        timer = 0;
        hits.clear();
        Map<Long, Integer> cells = new HashMap<>();
        Map<Long, BlockPos> sample = new HashMap<>();
        ChunkScan.forEachBlock(mc(), range.getInt(), 40, (pos, st) -> {
            if (!isLectern(st)) return;
            long cell = (((long) (pos.getX() >> 4)) << 32) ^ ((pos.getZ() >> 4) & 0xffffffffL);
            cells.merge(cell, 1, Integer::sum);
            sample.putIfAbsent(cell, pos.immutable());
        });
        int need = minLecterns.getInt();
        for (var e : cells.entrySet()) {
            if (e.getValue() < need) continue;
            BlockPos pos = sample.get(e.getKey());
            hits.add(new BasefindHits.Hit(pos, "Hall x" + e.getValue(), 0xFF8B5A2B));
            if (notify.get() && reported.add(e.getKey())) {
                try { NotificationManager.push("LecternHall", e.getValue() + " lecterns @ " + pos.getX() + " " + pos.getY() + " " + pos.getZ()); } catch (Throwable ignored) {}
            }
        }
    }

    private static boolean isLectern(BlockState st) {
        try { return st.is(Blocks.LECTERN); }
        catch (Throwable t) { return st.getBlock().toString().toLowerCase().contains("lectern"); }
    }

    @Override public void onRender2D(GuiGraphicsExtractor g, float pt) { BasefindHits.render(g, hits, tracers.get()); }
}
