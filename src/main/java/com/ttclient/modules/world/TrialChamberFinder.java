package com.ttclient.modules.world;

import com.ttclient.client.NotificationManager;
import com.ttclient.modules.Category;
import com.ttclient.modules.Module;
import com.ttclient.settings.BoolSetting;
import com.ttclient.settings.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Scans loaded terrain for Trial Chamber signatures:
 * trial spawners, vaults, copper variants, hoppers near those markers.
 */
public class TrialChamberFinder extends Module {
    public final NumberSetting range = addSetting(new NumberSetting("Range", "Block scan radius", 64, 24, 128, 8));
    public final NumberSetting interval = addSetting(new NumberSetting("Interval", "Ticks between scans", 40, 10, 100, 5));
    public final BoolSetting notify = addSetting(new BoolSetting("Notify", "Toast on find", true));
    public final BoolSetting requireSpawner = addSetting(new BoolSetting("NeedSpawner", "Only alert on trial_spawner/vault", false));

    private int timer;
    private final Set<Long> reported = new HashSet<>();
    private BlockPos lastHit;
    private String lastLabel = "";

    public TrialChamberFinder() {
        super("TrialChamberFinder",
                "Find trial chambers via spawners, vaults, copper, hoppers",
                Category.WORLD);
    }

    @Override
    public void onEnable() {
        timer = 0;
        reported.clear();
        lastHit = null;
        lastLabel = "";
    }

    @Override
    public void onTick() {
        Minecraft mc = mc();
        if (mc == null || mc.player == null || mc.level == null) return;
        if (timer++ < interval.getInt()) return;
        timer = 0;
        scan(mc);
    }

    private void scan(Minecraft mc) {
        LocalPlayer player = mc.player;
        int r = range.getInt();
        BlockPos origin = player.blockPosition();

        BlockPos bestSpawner = null;
        BlockPos bestVault = null;
        BlockPos bestCluster = null;
        int bestClusterScore = 0;

        for (int dx = -r; dx <= r; dx += 2) {
            for (int dy = -40; dy <= 40; dy += 2) {
                for (int dz = -r; dz <= r; dz += 2) {
                    BlockPos pos = origin.offset(dx, dy, dz);
                    if (!mc.level.hasChunkAt(pos)) continue;
                    BlockState state = mc.level.getBlockState(pos);
                    if (state.isAir()) continue;

                    String id = state.getBlock().toString().toLowerCase(Locale.ROOT);

                    if (id.contains("trial_spawner")) {
                        bestSpawner = pos;
                    } else if (id.contains("ominous_vault") || id.contains("vault")) {
                        bestVault = pos;
                    } else if (id.contains("copper") || id.contains("hopper")) {
                        int score = scoreNeighborhood(mc, pos);
                        if (score > bestClusterScore) {
                            bestClusterScore = score;
                            bestCluster = pos;
                        }
                    }
                }
            }
        }

        BlockPos hit = null;
        String label = null;
        if (bestSpawner != null) {
            hit = bestSpawner;
            label = "Trial Spawner";
        } else if (bestVault != null) {
            hit = bestVault;
            label = "Vault";
        } else if (!requireSpawner.get() && bestCluster != null && bestClusterScore >= 6) {
            hit = bestCluster;
            label = "Copper/Hopper cluster (" + bestClusterScore + ")";
        }

        if (hit == null) return;

        long grid = BlockPos.asLong(hit.getX() >> 4, hit.getY() >> 4, hit.getZ() >> 4);
        if (reported.contains(grid)) {
            lastHit = hit;
            lastLabel = label;
            return;
        }
        reported.add(grid);
        if (reported.size() > 64) reported.clear();

        lastHit = hit;
        lastLabel = label;

        String msg = String.format(Locale.ROOT, "%s @ %d %d %d", label, hit.getX(), hit.getY(), hit.getZ());
        if (notify.get()) {
            NotificationManager.push("Trial Chamber", msg);
        }
    }

    private int scoreNeighborhood(Minecraft mc, BlockPos center) {
        int copper = 0;
        int hoppers = 0;
        int tuff = 0;
        int markers = 0;
        for (int dx = -4; dx <= 4; dx++) {
            for (int dy = -3; dy <= 3; dy++) {
                for (int dz = -4; dz <= 4; dz++) {
                    BlockPos p = center.offset(dx, dy, dz);
                    if (!mc.level.hasChunkAt(p)) continue;
                    String id = mc.level.getBlockState(p).getBlock().toString().toLowerCase(Locale.ROOT);
                    if (id.contains("trial_spawner") || id.contains("vault")) markers++;
                    else if (id.contains("hopper")) hoppers++;
                    else if (id.contains("copper")) copper++;
                    else if (id.contains("tuff")) tuff++;
                }
            }
        }
        return markers * 20 + hoppers * 3 + copper + tuff / 2;
    }

    public BlockPos getLastHit() { return lastHit; }
    public String getLastLabel() { return lastLabel; }
}
