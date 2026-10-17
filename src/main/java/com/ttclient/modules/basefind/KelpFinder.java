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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class KelpFinder extends Module {
    public final NumberSetting minHeight = addSetting(new NumberSetting(
            "MinHeight", "Minimum kelp column height", 18, 8, 25, 1));
    public final NumberSetting range = addSetting(new NumberSetting(
            "Range", "Scan range", 96, 32, 160, 8));
    public final NumberSetting interval = addSetting(new NumberSetting(
            "Interval", "Ticks between scans", 30, 10, 100, 5));
    public final BoolSetting tracers = addSetting(new BoolSetting("Tracers", "Draw tracers", true));
    public final BoolSetting notify = addSetting(new BoolSetting("Notify", "Notify on new find", true));

    private final List<BasefindHits.Hit> hits = new ArrayList<>();
    private final Set<Long> reported = new HashSet<>();
    private int timer;

    public KelpFinder() {
        super("KelpFinder", "Tall kelp columns (AFK farms / bases)", Category.BASEFIND);
    }

    @Override public void onEnable() { timer = 0; reported.clear(); hits.clear(); }

    @Override
    public void onTick() {
        if (++timer < interval.getInt()) return;
        timer = 0;
        rescan();
    }

    private void rescan() {
        hits.clear();
        Minecraft mc = mc();
        if (mc == null || mc.level == null) return;
        int need = minHeight.getInt();
        Set<Long> measured = new HashSet<>();
        ChunkScan.forEachBlock(mc, range.getInt(), range.getInt(), (pos, st) -> {
            if (!isKelp(st)) return;
            if (isKelp(mc.level.getBlockState(pos.below()))) return;
            long key = (((long) pos.getX()) << 32) ^ (pos.getZ() & 0xffffffffL);
            if (!measured.add(key)) return;
            int h = 0;
            BlockPos p = pos;
            while (h < 32 && isKelp(mc.level.getBlockState(p))) { h++; p = p.above(); }
            if (h >= need) {
                hits.add(new BasefindHits.Hit(pos, "Kelp x" + h, 0xFF33FF66));
                if (notify.get() && reported.add(pos.asLong())) {
                    try { NotificationManager.push("KelpFinder", "Tall kelp (" + h + ") @ "
                            + pos.getX() + " " + pos.getY() + " " + pos.getZ()); } catch (Throwable ignored) {}
                }
            }
        });
    }

    private static boolean isKelp(BlockState st) {
        try { return st.is(Blocks.KELP) || st.is(Blocks.KELP_PLANT); }
        catch (Throwable t) { return st.getBlock().toString().toLowerCase().contains("kelp"); }
    }

    @Override public void onRender2D(GuiGraphicsExtractor g, float partialTick) {
        BasefindHits.render(g, hits, tracers.get());
    }
}
