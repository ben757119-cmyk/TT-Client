package com.ttclient.modules.render;

import com.ttclient.modules.BlockListModule;
import com.ttclient.modules.Category;
import com.ttclient.modules.Module;
import com.ttclient.render.EspDraw;
import com.ttclient.render.EspProjection;
import com.ttclient.settings.ColorSetting;
import com.ttclient.settings.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class BlockESP extends Module implements BlockListModule {
    public final NumberSetting range = addSetting(new NumberSetting("Range", "Scan range", 48, 16, 96, 8));
    public final ColorSetting color = addSetting(new ColorSetting("Color", "ESP color", 0xFF00E8A0));
    private final Set<String> selected = new LinkedHashSet<>();
    private final List<BlockPos> hits = new ArrayList<>();
    private int tick;
    public String uiSearch = "";
    public boolean uiSearchFocused = false;

    public BlockESP() {
        super("BlockESP", "ESP for custom blocks (.b add <id>)", Category.RENDER);
        selected.add("minecraft:spawner");
        selected.add("minecraft:chest");
    }

    @Override public Set<String> getSelected() { return selected; }
    @Override public String getUiSearch() { return uiSearch == null ? "" : uiSearch; }
    @Override public void setUiSearch(String s) { uiSearch = s == null ? "" : s; }
    @Override public boolean isUiSearchFocused() { return uiSearchFocused; }
    @Override public void setUiSearchFocused(boolean focused) { uiSearchFocused = focused; }
    @Override public String listTitle() { return "blocks"; }

    @Override public boolean addBlock(String id) {
        String norm = normalize(id);
        return norm != null && selected.add(norm);
    }
    @Override public boolean removeBlock(String id) {
        String norm = normalize(id);
        if (norm == null) return selected.remove(id.toLowerCase(Locale.ROOT));
        return selected.remove(norm);
    }
    public void clearBlocks() { selected.clear(); }
    public void loadBlocks(String csv) {
        selected.clear();
        if (csv == null || csv.isBlank()) return;
        for (String p : csv.split(",")) {
            String n = normalize(p.trim());
            if (n != null) selected.add(n);
        }
    }
    public String saveBlocks() { return String.join(",", selected); }

    @Override
    public List<String> searchRegistry(String query, int limit) {
        List<String> out = new ArrayList<>();
        String q = query == null ? "" : query.toLowerCase(Locale.ROOT).trim();
        try {
            for (Block block : BuiltInRegistries.BLOCK) {
                Identifier id = BuiltInRegistries.BLOCK.getKey(block);
                if (id == null) continue;
                String s = id.toString();
                if (q.isEmpty() || s.contains(q) || id.getPath().contains(q)) {
                    if (!selected.contains(s)) {
                        out.add(s);
                        if (out.size() >= limit) break;
                    }
                }
            }
        } catch (Throwable ignored) {}
        return out;
    }

    private static String normalize(String id) {
        if (id == null || id.isBlank()) return null;
        id = id.trim().toLowerCase(Locale.ROOT);
        if (!id.contains(":")) id = "minecraft:" + id;
        return id;
    }

    @Override
    public void onTick() {
        if (selected.isEmpty()) { hits.clear(); return; }
        if (++tick % 15 != 0) return;
        rescan();
    }

    private void rescan() {
        hits.clear();
        Minecraft mc = mc();
        if (mc == null || mc.level == null || mc.player == null) return;
        int r = range.getInt();
        BlockPos o = mc.player.blockPosition();
        int cR = (r >> 4) + 1;
        int cx0 = o.getX() >> 4, cz0 = o.getZ() >> 4;
        Set<String> wanted = new LinkedHashSet<>(selected);
        for (int dcx = -cR; dcx <= cR; dcx++) {
            for (int dcz = -cR; dcz <= cR; dcz++) {
                LevelChunk chunk = mc.level.getChunkSource().getChunk(cx0 + dcx, cz0 + dcz, false);
                if (chunk == null) continue;
                int x0 = chunk.getPos().getMinBlockX();
                int z0 = chunk.getPos().getMinBlockZ();
                int minY = Math.max(mc.level.getMinY(), o.getY() - r);
                int maxY = Math.min(mc.level.getMaxY(), o.getY() + r);
                for (int y = minY; y <= maxY; y += 2)
                    for (int x = 0; x < 16; x += 2)
                        for (int z = 0; z < 16; z += 2) {
                            BlockPos pos = new BlockPos(x0 + x, y, z0 + z);
                            if (pos.distSqr(o) > (double) r * r) continue;
                            BlockState st = chunk.getBlockState(pos);
                            String n = st.getBlock().toString().toLowerCase(Locale.ROOT);
                            for (String s : wanted) {
                                String path = s.contains(":") ? s.substring(s.indexOf(':') + 1) : s;
                                if (n.contains(path)) { hits.add(pos); break; }
                            }
                        }
            }
        }
    }

    @Override
    public void onRender2D(GuiGraphicsExtractor g, float partialTick) {
        Minecraft mc = mc();
        if (mc == null || mc.font == null) return;
        int col = color.get();
        if (((col >> 24) & 0xFF) == 0) col |= 0xFF000000;
        for (BlockPos pos : hits) {
            double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE, maxX = -Double.MAX_VALUE, maxY = -Double.MAX_VALUE;
            int visible = 0;
            for (int i = 0; i < 8; i++) {
                double[] p = new double[2];
                if (!EspProjection.worldToScreen(pos.getX()+((i&1)==0?0.05:0.95), pos.getY()+((i&2)==0?0.05:0.95), pos.getZ()+((i&4)==0?0.05:0.95), p)) continue;
                minX = Math.min(minX,p[0]); minY = Math.min(minY,p[1]);
                maxX = Math.max(maxX,p[0]); maxY = Math.max(maxY,p[1]);
                visible++;
            }
            if (visible < 2) continue;
            if (maxX-minX < 4) { double c=(minX+maxX)*0.5; minX=c-2; maxX=c+2; }
            if (maxY-minY < 4) { double c=(minY+maxY)*0.5; minY=c-2; maxY=c+2; }
            EspDraw.box(g, minX, minY, maxX, maxY, col, 1);
        }
    }
}
