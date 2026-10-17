package com.ttclient.modules.basefind;

import com.ttclient.client.NotificationManager;
import com.ttclient.modules.Category;
import com.ttclient.modules.Module;
import com.ttclient.settings.BoolSetting;
import com.ttclient.settings.NumberSetting;
import com.ttclient.util.BasefindHits;
import com.ttclient.util.ChunkScan;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ShulkerFinder extends Module {
    public final NumberSetting range = addSetting(new NumberSetting("Range", "Scan range", 80, 32, 160, 8));
    public final NumberSetting interval = addSetting(new NumberSetting("Interval", "Ticks between scans", 25, 10, 100, 5));
    public final BoolSetting tracers = addSetting(new BoolSetting("Tracers", "Draw tracers", true));
    public final BoolSetting notify = addSetting(new BoolSetting("Notify", "Notify on find", true));
    private final List<BasefindHits.Hit> hits = new ArrayList<>();
    private final Set<Long> reported = new HashSet<>();
    private int timer;

    public ShulkerFinder() { super("ShulkerFinder", "Placed shulker boxes (end-game stashes)", Category.BASEFIND); }
    @Override public void onEnable() { timer = 0; reported.clear(); hits.clear(); }

    @Override
    public void onTick() {
        if (++timer < interval.getInt()) return;
        timer = 0;
        hits.clear();
        ChunkScan.forEachBlock(mc(), range.getInt(), range.getInt(), (pos, st) -> {
            if (!isShulker(st)) return;
            hits.add(new BasefindHits.Hit(pos, "Shulker", 0xFFFF55FF));
            if (notify.get() && reported.add(pos.asLong())) {
                try { NotificationManager.push("ShulkerFinder", "Shulker @ " + pos.getX() + " " + pos.getY() + " " + pos.getZ()); } catch (Throwable ignored) {}
            }
        });
    }

    private static boolean isShulker(BlockState st) {
        return st.getBlock().toString().toLowerCase().contains("shulker_box");
    }

    @Override public void onRender2D(GuiGraphicsExtractor g, float pt) { BasefindHits.render(g, hits, tracers.get()); }
}
