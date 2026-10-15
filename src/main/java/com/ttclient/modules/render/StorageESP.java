package com.ttclient.modules.render;

import com.ttclient.modules.Category;
import com.ttclient.modules.Module;
import com.ttclient.render.EspDraw;
import com.ttclient.render.EspProjection;
import com.ttclient.settings.BoolSetting;
import com.ttclient.settings.ColorSetting;
import com.ttclient.settings.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.ArrayList;
import java.util.List;

/** Meteor-style storage ESP — boxes projected to true block positions. */
public class StorageESP extends Module {
    public final BoolSetting chests = addSetting(new BoolSetting("Chests", "Chests / trapped", true));
    public final BoolSetting enderChests = addSetting(new BoolSetting("EnderChests", "Ender chests", true));
    public final BoolSetting shulkers = addSetting(new BoolSetting("Shulkers", "Shulker boxes", true));
    public final BoolSetting barrels = addSetting(new BoolSetting("Barrels", "Barrels", true));
    public final BoolSetting hoppers = addSetting(new BoolSetting("Hoppers", "Hoppers", false));
    public final NumberSetting range = addSetting(new NumberSetting("Range", "Scan range (blocks)", 64, 16, 128, 8));
    public final ColorSetting color = addSetting(new ColorSetting("Color", "Default color", 0xFFFFAA00));

    private final List<Target> targets = new ArrayList<>();
    private int scanTicker;

    public StorageESP() {
        super("StorageESP", "Highlight storage blocks through walls", Category.RENDER);
    }

    @Override
    public void onTick() {
        if (++scanTicker % 10 != 0) return;
        rescan();
    }

    private void rescan() {
        targets.clear();
        Minecraft mc = mc();
        if (mc == null || mc.level == null || mc.player == null) return;

        int r = range.getInt();
        BlockPos origin = mc.player.blockPosition();
        int cRadius = (r >> 4) + 1;
        int cx0 = origin.getX() >> 4;
        int cz0 = origin.getZ() >> 4;

        for (int dcx = -cRadius; dcx <= cRadius; dcx++) {
            for (int dcz = -cRadius; dcz <= cRadius; dcz++) {
                LevelChunk chunk = mc.level.getChunkSource().getChunk(cx0 + dcx, cz0 + dcz, false);
                if (chunk == null) continue;
                for (BlockEntity be : chunk.getBlockEntities().values()) {
                    BlockPos pos = be.getBlockPos();
                    if (pos.distSqr(origin) > (double) r * r) continue;
                    BlockState st = be.getBlockState();
                    String label = null;
                    int col = color.get();

                    if (chests.get() && (be instanceof ChestBlockEntity
                            || st.is(Blocks.CHEST) || st.is(Blocks.TRAPPED_CHEST))) {
                        label = "Chest";
                        col = 0xFFFFAA00;
                    } else if (enderChests.get() && st.is(Blocks.ENDER_CHEST)) {
                        label = "Ender";
                        col = 0xFFAA00FF;
                    } else if (barrels.get() && st.is(Blocks.BARREL)) {
                        label = "Barrel";
                        col = 0xFFCC8844;
                    } else if (shulkers.get() && st.getBlock().getDescriptionId().contains("shulker")) {
                        label = "Shulker";
                        col = 0xFFFF55FF;
                    } else if (hoppers.get() && (be instanceof HopperBlockEntity || st.is(Blocks.HOPPER))) {
                        label = "Hopper";
                        col = 0xFF888888;
                    }

                    if (label != null) targets.add(new Target(pos, label, col));
                }
            }
        }
    }

    @Override
    public void onRender2D(GuiGraphicsExtractor g, float partialTick) {
        Minecraft mc = mc();
        if (mc == null || mc.font == null) return;

        // Project all 8 corners of the block AABB and draw the 2D bounding box
        double[] sx = new double[8];
        double[] sy = new double[8];

        for (Target t : targets) {
            int visible = 0;
            double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE;
            double maxX = -Double.MAX_VALUE, maxY = -Double.MAX_VALUE;

            for (int i = 0; i < 8; i++) {
                double bx = t.pos.getX() + ((i & 1) == 0 ? 0.05 : 0.95);
                double by = t.pos.getY() + ((i & 2) == 0 ? 0.05 : 0.95);
                double bz = t.pos.getZ() + ((i & 4) == 0 ? 0.05 : 0.95);
                double[] p = new double[2];
                if (!EspProjection.worldToScreen(bx, by, bz, p)) continue;
                sx[visible] = p[0];
                sy[visible] = p[1];
                minX = Math.min(minX, p[0]);
                minY = Math.min(minY, p[1]);
                maxX = Math.max(maxX, p[0]);
                maxY = Math.max(maxY, p[1]);
                visible++;
            }
            if (visible < 2) continue;

            // Clamp tiny boxes so they're still visible
            if (maxX - minX < 4) { double c = (minX + maxX) * 0.5; minX = c - 2; maxX = c + 2; }
            if (maxY - minY < 4) { double c = (minY + maxY) * 0.5; minY = c - 2; maxY = c + 2; }

            EspDraw.box(g, minX, minY, maxX, maxY, t.color | 0xFF000000, 1);
            EspDraw.label(g, mc.font, t.label, (minX + maxX) * 0.5, minY - 11, t.color | 0xFF000000);
        }
    }

    private record Target(BlockPos pos, String label, int color) {}
}
