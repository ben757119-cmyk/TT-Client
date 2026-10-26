package com.ttclient.modules.player;

import com.ttclient.modules.Category;
import com.ttclient.modules.Module;
import com.ttclient.settings.BoolSetting;
import com.ttclient.settings.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Meteor-style InstantRebreak: remember the last block you started mining,
 * then spam STOP_DESTROY_BLOCK at that position so soft / regenerating blocks
 * re-break instantly (crops, slime farms, etc.).
 */
public class InstantRebreak extends Module {
    public final NumberSetting delay = addSetting(new NumberSetting("Delay", "Ticks between rebreak packets", 0, 0, 20, 1));
    public final BoolSetting onlyPick = addSetting(new BoolSetting("OnlyPick", "Only when holding a pickaxe", true));
    public final BoolSetting swing = addSetting(new BoolSetting("Swing", "Swing hand each attempt", true));

    private final BlockPos.MutableBlockPos target = new BlockPos.MutableBlockPos(0, Integer.MIN_VALUE, 0);
    private Direction direction = Direction.UP;
    private int ticks;
    private boolean hasTarget;

    public InstantRebreak() {
        super("InstantRebreak", "Instantly re-breaks the last mined position (Meteor-style)", Category.PLAYER);
    }

    public static InstantRebreak get() {
        try {
            var mods = com.ttclient.client.TTClientClient.modules;
            return mods == null ? null : mods.getModule(InstantRebreak.class);
        } catch (Throwable t) {
            return null;
        }
    }

    /** Called from MultiPlayerGameMode mixin when the player starts mining. */
    public static void onStartBreak(BlockPos pos, Direction dir) {
        InstantRebreak ir = get();
        if (ir == null || !ir.isEnabled() || pos == null) return;
        ir.target.set(pos.getX(), pos.getY(), pos.getZ());
        ir.direction = dir == null ? Direction.UP : dir;
        ir.hasTarget = true;
        ir.ticks = 0;
    }

    @Override
    public void onEnable() {
        ticks = 0;
        hasTarget = false;
        target.set(0, Integer.MIN_VALUE, 0);
    }

    @Override
    public void onDisable() {
        hasTarget = false;
    }

    @Override
    public void onTick() {
        if (!hasTarget) {
            tryCaptureFromGameMode();
            if (!hasTarget) return;
        }
        if (ticks < delay.getInt()) {
            ticks++;
            return;
        }
        ticks = 0;
        if (!shouldMine()) return;
        sendRebreak();
    }

    private void tryCaptureFromGameMode() {
        Minecraft mc = mc();
        if (mc == null || mc.gameMode == null) return;
        try {
            var gm = mc.gameMode;
            BlockPos pos = null;
            Direction dir = Direction.UP;
            for (var f : gm.getClass().getDeclaredFields()) {
                f.setAccessible(true);
                Object v = f.get(gm);
                if (v instanceof BlockPos bp && pos == null) pos = bp;
                if (v instanceof Direction d) dir = d;
            }
            if (pos != null && pos.getY() > Integer.MIN_VALUE / 2) {
                float progress = 0f;
                try {
                    for (var f : gm.getClass().getDeclaredFields()) {
                        if (f.getType() == float.class) {
                            f.setAccessible(true);
                            float p = f.getFloat(gm);
                            if (p > 0f && p <= 1.01f) { progress = p; break; }
                        }
                    }
                } catch (Throwable ignored) {}
                if (progress > 0f || mc.options.keyAttack.isDown()) {
                    target.set(pos.getX(), pos.getY(), pos.getZ());
                    direction = dir;
                    hasTarget = true;
                }
            }
        } catch (Throwable ignored) {}
    }

    public boolean shouldMine() {
        Minecraft mc = mc();
        if (mc == null || mc.level == null || mc.player == null) return false;
        if (!hasTarget) return false;
        try {
            if (mc.level.isOutsideBuildHeight(target)) return false;
        } catch (Throwable ignored) {}
        BlockState st = mc.level.getBlockState(target);
        if (st == null || st.isAir()) return true;
        try {
            if (st.getDestroySpeed(mc.level, target) < 0) return false;
        } catch (Throwable ignored) {}
        if (onlyPick.get()) {
            try {
                if (!mc.player.getMainHandItem().is(ItemTags.PICKAXES)) return false;
            } catch (Throwable t) {
                return false;
            }
        }
        return true;
    }

    public void sendRebreak() {
        Minecraft mc = mc();
        if (mc == null || mc.player == null || mc.player.connection == null) return;
        Direction d = direction == null ? Direction.UP : direction;
        BlockPos pos = target.immutable();
        try {
            mc.player.connection.send(new ServerboundPlayerActionPacket(
                    ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK, pos, d));
            mc.player.connection.send(new ServerboundPlayerActionPacket(
                    ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK, pos, d));
            mc.player.connection.send(new ServerboundPlayerActionPacket(
                    ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK, pos, d));
        } catch (Throwable ignored) {}
        if (swing.get()) {
            try {
                mc.player.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
            } catch (Throwable ignored) {}
        }
    }
}
