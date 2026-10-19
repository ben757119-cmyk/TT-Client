package com.ttclient.modules.world;

import com.ttclient.client.TTClientClient;
import com.ttclient.modules.BlockListModule;
import com.ttclient.modules.Category;
import com.ttclient.modules.Module;
import com.ttclient.render.EspDraw;
import com.ttclient.render.EspProjection;
import com.ttclient.settings.BoolSetting;
import com.ttclient.settings.NumberSetting;
import com.ttclient.util.ChunkMeshReload;
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

/** Fast progressive ore ESP + optional opacity. */
public class XRay extends Module implements BlockListModule {
    public final BoolSetting opacity = addSetting(new BoolSetting("Opacity", "Hide non-selected (SLOW — leave OFF on Donut)", false));
    public final BoolSetting esp = addSetting(new BoolSetting("ESP", "Boxes on selected ores", true));
    public final BoolSetting tracers = addSetting(new BoolSetting("Tracers", "Lines to ores", false));
    public final BoolSetting fullbright = addSetting(new BoolSetting("Fullbright", "Brighten while on", true));
    public final NumberSetting range = addSetting(new NumberSetting("Range", "ESP range", 48, 16, 80, 8));
    public final NumberSetting maxHits = addSetting(new NumberSetting("MaxHits", "Max boxes", 200, 50, 500, 25));

    private final Set<String> selected = new LinkedHashSet<>();
    private final List<Hit> hits = new ArrayList<>();
    private final List<Hit> building = new ArrayList<>();
    private int scanDx, scanDz;
    private Double oldGamma;
    private boolean lastOpacity;
    public String uiSearch = "";
    public boolean uiSearchFocused;

    public static volatile boolean opacityActiveCache = false;

    public XRay() {
        super("XRay", "Ore ESP + optional opacity", Category.WORLD);
        presetValuables();
    }

    public static XRay get() {
        try {
            var mods = TTClientClient.modules;
            return mods == null ? null : mods.getModule(XRay.class);
        } catch (Throwable t) { return null; }
    }

    public static boolean isOpacityActive() {
        return opacityActiveCache;
    }
    public static boolean shouldHideNonOres() { return opacityActiveCache; }
    public static boolean isOre(BlockState st) {
        if (!opacityActiveCache || st == null) return false;
        XRay x = get();
        return x != null && x.matchesSelected(st);
    }

    @Override public String getUiSearch() { return uiSearch == null ? "" : uiSearch; }
    @Override public void setUiSearch(String s) { uiSearch = s == null ? "" : s; }
    @Override public boolean isUiSearchFocused() { return uiSearchFocused; }
    @Override public void setUiSearchFocused(boolean f) { uiSearchFocused = f; }
    @Override public Set<String> getSelected() { return selected; }
    @Override public String listTitle() { return "ores"; }

    @Override public boolean addBlock(String id) {
        String n = normalize(id);
        if (n == null) return false;
        boolean ok = selected.add(n);
        if (ok && opacity.get()) ChunkMeshReload.request();
        return ok;
    }
    @Override public boolean removeBlock(String id) {
        String n = normalize(id);
        boolean ok = n != null ? selected.remove(n) : selected.remove(id.toLowerCase(Locale.ROOT));
        if (ok && opacity.get()) ChunkMeshReload.request();
        return ok;
    }
    public void loadBlocks(String csv) {
        selected.clear();
        if (csv == null || csv.isBlank()) return;
        for (String p : csv.split(",")) {
            String n = normalize(p.trim());
            if (n != null) selected.add(n);
        }
    }
    public String saveBlocks() { return String.join(",", selected); }

    public void presetValuables() {
        selected.clear();
        for (String s : List.of(
                "minecraft:diamond_ore", "minecraft:deepslate_diamond_ore",
                "minecraft:ancient_debris",
                "minecraft:emerald_ore", "minecraft:deepslate_emerald_ore",
                "minecraft:gold_ore", "minecraft:deepslate_gold_ore", "minecraft:nether_gold_ore",
                "minecraft:iron_ore", "minecraft:deepslate_iron_ore")) selected.add(s);
    }
    public void presetAllOres() {
        selected.clear();
        for (String s : List.of(
                "minecraft:diamond_ore", "minecraft:deepslate_diamond_ore", "minecraft:ancient_debris",
                "minecraft:emerald_ore", "minecraft:deepslate_emerald_ore",
                "minecraft:iron_ore", "minecraft:deepslate_iron_ore",
                "minecraft:gold_ore", "minecraft:deepslate_gold_ore", "minecraft:nether_gold_ore",
                "minecraft:copper_ore", "minecraft:deepslate_copper_ore",
                "minecraft:coal_ore", "minecraft:deepslate_coal_ore",
                "minecraft:redstone_ore", "minecraft:deepslate_redstone_ore",
                "minecraft:lapis_ore", "minecraft:deepslate_lapis_ore",
                "minecraft:nether_quartz_ore")) selected.add(s);
    }

    @Override
    public List<String> searchRegistry(String query, int limit) {
        String q = query == null ? "" : query.toLowerCase(Locale.ROOT).trim();
        List<String> out = new ArrayList<>();
        try {
            for (Block block : BuiltInRegistries.BLOCK) {
                Identifier id;
                try { id = BuiltInRegistries.BLOCK.getKey(block); } catch (Throwable t) { continue; }
                if (id == null) continue;
                String path = id.getPath();
                String full = id.toString();
                boolean oreLike = path.contains("ore") || path.contains("debris") || path.contains("raw_");
                if (!oreLike && q.isEmpty()) continue;
                if (!q.isEmpty() && !path.contains(q) && !full.contains(q)) continue;
                if (selected.contains(full)) continue;
                out.add(full);
                if (out.size() >= limit) break;
            }
        } catch (Throwable ignored) {}
        return out;
    }

    private static String normalize(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String s = raw.trim().toLowerCase(Locale.ROOT);
        if (!s.contains(":")) s = "minecraft:" + s;
        return s;
    }

    private boolean matchesSelected(BlockState st) {
        if (selected.isEmpty()) return false;
        try {
            Identifier id = BuiltInRegistries.BLOCK.getKey(st.getBlock());
            if (id != null && selected.contains(id.toString())) return true;
        } catch (Throwable ignored) {}
        String n = st.getBlock().toString().toLowerCase(Locale.ROOT);
        for (String s : selected) {
            String path = s.contains(":") ? s.substring(s.indexOf(':') + 1) : s;
            if (n.contains(path)) return true;
        }
        return false;
    }

    @Override public void onEnable() {
        hits.clear(); building.clear(); scanDx = scanDz = 0;
        lastOpacity = opacity.get();
        opacityActiveCache = lastOpacity;
        if (fullbright.get()) applyGamma(true);
        if (opacity.get()) ChunkMeshReload.request();
    }
    @Override public void onDisable() {
        hits.clear(); building.clear();
        boolean had = lastOpacity || opacityActiveCache;
        opacityActiveCache = false;
        applyGamma(false);
        if (had) ChunkMeshReload.request();
        lastOpacity = false;
    }

    @Override public void onTick() {
        if (fullbright.get()) applyGamma(true);
        boolean op = opacity.get();
        if (op != lastOpacity) {
            lastOpacity = op;
            opacityActiveCache = op && isEnabled();
            ChunkMeshReload.request();
        }
        if (!esp.get() && !tracers.get()) { hits.clear(); return; }
        progressiveScan();
    }

    private void progressiveScan() {
        Minecraft mc = mc();
        if (mc == null || mc.level == null || mc.player == null) return;
        int r = Math.min(range.getInt(), 80);
        BlockPos o = mc.player.blockPosition();
        int cR = Math.max(1, r >> 4);
        int minY = Math.max(mc.level.getMinY(), o.getY() - r);
        int maxY = Math.min(mc.level.getMaxY(), o.getY() + r);
        double r2 = (double) r * r;
        for (int n = 0; n < 3; n++) {
            int dcx = scanDx - cR;
            int dcz = scanDz - cR;
            LevelChunk chunk = mc.level.getChunkSource().getChunk((o.getX() >> 4) + dcx, (o.getZ() >> 4) + dcz, false);
            if (chunk != null) {
                int x0 = chunk.getPos().getMinBlockX();
                int z0 = chunk.getPos().getMinBlockZ();
                for (int y = minY; y <= maxY; y += 2)
                    for (int x = 0; x < 16; x += 2)
                        for (int z = 0; z < 16; z += 2) {
                            BlockPos pos = new BlockPos(x0 + x, y, z0 + z);
                            if (pos.distSqr(o) > r2) continue;
                            BlockState st = chunk.getBlockState(pos);
                            if (!matchesSelected(st)) continue;
                            if (building.size() < maxHits.getInt())
                                building.add(new Hit(pos.immutable(), colorFor(st), shortLabel(st)));
                        }
            }
            scanDz++;
            if (scanDz > cR * 2) {
                scanDz = 0; scanDx++;
                if (scanDx > cR * 2) {
                    hits.clear(); hits.addAll(building); building.clear();
                    scanDx = 0; scanDz = 0;
                }
            }
        }
    }

    private int colorFor(BlockState st) {
        String n = st.getBlock().toString().toLowerCase(Locale.ROOT);
        if (n.contains("diamond")) return 0x5CDBFF;
        if (n.contains("debris")) return 0xC4A484;
        if (n.contains("emerald")) return 0x17C964;
        if (n.contains("gold")) return 0xFFD700;
        if (n.contains("iron")) return 0xD8D8D8;
        if (n.contains("copper")) return 0xE07A3F;
        if (n.contains("redstone")) return 0xFF3333;
        if (n.contains("lapis")) return 0x3366FF;
        if (n.contains("coal")) return 0x555555;
        if (n.contains("quartz")) return 0xF0F0F0;
        return 0xAAFFAA;
    }
    private String shortLabel(BlockState st) {
        try {
            Identifier id = BuiltInRegistries.BLOCK.getKey(st.getBlock());
            if (id != null) return id.getPath().replace("_ore", "").replace("deepslate_", "ds_");
        } catch (Throwable ignored) {}
        return "ore";
    }

    private void applyGamma(boolean on) {
        try {
            Minecraft mc = mc();
            if (mc == null || mc.options == null) return;
            var gamma = mc.options.gamma();
            if (on) {
                if (oldGamma == null) try { oldGamma = gamma.get(); } catch (Throwable t) { oldGamma = 1.0; }
                try { gamma.set(16.0); } catch (Throwable t) { try { gamma.set(1.0); } catch (Throwable ignored) {} }
            } else if (oldGamma != null) {
                try { gamma.set(oldGamma); } catch (Throwable ignored) {}
                oldGamma = null;
            }
        } catch (Throwable ignored) {}
    }

    @Override
    public void onRender2D(GuiGraphicsExtractor g, float partialTick) {
        if ((!esp.get() && !tracers.get()) || hits.isEmpty()) return;
        Minecraft mc = mc();
        if (mc == null || mc.font == null || mc.getWindow() == null) return;
        int cx = mc.getWindow().getGuiScaledWidth() / 2;
        int cy = mc.getWindow().getGuiScaledHeight() / 2;
        for (Hit h : hits) {
            double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE, maxX = -Double.MAX_VALUE, maxY = -Double.MAX_VALUE;
            int visible = 0;
            for (int i = 0; i < 8; i++) {
                double[] p = new double[2];
                if (!EspProjection.worldToScreen(
                        h.pos.getX() + ((i & 1) == 0 ? 0.02 : 0.98),
                        h.pos.getY() + ((i & 2) == 0 ? 0.02 : 0.98),
                        h.pos.getZ() + ((i & 4) == 0 ? 0.02 : 0.98), p)) continue;
                minX = Math.min(minX, p[0]); minY = Math.min(minY, p[1]);
                maxX = Math.max(maxX, p[0]); maxY = Math.max(maxY, p[1]);
                visible++;
            }
            if (visible < 2) continue;
            if (maxX - minX < 4) { double c = (minX + maxX) * 0.5; minX = c - 2; maxX = c + 2; }
            if (maxY - minY < 4) { double c = (minY + maxY) * 0.5; minY = c - 2; maxY = c + 2; }
            int col = h.color | 0xFF000000;
            if (esp.get()) {
                EspDraw.box(g, minX, minY, maxX, maxY, col, 1);
                EspDraw.label(g, mc.font, h.label, (minX + maxX) * 0.5, minY - 10, col);
            }
            if (tracers.get()) EspDraw.line(g, cx, cy, (minX + maxX) * 0.5, (minY + maxY) * 0.5, col);
        }
    }

    private record Hit(BlockPos pos, int color, String label) {}
}
