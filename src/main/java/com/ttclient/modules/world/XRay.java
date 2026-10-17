package com.ttclient.modules.world;

import com.ttclient.client.TTClientClient;
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
 * XRay — opacity mode (mixin) + ore ESP.
 *
 * Opacity: hides non-ore blocks client-side so ores pop (singleplayer / no anti-xray).
 * ESP: boxes/tracers on every ore block currently loaded on the client.
 *
 * Server anti-xray (Paper engine-mode / orebfuscation) never sends hidden ores to the
 * client — nothing can see blocks the server withheld. ESP still shows every ore
 * the server did send (cave-adjacent ores, exposed veins, etc.).
 */
public class XRay extends Module {
    public final BoolSetting opacity = addSetting(new BoolSetting(
            "Opacity", "Hide non-ores (classic XRay look)", true));
    public final BoolSetting esp = addSetting(new BoolSetting(
            "ESP", "Draw boxes on ores", true));
    public final BoolSetting tracers = addSetting(new BoolSetting(
            "Tracers", "Lines from crosshair to ores", true));
    public final BoolSetting fullbright = addSetting(new BoolSetting(
            "Fullbright", "Force max brightness while XRay is on", true));

    public final BoolSetting diamonds = addSetting(new BoolSetting("Diamonds", "Diamond ore", true));
    public final BoolSetting ancientDebris = addSetting(new BoolSetting("Debris", "Ancient debris", true));
    public final BoolSetting emerald = addSetting(new BoolSetting("Emerald", "Emerald ore", true));
    public final BoolSetting iron = addSetting(new BoolSetting("Iron", "Iron ore", true));
    public final BoolSetting gold = addSetting(new BoolSetting("Gold", "Gold / nether gold", true));
    public final BoolSetting copper = addSetting(new BoolSetting("Copper", "Copper ore", false));
    public final BoolSetting lapis = addSetting(new BoolSetting("Lapis", "Lapis ore", false));
    public final BoolSetting redstone = addSetting(new BoolSetting("Redstone", "Redstone ore", false));
    public final BoolSetting coal = addSetting(new BoolSetting("Coal", "Coal ore", false));
    public final BoolSetting quartz = addSetting(new BoolSetting("Quartz", "Nether quartz", false));

    public final NumberSetting range = addSetting(new NumberSetting(
            "Range", "ESP scan range", 64, 16, 128, 8));

    private final List<Hit> hits = new ArrayList<>();
    private int tick;
    private Double oldGamma;

    public XRay() {
        super("XRay", "Opacity + ore ESP (client-loaded blocks only)", Category.WORLD);
    }

    public static XRay get() {
        try {
            var mods = TTClientClient.modules;
            if (mods == null) return null;
            return mods.getModule(XRay.class);
        } catch (Throwable t) {
            return null;
        }
    }

    public static boolean isOpacityActive() {
        XRay x = get();
        return x != null && x.isEnabled() && x.opacity.get();
    }

    public static boolean shouldHideNonOres() {
        return isOpacityActive();
    }

    public static boolean isOre(BlockState st) {
        XRay x = get();
        if (x == null || !x.isEnabled() || st == null) return false;
        return x.match(st) != 0;
    }

    @Override
    public void onEnable() {
        reloadChunks();
        applyGamma(true);
    }

    @Override
    public void onDisable() {
        hits.clear();
        applyGamma(false);
        reloadChunks();
    }

    @Override
    public void onTick() {
        if (fullbright.get()) applyGamma(true);

        if (!esp.get() && !tracers.get()) {
            hits.clear();
            return;
        }
        if (++tick % 10 != 0) return;
        rescan();
    }

    private void applyGamma(boolean on) {
        try {
            Minecraft mc = mc();
            if (mc == null || mc.options == null) return;
            try {
                var gamma = mc.options.gamma();
                if (on) {
                    if (oldGamma == null) {
                        try { oldGamma = gamma.get(); } catch (Throwable t) { oldGamma = 1.0; }
                    }
                    try { gamma.set(16.0); } catch (Throwable ignored) {}
                } else if (oldGamma != null) {
                    try { gamma.set(oldGamma); } catch (Throwable ignored) {}
                    oldGamma = null;
                }
            } catch (Throwable ignored) {}
        } catch (Throwable ignored) {}
    }

    private void reloadChunks() {
        try {
            Minecraft mc = mc();
            if (mc != null && mc.levelRenderer != null) {
                mc.levelRenderer.allChanged();
            }
        } catch (Throwable ignored) {}
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

        int worldMin = mc.level.getMinY();
        int worldMax = mc.level.getMaxY();
        int minY = Math.max(worldMin, o.getY() - r);
        int maxY = Math.min(worldMax, o.getY() + r);

        for (int dcx = -cR; dcx <= cR; dcx++) {
            for (int dcz = -cR; dcz <= cR; dcz++) {
                LevelChunk chunk = mc.level.getChunkSource().getChunk(cx0 + dcx, cz0 + dcz, false);
                if (chunk == null) continue;

                int x0 = chunk.getPos().getMinBlockX();
                int z0 = chunk.getPos().getMinBlockZ();

                for (int y = minY; y <= maxY; y++) {
                    for (int x = 0; x < 16; x++) {
                        for (int z = 0; z < 16; z++) {
                            BlockPos pos = new BlockPos(x0 + x, y, z0 + z);
                            if (pos.distSqr(o) > (double) r * r) continue;
                            BlockState st = chunk.getBlockState(pos);
                            int col = match(st);
                            if (col != 0) hits.add(new Hit(pos.immutable(), col, label(st)));
                        }
                    }
                }
            }
        }
    }

    private int match(BlockState st) {
        Block b = st.getBlock();
        try {
            if (diamonds.get() && (b == Blocks.DIAMOND_ORE || b == Blocks.DEEPSLATE_DIAMOND_ORE))
                return 0xFF55FFFF;
            if (ancientDebris.get() && b == Blocks.ANCIENT_DEBRIS)
                return 0xFFAA4444;
            if (emerald.get() && (b == Blocks.EMERALD_ORE || b == Blocks.DEEPSLATE_EMERALD_ORE))
                return 0xFF55FF55;
            if (iron.get() && (b == Blocks.IRON_ORE || b == Blocks.DEEPSLATE_IRON_ORE))
                return 0xFFD8D8D8;
            if (gold.get() && (b == Blocks.GOLD_ORE || b == Blocks.DEEPSLATE_GOLD_ORE || b == Blocks.NETHER_GOLD_ORE))
                return 0xFFFFD700;
            if (copper.get() && (b == Blocks.COPPER_ORE || b == Blocks.DEEPSLATE_COPPER_ORE))
                return 0xFFE07A3F;
            if (lapis.get() && (b == Blocks.LAPIS_ORE || b == Blocks.DEEPSLATE_LAPIS_ORE))
                return 0xFF4444FF;
            if (redstone.get() && (b == Blocks.REDSTONE_ORE || b == Blocks.DEEPSLATE_REDSTONE_ORE))
                return 0xFFFF2222;
            if (coal.get() && (b == Blocks.COAL_ORE || b == Blocks.DEEPSLATE_COAL_ORE))
                return 0xFF555555;
            if (quartz.get() && b == Blocks.NETHER_QUARTZ_ORE)
                return 0xFFE8E8E8;
        } catch (Throwable t) {
            String n = b.toString().toLowerCase();
            if (diamonds.get() && n.contains("diamond_ore")) return 0xFF55FFFF;
            if (ancientDebris.get() && n.contains("ancient_debris")) return 0xFFAA4444;
            if (emerald.get() && n.contains("emerald_ore")) return 0xFF55FF55;
            if (iron.get() && n.contains("iron_ore")) return 0xFFD8D8D8;
            if (gold.get() && n.contains("gold_ore")) return 0xFFFFD700;
            if (copper.get() && n.contains("copper_ore")) return 0xFFE07A3F;
            if (lapis.get() && n.contains("lapis_ore")) return 0xFF4444FF;
            if (redstone.get() && n.contains("redstone_ore")) return 0xFFFF2222;
            if (coal.get() && n.contains("coal_ore")) return 0xFF555555;
            if (quartz.get() && n.contains("quartz_ore")) return 0xFFE8E8E8;
        }
        return 0;
    }

    private String label(BlockState st) {
        String n = st.getBlock().toString().toLowerCase();
        if (n.contains("diamond")) return "Diamond";
        if (n.contains("debris")) return "Debris";
        if (n.contains("emerald")) return "Emerald";
        if (n.contains("iron")) return "Iron";
        if (n.contains("gold")) return "Gold";
        if (n.contains("copper")) return "Copper";
        if (n.contains("lapis")) return "Lapis";
        if (n.contains("redstone")) return "Redstone";
        if (n.contains("coal")) return "Coal";
        if (n.contains("quartz")) return "Quartz";
        return "Ore";
    }

    @Override
    public void onRender2D(GuiGraphicsExtractor g, float partialTick) {
        if (!esp.get() && !tracers.get()) return;
        Minecraft mc = mc();
        if (mc == null || mc.font == null || mc.player == null) return;

        int cx = mc.getWindow().getGuiScaledWidth() / 2;
        int cy = mc.getWindow().getGuiScaledHeight() / 2;

        for (Hit h : hits) {
            double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE;
            double maxX = -Double.MAX_VALUE, maxY = -Double.MAX_VALUE;
            int visible = 0;
            for (int i = 0; i < 8; i++) {
                double bx = h.pos.getX() + ((i & 1) == 0 ? 0.02 : 0.98);
                double by = h.pos.getY() + ((i & 2) == 0 ? 0.02 : 0.98);
                double bz = h.pos.getZ() + ((i & 4) == 0 ? 0.02 : 0.98);
                double[] p = new double[2];
                if (!EspProjection.worldToScreen(bx, by, bz, p)) continue;
                minX = Math.min(minX, p[0]);
                minY = Math.min(minY, p[1]);
                maxX = Math.max(maxX, p[0]);
                maxY = Math.max(maxY, p[1]);
                visible++;
            }
            if (visible < 2) continue;
            if (maxX - minX < 4) {
                double c = (minX + maxX) * 0.5;
                minX = c - 2;
                maxX = c + 2;
            }
            if (maxY - minY < 4) {
                double c = (minY + maxY) * 0.5;
                minY = c - 2;
                maxY = c + 2;
            }

            int col = h.color | 0xFF000000;
            if (esp.get()) {
                EspDraw.box(g, minX, minY, maxX, maxY, col, 1);
                EspDraw.label(g, mc.font, h.label, (minX + maxX) * 0.5, minY - 11, col);
            }
            if (tracers.get()) {
                double mx = (minX + maxX) * 0.5;
                double my = (minY + maxY) * 0.5;
                EspDraw.line(g, cx, cy, mx, my, col);
            }
        }
    }

    private record Hit(BlockPos pos, int color, String label) {}
}
