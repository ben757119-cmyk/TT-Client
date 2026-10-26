package com.ttclient.modules.world;

import com.ttclient.modules.BlockListModule;
import com.ttclient.modules.Category;
import com.ttclient.modules.Module;
import com.ttclient.settings.BoolSetting;
import com.ttclient.settings.ModeSetting;
import com.ttclient.settings.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Sphere nuker: Normal / Flatten / Selective (block list like BlockESP). */
public class Nuker extends Module implements BlockListModule {
    public final ModeSetting mode = addSetting(new ModeSetting(
            "Mode", "Normal=all, Flatten=at/above feet, Selective=listed blocks only",
            "Normal", "Normal", "Flatten", "Selective"));
    public final NumberSetting range = addSetting(new NumberSetting("Range", "Break range", 4.5, 1.0, 6.0, 0.5));
    public final NumberSetting perTick = addSetting(new NumberSetting("PerTick", "Max blocks broken per tick", 1, 1, 8, 1));
    public final BoolSetting swing = addSetting(new BoolSetting("Swing", "Swing hand", true));
    public final BoolSetting packet = addSetting(new BoolSetting(
            "Packet", "Send START+STOP packets (faster, more detectable)", false));

    private final Set<String> selected = new LinkedHashSet<>();
    public String uiSearch = "";
    public boolean uiSearchFocused = false;

    public Nuker() {
        super("Nuker", "Auto-break blocks around you (select blocks like BlockESP)", Category.WORLD);
    }

    @Override public Set<String> getSelected() { return selected; }
    @Override public String getUiSearch() { return uiSearch == null ? "" : uiSearch; }
    @Override public void setUiSearch(String s) { uiSearch = s == null ? "" : s; }
    @Override public boolean isUiSearchFocused() { return uiSearchFocused; }
    @Override public void setUiSearchFocused(boolean focused) { uiSearchFocused = focused; }
    @Override public String listTitle() { return "break list"; }

    @Override
    public boolean addBlock(String id) {
        String n = normalize(id);
        return n != null && selected.add(n);
    }

    @Override
    public boolean removeBlock(String id) {
        String n = normalize(id);
        if (n == null) return selected.remove(id.toLowerCase(Locale.ROOT));
        return selected.remove(n);
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

    private boolean isBreakable(Minecraft mc, BlockPos pos, BlockState st) {
        if (st == null || st.isAir()) return false;
        try {
            if (st.getDestroySpeed(mc.level, pos) < 0) return false;
        } catch (Throwable ignored) {}
        return true;
    }

    @Override
    public void onTick() {
        Minecraft mc = mc();
        if (mc == null || mc.player == null || mc.level == null || mc.gameMode == null) return;
        try {
            Object scr = null;
            try { scr = mc.gui.screen(); } catch (Throwable t) {
                try { scr = Minecraft.class.getMethod("getScreen").invoke(mc); } catch (Throwable ignored) {}
            }
            if (scr != null) return;
        } catch (Throwable ignored) {}

        String m = String.valueOf(mode.get());
        if ("Selective".equalsIgnoreCase(m) && selected.isEmpty()) return;

        double r = range.get();
        double r2 = r * r;
        BlockPos origin = mc.player.blockPosition();
        Vec3 eye = mc.player.getEyePosition(1f);
        int feetY = origin.getY();

        List<BlockPos> candidates = new ArrayList<>();
        int ri = (int) Math.ceil(r);
        for (int dx = -ri; dx <= ri; dx++) {
            for (int dy = -ri; dy <= ri; dy++) {
                for (int dz = -ri; dz <= ri; dz++) {
                    BlockPos pos = origin.offset(dx, dy, dz);
                    double dist2 = eye.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
                    if (dist2 > r2) continue;
                    if ("Flatten".equalsIgnoreCase(m) && pos.getY() < feetY) continue;
                    BlockState st = mc.level.getBlockState(pos);
                    if (!isBreakable(mc, pos, st)) continue;
                    if ("Selective".equalsIgnoreCase(m) && !matchesSelected(st)) continue;
                    candidates.add(pos.immutable());
                }
            }
        }

        candidates.sort(Comparator.comparingDouble(p ->
                eye.distanceToSqr(p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5)));

        int limit = Math.max(1, perTick.getInt());
        int broken = 0;
        for (BlockPos pos : candidates) {
            if (broken >= limit) break;
            if (breakOne(mc, pos)) broken++;
        }
    }

    private boolean breakOne(Minecraft mc, BlockPos pos) {
        Direction dir = closestFace(mc.player.getEyePosition(1f), pos);
        try {
            if (packet.get()) {
                var conn = mc.player.connection;
                if (conn == null) return false;
                conn.send(new net.minecraft.network.protocol.game.ServerboundPlayerActionPacket(
                        net.minecraft.network.protocol.game.ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK, pos, dir));
                conn.send(new net.minecraft.network.protocol.game.ServerboundPlayerActionPacket(
                        net.minecraft.network.protocol.game.ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK, pos, dir));
            } else {
                mc.gameMode.startDestroyBlock(pos, dir);
                mc.gameMode.continueDestroyBlock(pos, dir);
            }
            if (swing.get()) {
                try { mc.player.swing(net.minecraft.world.InteractionHand.MAIN_HAND); } catch (Throwable ignored) {}
            }
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    private static Direction closestFace(Vec3 eye, BlockPos pos) {
        double cx = pos.getX() + 0.5 - eye.x;
        double cy = pos.getY() + 0.5 - eye.y;
        double cz = pos.getZ() + 0.5 - eye.z;
        double ax = Math.abs(cx), ay = Math.abs(cy), az = Math.abs(cz);
        if (ay >= ax && ay >= az) return cy > 0 ? Direction.DOWN : Direction.UP;
        if (ax >= az) return cx > 0 ? Direction.WEST : Direction.EAST;
        return cz > 0 ? Direction.NORTH : Direction.SOUTH;
    }
}
