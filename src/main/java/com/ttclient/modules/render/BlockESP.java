package com.ttclient.modules.render;

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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Custom block ESP — add/remove blocks (ClickGUI search or chat: ttblock add/remove).
 */
public class BlockESP extends Module {
    public final NumberSetting range = addSetting(new NumberSetting("Range", "Scan range", 48, 16, 96, 8));
    public final ColorSetting color = addSetting(new ColorSetting("Color", "ESP color", 0xFF00E8A0));

    /** Selected block ids (e.g. minecraft:spawner). */
    private final Set<String> selected = new LinkedHashSet<>();
    private final List<BlockPos> hits = new ArrayList<>();
    private int tick;

    /** UI search filter (ClickGUI). */
    public String uiSearch = "";

    public BlockESP() {
        super("BlockESP", "ESP for custom blocks — search & add in settings", Category.RENDER);
        // Sensible defaults
        selected.add("minecraft:spawner");
        selected.add("minecraft:chest");
    }

    public Set<String> getSelected() {
        return selected;
    }

    public boolean addBlock(String id) {
        String norm = normalize(id);
        if (norm == null) return false;
        return selected.add(norm);
    }

    public boolean removeBlock(String id) {
        String norm = normalize(id);
        if (norm == null) return selected.remove(id.toLowerCase(Locale.ROOT));
        return selected.remove(norm);
    }

    public void clearBlocks() {
        selected.clear();
    }

    public void loadBlocks(String csv) {
        selected.clear();
        if (csv == null || csv.isBlank()) return;
        for (String p : csv.split(",")) {
            String n = normalize(p.trim());
            if (n != null) selected.add(n);
        }
    }

    public String saveBlocks() {
        return String.join(",", selected);
    }

    /** Registry search for ClickGUI. */
    public List<String> searchRegistry(String query, int limit) {
        List<String> out = new ArrayList<>();
        String q = query == null ? "" : query.toLowerCase(Locale.ROOT).trim();
        try {
            for (Block block : BuiltInRegistries.BLOCK) {
                ResourceLocation id = BuiltInRegistries.BLOCK.getKey(block);
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
        try {
            ResourceLocation rl = ResourceLocation.parse(id);
            if (!BuiltInRegistries.BLOCK.containsKey(rl)) return null;
            return rl.toString();
        } catch (Throwable t) {
            return null;
        }
    }

    @Override
    public void onTick() {
        if (selected.isEmpty()) {
            hits.clear();
            return;
        }
        if (++tick % 12 != 0) return;
        rescan();
    }

    private void rescan() {
        hits.clear();
        Minecraft mc = mc();
        if (mc == null || mc.level == null || mc.player == null) return;

        int r = range.getInt();
        BlockPos o = mc.player.blockPosition();
        int cR = (r >> 4) + 1;
        int cx0 = o.getX() >> 4;
        int cz0 = o.getZ() >> 4;

        Set<Block> wanted = new LinkedHashSet<>();
        for (String id : selected) {
            try {
                ResourceLocation rl = ResourceLocation.parse(id);
                BuiltInRegistries.BLOCK.getOptional(rl).ifPresent(wanted::add);
            } catch (Throwable ignored) {}
        }
        if (wanted.isEmpty()) return;

        for (int dcx = -cR; dcx <= cR; dcx++) {
            for (int dcz = -cR; dcz <= cR; dcz++) {
                LevelChunk chunk = mc.level.getChunkSource().getChunk(cx0 + dcx, cz0 + dcz, false);
                if (chunk == null) continue;
                int x0 = chunk.getPos().getMinBlockX();
                int z0 = chunk.getPos().getMinBlockZ();
                int minY = Math.max(mc.level.getMinY(), o.getY() - r);
                int maxY = Math.min(mc.level.getMaxY(), o.getY() + r);
                for (int y = minY; y <= maxY; y++) {
                    for (int x = 0; x < 16; x++) {
                        for (int z = 0; z < 16; z++) {
                            BlockPos pos = new BlockPos(x0 + x, y, z0 + z);
                            if (pos.distSqr(o) > (double) r * r) continue;
                            BlockState st = chunk.getBlockState(pos);
                            if (wanted.contains(st.getBlock())) hits.add(pos);
                        }
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
        double[] s = new double[2];
        for (BlockPos pos : hits) {
            if (!EspProjection.worldToScreen(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, s)) continue;
            EspDraw.box(g, s[0] - 5, s[1] - 5, s[0] + 5, s[1] + 5, col, 1);
        }
    }
}
