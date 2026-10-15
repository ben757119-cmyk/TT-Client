package com.ttclient.modules.world;

import com.ttclient.modules.Category;
import com.ttclient.modules.Module;
import com.ttclient.render.EspDraw;
import com.ttclient.render.EspProjection;
import com.ttclient.settings.BoolSetting;
import com.ttclient.settings.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.ArrayList;
import java.util.List;

/**
 * Ore ESP / soft xray — scans and boxes ores (true opacity xray needs mixins).
 */
public class XRay extends Module {
    public final BoolSetting diamonds = addSetting(new BoolSetting("Diamonds", "Diamond ore", true));
    public final BoolSetting ancientDebris = addSetting(new BoolSetting("Debris", "Ancient debris", true));
    public final BoolSetting iron = addSetting(new BoolSetting("Iron", "Iron ore", false));
    public final BoolSetting gold = addSetting(new BoolSetting("Gold", "Gold ore", false));
    public final BoolSetting emerald = addSetting(new BoolSetting("Emerald", "Emerald ore", true));
    public final BoolSetting lapis = addSetting(new BoolSetting("Lapis", "Lapis ore", false));
    public final BoolSetting redstone = addSetting(new BoolSetting("Redstone", "Redstone ore", false));
    public final BoolSetting coal = addSetting(new BoolSetting("Coal", "Coal ore", false));
    public final NumberSetting range = addSetting(new NumberSetting("Range", "Scan range", 48, 16, 96, 8));

    private final List<Hit> hits = new ArrayList<>();
    private int tick;

    public XRay() {
        super("XRay", "See ores through walls (ESP-style)", Category.WORLD);
    }

    @Override
    public void onEnable() {
        // Soft fullbright help while xray is on
        Minecraft mc = mc();
        if (mc != null && mc.options != null) {
            try {
                // gamma may not exist the same way — ignore failures
            } catch (Throwable ignored) {}
        }
    }

    @Override
    public void onTick() {
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
        int cx0 = o.getX() >> 4;
        int cz0 = o.getZ() >> 4;

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
                            int col = match(st);
                            if (col != 0) hits.add(new Hit(pos, col));
                        }
                    }
                }
            }
        }
    }

    private int match(BlockState st) {
        Block b = st.getBlock();
        if (diamonds.get() && (b == Blocks.DIAMOND_ORE || b == Blocks.DEEPSLATE_DIAMOND_ORE)) return 0xFF55FFFF;
        if (ancientDebris.get() && b == Blocks.ANCIENT_DEBRIS) return 0xFFAA4444;
        if (iron.get() && (b == Blocks.IRON_ORE || b == Blocks.DEEPSLATE_IRON_ORE)) return 0xFFD8D8D8;
        if (gold.get() && (b == Blocks.GOLD_ORE || b == Blocks.DEEPSLATE_GOLD_ORE || b == Blocks.NETHER_GOLD_ORE)) return 0xFFFFD700;
        if (emerald.get() && (b == Blocks.EMERALD_ORE || b == Blocks.DEEPSLATE_EMERALD_ORE)) return 0xFF55FF55;
        if (lapis.get() && (b == Blocks.LAPIS_ORE || b == Blocks.DEEPSLATE_LAPIS_ORE)) return 0xFF4444FF;
        if (redstone.get() && (b == Blocks.REDSTONE_ORE || b == Blocks.DEEPSLATE_REDSTONE_ORE)) return 0xFFFF2222;
        if (coal.get() && (b == Blocks.COAL_ORE || b == Blocks.DEEPSLATE_COAL_ORE)) return 0xFF333333;
        return 0;
    }

    @Override
    public void onRender2D(GuiGraphicsExtractor g, float partialTick) {
        Minecraft mc = mc();
        if (mc == null) return;
        double[] s = new double[2];
        for (Hit h : hits) {
            double x = h.pos.getX() + 0.5;
            double y = h.pos.getY() + 0.5;
            double z = h.pos.getZ() + 0.5;
            if (!EspProjection.worldToScreen(x, y, z, s)) continue;
            EspDraw.box(g, s[0] - 4, s[1] - 4, s[0] + 4, s[1] + 4, h.color, 1);
        }
    }

    private record Hit(BlockPos pos, int color) {}
}
