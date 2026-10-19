package com.ttclient.util;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;

/**
 * Force client chunk meshes to rebuild after XRay / Freecam wall-vision toggles.
 * One call is often not enough (async rebuild) — we also schedule delayed passes.
 */
public final class ChunkMeshReload {
    private static int delayA = -1;
    private static int delayB = -1;

    private ChunkMeshReload() {}

    /** Immediate rebuild + delayed passes at ~3 ticks and ~20 ticks. */
    public static void request() {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) return;
        mc.execute(() -> run(mc));
        delayA = 3;
        delayB = 20;
    }

    /** Call once per client tick from client setup. */
    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.level == null) {
            delayA = delayB = -1;
            return;
        }
        if (delayA == 0) {
            delayA = -1;
            mc.execute(() -> run(mc));
        } else if (delayA > 0) {
            delayA--;
        }
        if (delayB == 0) {
            delayB = -1;
            mc.execute(() -> run(mc));
        } else if (delayB > 0) {
            delayB--;
        }
    }

    private static void run(Minecraft mc) {
        try {
            Object lr = mc.levelRenderer;
            if (lr == null) return;

            // Prefer known full-reload names
            for (String name : new String[]{
                    "allChanged", "reloadAll", "rebuildAllChunks", "reload"
            }) {
                try {
                    lr.getClass().getMethod(name).invoke(lr);
                    dirtyNearbySections(mc, lr);
                    return;
                } catch (Throwable ignored) {}
            }

            // Fallback: any no-arg method that looks like a full rebuild
            for (var m : lr.getClass().getMethods()) {
                if (m.getParameterCount() != 0) continue;
                String n = m.getName().toLowerCase();
                if ((n.contains("all") && n.contains("chang"))
                        || n.equals("reload")
                        || (n.contains("rebuild") && n.contains("all"))) {
                    try {
                        m.invoke(lr);
                        dirtyNearbySections(mc, lr);
                        return;
                    } catch (Throwable ignored) {}
                }
            }

            dirtyNearbySections(mc, lr);
        } catch (Throwable ignored) {}
    }

    /** Mark a large area of sections dirty so meshes recompile even if allChanged is missing. */
    private static void dirtyNearbySections(Minecraft mc, Object lr) {
        try {
            if (mc.player == null || mc.level == null) return;
            BlockPos o = mc.player.blockPosition();
            int view = 8;
            try {
                view = Math.max(4, mc.options.getEffectiveRenderDistance());
            } catch (Throwable ignored) {
                try {
                    Object rd = mc.options.getClass().getMethod("renderDistance").invoke(mc.options);
                    Object v = rd.getClass().getMethod("get").invoke(rd);
                    if (v instanceof Number n) view = Math.max(4, n.intValue());
                } catch (Throwable ignored2) {}
            }

            int cx = SectionPos.blockToSectionCoord(o.getX());
            int cy = SectionPos.blockToSectionCoord(o.getY());
            int cz = SectionPos.blockToSectionCoord(o.getZ());
            int minSy = SectionPos.blockToSectionCoord(mc.level.getMinY());
            int maxSy = SectionPos.blockToSectionCoord(mc.level.getMaxY() - 1);

            // setSectionDirty(int, int, int, boolean) or similar
            for (String name : new String[]{"setSectionDirty", "setSectionDirtyWithNeighbors", "blockChanged"}) {
                for (var m : lr.getClass().getMethods()) {
                    if (!m.getName().equals(name)) continue;
                    Class<?>[] pt = m.getParameterTypes();
                    try {
                        if (pt.length == 4 && pt[0] == int.class && pt[3] == boolean.class) {
                            for (int dx = -view; dx <= view; dx++) {
                                for (int dz = -view; dz <= view; dz++) {
                                    for (int sy = minSy; sy <= maxSy; sy++) {
                                        m.invoke(lr, cx + dx, sy, cz + dz, true);
                                    }
                                }
                            }
                            return;
                        }
                        if (pt.length == 3 && pt[0] == int.class) {
                            for (int dx = -view; dx <= view; dx++) {
                                for (int dz = -view; dz <= view; dz++) {
                                    for (int sy = minSy; sy <= maxSy; sy++) {
                                        m.invoke(lr, cx + dx, sy, cz + dz);
                                    }
                                }
                            }
                            return;
                        }
                        if (pt.length == 1 && BlockPos.class.isAssignableFrom(pt[0])) {
                            // Only a few representative positions — full column grid is huge
                            for (int dx = -view; dx <= view; dx += 2) {
                                for (int dz = -view; dz <= view; dz += 2) {
                                    for (int y = mc.level.getMinY(); y < mc.level.getMaxY(); y += 16) {
                                        m.invoke(lr, new BlockPos(o.getX() + (dx << 4), y, o.getZ() + (dz << 4)));
                                    }
                                }
                            }
                            return;
                        }
                    } catch (Throwable ignored) {}
                }
            }
        } catch (Throwable ignored) {}
    }
}
