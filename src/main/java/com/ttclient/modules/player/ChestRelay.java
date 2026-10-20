package com.ttclient.modules.player;

import com.ttclient.modules.Category;
import com.ttclient.modules.Module;
import com.ttclient.settings.BoolSetting;
import com.ttclient.settings.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Ordered chest list. Every Interval seconds: transfer items from chest[i] -> chest[i+1]
 * using camera look + open only (no WASD movement).
 *
 * Mark chests: look at a chest and run {@code .chest add}
 */
public class ChestRelay extends Module {
    public final NumberSetting interval = addSetting(new NumberSetting(
            "Interval", "Seconds between each hop (1->2, then 2->3, ...)", 60, 5, 600, 5));
    public final NumberSetting range = addSetting(new NumberSetting(
            "Range", "Max reach to open a marked chest", 4.5, 2.0, 6.0, 0.5));
    public final NumberSetting clickDelay = addSetting(new NumberSetting(
            "ClickDelay", "Ticks between inventory clicks", 2, 0, 10, 1));
    public final NumberSetting rotateTicks = addSetting(new NumberSetting(
            "RotateTicks", "Ticks to aim camera before opening", 5, 1, 20, 1));
    public final BoolSetting loop = addSetting(new BoolSetting(
            "Loop", "Restart from #1 after the last hop", false));
    public final BoolSetting closeAfter = addSetting(new BoolSetting(
            "CloseAfter", "Close container when step finishes", true));

    /** Ordered chest positions (world). */
    private final List<BlockPos> chests = new ArrayList<>();

    private enum Phase {
        IDLE_WAIT,
        AIM_SRC,
        OPEN_SRC,
        LOOT_SRC,
        CLOSE_SRC,
        AIM_DST,
        OPEN_DST,
        DEPOSIT_DST,
        CLOSE_DST,
        ADVANCE
    }

    private Phase phase = Phase.IDLE_WAIT;
    private int pairIndex;      // transfer chests[pairIndex] -> chests[pairIndex+1]
    private int tickCounter;
    private int waitTicks;
    private int clickCd;
    private int aimTicks;

    public ChestRelay() {
        super("ChestRelay", "Transfer items along a marked chest list (camera only)", Category.PLAYER);
    }

    public static ChestRelay get() {
        try {
            var mods = com.ttclient.client.TTClientClient.modules;
            return mods == null ? null : mods.getModule(ChestRelay.class);
        } catch (Throwable t) {
            return null;
        }
    }

    public List<BlockPos> getChests() {
        return chests;
    }

    public boolean addChest(BlockPos pos) {
        if (pos == null) return false;
        BlockPos im = pos.immutable();
        if (chests.contains(im)) return false;
        chests.add(im);
        return true;
    }

    public boolean removeIndex(int index) {
        if (index < 0 || index >= chests.size()) return false;
        chests.remove(index);
        if (pairIndex >= Math.max(0, chests.size() - 1)) pairIndex = 0;
        return true;
    }

    public void clearChests() {
        chests.clear();
        pairIndex = 0;
        phase = Phase.IDLE_WAIT;
    }

    @Override
    public void onEnable() {
        phase = Phase.IDLE_WAIT;
        pairIndex = 0;
        tickCounter = 0;
        // First hop soon so user can verify, then full interval
        waitTicks = 20;
        clickCd = 0;
        aimTicks = 0;
    }

    @Override
    public void onDisable() {
        phase = Phase.IDLE_WAIT;
        closeScreen();
    }

    @Override
    public void onTick() {
        Minecraft mc = mc();
        if (mc == null || mc.player == null || mc.level == null || mc.gameMode == null) return;

        // Never drive WASD — only clear movement intent while active transferring
        if (phase != Phase.IDLE_WAIT) {
            zeroMovement(mc);
        }

        if (chests.size() < 2) {
            phase = Phase.IDLE_WAIT;
            return;
        }

        if (clickCd > 0) clickCd--;

        switch (phase) {
            case IDLE_WAIT -> {
                if (waitTicks > 0) {
                    waitTicks--;
                    return;
                }
                if (pairIndex >= chests.size() - 1) {
                    if (loop.get()) {
                        pairIndex = 0;
                    } else {
                        setEnabled(false);
                        return;
                    }
                }
                phase = Phase.AIM_SRC;
                aimTicks = rotateTicks.getInt();
            }
            case AIM_SRC -> {
                BlockPos src = chests.get(pairIndex);
                if (!lookAt(mc, src)) {
                    // out of range / missing — skip hop
                    phase = Phase.ADVANCE;
                    return;
                }
                if (aimTicks-- <= 0) phase = Phase.OPEN_SRC;
            }
            case OPEN_SRC -> {
                BlockPos src = chests.get(pairIndex);
                if (isContainerOpen(mc)) {
                    phase = Phase.LOOT_SRC;
                    return;
                }
                if (!lookAt(mc, src)) {
                    phase = Phase.ADVANCE;
                    return;
                }
                tryOpen(mc, src);
                // give server a moment
                aimTicks = 10;
                phase = Phase.LOOT_SRC;
            }
            case LOOT_SRC -> {
                if (!isContainerOpen(mc)) {
                    // still opening
                    if (aimTicks > 0) {
                        aimTicks--;
                        BlockPos src = chests.get(pairIndex);
                        lookAt(mc, src);
                        if (aimTicks == 5) tryOpen(mc, src);
                        return;
                    }
                    phase = Phase.ADVANCE; // failed open
                    return;
                }
                if (clickCd > 0) return;
                if (quickMoveOneContainerSlot(mc)) {
                    clickCd = clickDelay.getInt();
                    return;
                }
                // empty or done
                phase = Phase.CLOSE_SRC;
            }
            case CLOSE_SRC -> {
                if (closeAfter.get()) closeScreen();
                phase = Phase.AIM_DST;
                aimTicks = rotateTicks.getInt();
            }
            case AIM_DST -> {
                BlockPos dst = chests.get(pairIndex + 1);
                if (!lookAt(mc, dst)) {
                    phase = Phase.ADVANCE;
                    return;
                }
                if (aimTicks-- <= 0) phase = Phase.OPEN_DST;
            }
            case OPEN_DST -> {
                BlockPos dst = chests.get(pairIndex + 1);
                if (isContainerOpen(mc)) {
                    phase = Phase.DEPOSIT_DST;
                    return;
                }
                if (!lookAt(mc, dst)) {
                    phase = Phase.ADVANCE;
                    return;
                }
                tryOpen(mc, dst);
                aimTicks = 10;
                phase = Phase.DEPOSIT_DST;
            }
            case DEPOSIT_DST -> {
                if (!isContainerOpen(mc)) {
                    if (aimTicks > 0) {
                        aimTicks--;
                        BlockPos dst = chests.get(pairIndex + 1);
                        lookAt(mc, dst);
                        if (aimTicks == 5) tryOpen(mc, dst);
                        return;
                    }
                    phase = Phase.ADVANCE;
                    return;
                }
                if (clickCd > 0) return;
                if (quickMoveOnePlayerSlot(mc)) {
                    clickCd = clickDelay.getInt();
                    return;
                }
                phase = Phase.CLOSE_DST;
            }
            case CLOSE_DST -> {
                if (closeAfter.get()) closeScreen();
                phase = Phase.ADVANCE;
            }
            case ADVANCE -> {
                pairIndex++;
                waitTicks = Math.max(1, interval.getInt() * 20);
                phase = Phase.IDLE_WAIT;
                if (pairIndex >= chests.size() - 1 && !loop.get()) {
                    // finished chain after last wait is skipped
                    waitTicks = 5;
                }
            }
        }
    }

    private void zeroMovement(Minecraft mc) {
        try {
            mc.options.keyUp.setDown(false);
            mc.options.keyDown.setDown(false);
            mc.options.keyLeft.setDown(false);
            mc.options.keyRight.setDown(false);
            mc.options.keyJump.setDown(false);
        } catch (Throwable ignored) {}
        try {
            LocalPlayer p = mc.player;
            if (p != null) {
                p.setDeltaMovement(p.getDeltaMovement().multiply(0, 1, 0));
                Object in = p.input;
                if (in != null) {
                    for (var f : in.getClass().getDeclaredFields()) {
                        if (f.getType() == float.class) {
                            f.setAccessible(true);
                            f.setFloat(in, 0f);
                        }
                    }
                }
            }
        } catch (Throwable ignored) {}
    }

    /** Soft aim: only rotates camera/body look toward block center. Returns false if too far. */
    private boolean lookAt(Minecraft mc, BlockPos pos) {
        LocalPlayer p = mc.player;
        if (p == null || pos == null) return false;
        Vec3 eye = p.getEyePosition(1f);
        Vec3 target = Vec3.atCenterOf(pos);
        double dist = eye.distanceTo(target);
        if (dist > range.get().doubleValue() + 0.75) return false;

        double dx = target.x - eye.x;
        double dy = target.y - eye.y;
        double dz = target.z - eye.z;
        double horiz = Math.sqrt(dx * dx + dz * dz);
        float yaw = (float) (Mth.atan2(dz, dx) * (180.0 / Math.PI)) - 90f;
        float pitch = (float) (-(Mth.atan2(dy, horiz) * (180.0 / Math.PI)));
        pitch = Mth.clamp(pitch, -90f, 90f);

        p.setYRot(yaw);
        p.setXRot(pitch);
        p.yRotO = yaw;
        p.xRotO = pitch;
        return true;
    }

    private void tryOpen(Minecraft mc, BlockPos pos) {
        LocalPlayer p = mc.player;
        if (p == null || mc.gameMode == null || mc.level == null) return;
        try {
            Vec3 hit = Vec3.atCenterOf(pos);
            BlockHitResult bhr = new BlockHitResult(hit, Direction.UP, pos, false);
            mc.gameMode.useItemOn(p, InteractionHand.MAIN_HAND, bhr);
        } catch (Throwable ignored) {}
    }

    private boolean isContainerOpen(Minecraft mc) {
        try {
            return mc.gui.screen() instanceof ContainerScreen;
        } catch (Throwable t) {
            return false;
        }
    }

    private void closeScreen() {
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc == null || mc.player == null) return;
            try {
                mc.player.closeContainer();
            } catch (Throwable ignored) {}
            try {
                mc.gui.setScreen(null);
            } catch (Throwable ignored) {
                try {
                    Minecraft.class.getMethod("setScreen", net.minecraft.client.gui.screens.Screen.class)
                            .invoke(mc, new Object[]{null});
                } catch (Throwable ignored2) {}
            }
        } catch (Throwable ignored) {}
    }

    /** Shift-click one non-empty slot from the container (top) into player inv. */
    private boolean quickMoveOneContainerSlot(Minecraft mc) {
        try {
            if (!(mc.gui.screen() instanceof ContainerScreen screen)) return false;
            AbstractContainerMenu menu = screen.getMenu();
            int chestSlots = Math.max(0, menu.slots.size() - 36);
            for (int i = 0; i < chestSlots; i++) {
                Slot slot = menu.slots.get(i);
                if (slot != null && slot.hasItem()) {
                    return inventoryClick(mc, menu.containerId, i);
                }
            }
        } catch (Throwable ignored) {}
        return false;
    }

    /** Shift-click one item from player inventory area into the open container. */
    private boolean quickMoveOnePlayerSlot(Minecraft mc) {
        try {
            if (!(mc.gui.screen() instanceof ContainerScreen screen)) return false;
            AbstractContainerMenu menu = screen.getMenu();
            int chestSlots = Math.max(0, menu.slots.size() - 36);
            // player main inventory slots in container menu
            for (int i = chestSlots; i < menu.slots.size(); i++) {
                Slot slot = menu.slots.get(i);
                if (slot == null || !slot.hasItem()) continue;
                // skip armor-like if any show up; mostly main+hotbar
                return inventoryClick(mc, menu.containerId, i);
            }
        } catch (Throwable ignored) {}
        return false;
    }

    private boolean inventoryClick(Minecraft mc, int containerId, int slot) {
        try {
            var method = mc.gameMode.getClass().getMethod(
                    "handleInventoryMouseClick",
                    int.class, int.class, int.class,
                    Class.forName("net.minecraft.world.inventory.ClickType"),
                    net.minecraft.world.entity.player.Player.class
            );
            Object quickMove = Enum.valueOf(
                    (Class<? extends Enum>) Class.forName("net.minecraft.world.inventory.ClickType"),
                    "QUICK_MOVE"
            );
            method.invoke(mc.gameMode, containerId, slot, 0, quickMove, mc.player);
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    /** True if block looks like a storage container we can open. */
    public static boolean looksLikeChest(Minecraft mc, BlockPos pos) {
        if (mc == null || mc.level == null || pos == null) return false;
        try {
            BlockState st = mc.level.getBlockState(pos);
            String n = st.getBlock().toString().toLowerCase();
            if (n.contains("chest") || n.contains("barrel") || n.contains("shulker")
                    || n.contains("hopper") || n.contains("dispenser") || n.contains("dropper")) {
                return true;
            }
            BlockEntity be = mc.level.getBlockEntity(pos);
            return be instanceof RandomizableContainerBlockEntity;
        } catch (Throwable t) {
            return false;
        }
    }
}
