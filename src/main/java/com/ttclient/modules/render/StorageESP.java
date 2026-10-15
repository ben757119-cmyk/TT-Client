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

/**
 * Meteor-style storage ESP — highlights chests, barrels, shulkers, hoppers, etc.
 */
public class StorageESP extends Module {
    public final BoolSetting chests = addSetting(new BoolSetting("Chests", "Chests / trapped", true));
    public final BoolSetting enderChests = addSetting(new BoolSetting("EnderChests", "Ender chests", true));
    public final BoolSetting shulkers = addSetting(new BoolSetting("Shulkers", "Shulker boxes", true));
    public final BoolSetting barrels = addSetting(new BoolSetting("Barrels", "Barrels", true));
    public final BoolSetting hoppers = addSetting(new BoolSetting("Hoppers", "Hoppers", false));
    public final NumberSetting range = addSetting(new NumberSetting("Range", "Scan range (blocks)", 64, 16, 128, 8));
    public final ColorSetting color = addSetting(new ColorSetting("Color", "Box color", 0xFFFFAA00));

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
        double[] a = new double[2];
        double[] b = new double[2];

        for (Target t : targets) {
            // Project block corners roughly via center + size
            double x = t.pos.getX() + 0.5;
            double y = t.pos.getY() + 0.5;
            double z = t.pos.getZ() + 0.5;
            if (!EspProjection.worldToScreen(x, y + 0.5, z, a)) continue;
            if (!EspProjection.worldToScreen(x, y - 0.5, z, b)) continue;

            double h = Math.max(12, Math.abs(a[1] - b[1]));
            double w = h * 0.7;
            EspDraw.box(g, a[0] - w / 2, a[1] - h / 2, a[0] + w / 2, a[1] + h / 2, t.color, 1);
            EspDraw.label(g, mc.font, t.label, a[0], a[1] - h / 2 - 11, t.color | 0xFF000000);
        }
    }

    private record Target(BlockPos pos, String label, int color) {}
}
