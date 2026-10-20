package com.ttclient.modules.player;

import com.ttclient.client.NotificationManager;
import com.ttclient.modules.Category;
import com.ttclient.modules.Module;
import com.ttclient.render.EspDraw;
import com.ttclient.render.EspProjection;
import com.ttclient.settings.BoolSetting;
import com.ttclient.settings.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Chain chest relay:
 * take all from #1 → dump into #2 → take all from #2 → dump into #3 …
 * If dest is full, dump remainder back into previous and mark dest full (removed).
 * Camera-only (no WASD).
 */
public class ChestRelay extends Module {
    public final NumberSetting interval = addSetting(new NumberSetting(
            "Interval", "Seconds between hops", 3, 1, 120, 1));
    public final NumberSetting range = addSetting(new NumberSetting(
            "Range", "Max reach", 4.5, 2.0, 6.0, 0.5));
    public final NumberSetting clickDelay = addSetting(new NumberSetting(
            "ClickDelay", "Ticks between clicks", 1, 0, 8, 1));
    public final NumberSetting rotateTicks = addSetting(new NumberSetting(
            "RotateTicks", "Aim ticks", 4, 1, 20, 1));
    public final BoolSetting loop = addSetting(new BoolSetting("Loop", "Loop the list", false));
    public final BoolSetting visuals = addSetting(new BoolSetting("Visuals", "ESP + numbers", true));
    public final BoolSetting removeFull = addSetting(new BoolSetting(
            "RemoveFull", "Drop full chests from the list", true));

    private final List<BlockPos> chests = new ArrayList<>();
    private final Set<BlockPos> fullMarked = new HashSet<>();

    private enum Phase {
        WAIT, AIM_SRC, OPEN_SRC, LOOT, CLOSE1, AIM_DST, OPEN_DST, DEPOSIT, FULL_BACK, CLOSE2, ADVANCE
    }

    private Phase phase = Phase.WAIT;
    private int srcIndex;
    private int waitTicks;
    private int clickCd;
    private int aimTicks;
    private int stuckTicks;
    private int depositFails; // consecutive clicks that didn't free a slot → treat as full

    public ChestRelay() {
        super("ChestRelay", "Chain loot chests with camera only", Category.PLAYER);
    }

    public static ChestRelay get() {
        try {
            var mods = com.ttclient.client.TTClientClient.modules;
            return mods == null ? null : mods.getModule(ChestRelay.class);
        } catch (Throwable t) {
            return null;
        }
    }

    public List<BlockPos> getChests() { return chests; }
    public Set<BlockPos> getFullMarked() { return fullMarked; }

    public boolean addChest(BlockPos pos) {
        if (pos == null) return false;
        BlockPos im = pos.immutable();
        if (chests.contains(im)) return false;
        chests.add(im);
        fullMarked.remove(im);
        return true;
    }

    public boolean removeIndex(int index) {
        if (index < 0 || index >= chests.size()) return false;
        BlockPos p = chests.remove(index);
        fullMarked.remove(p);
        if (srcIndex >= chests.size()) srcIndex = Math.max(0, chests.size() - 1);
        return true;
    }

    public void clearChests() {
        chests.clear();
        fullMarked.clear();
        srcIndex = 0;
        phase = Phase.WAIT;
    }

    @Override
    public void onEnable() {
        phase = Phase.WAIT;
        srcIndex = 0;
        waitTicks = 10;
        clickCd = 0;
        aimTicks = 0;
        stuckTicks = 0;
        depositFails = 0;
        NotificationManager.push("ChestRelay", "started · " + chests.size() + " chests");
    }

    @Override
    public void onDisable() {
        phase = Phase.WAIT;
        closeScreen();
        NotificationManager.push("ChestRelay", "stopped");
    }

    @Override
    public void onTick() {
        Minecraft mc = mc();
        if (mc == null || mc.player == null || mc.level == null || mc.gameMode == null) return;

        if (phase != Phase.WAIT) zeroMovement(mc);

        // prune full from list if setting on
        if (removeFull.get() && !fullMarked.isEmpty()) {
            Iterator<BlockPos> it = chests.iterator();
            while (it.hasNext()) {
                BlockPos p = it.next();
                if (fullMarked.contains(p)) {
                    it.remove();
                    NotificationManager.push("ChestRelay", "removed full " + p.toShortString());
                }
            }
            fullMarked.retainAll(new HashSet<>(chests));
        }

        if (chests.size() < 2) {
            phase = Phase.WAIT;
            return;
        }

        if (clickCd > 0) clickCd--;

        switch (phase) {
            case WAIT -> {
                if (waitTicks > 0) { waitTicks--; return; }
                if (srcIndex >= chests.size() - 1) {
                    if (loop.get()) srcIndex = 0;
                    else { setEnabled(false); return; }
                }
                // skip full sources
                while (srcIndex < chests.size() - 1 && fullMarked.contains(chests.get(srcIndex))) srcIndex++;
                if (srcIndex >= chests.size() - 1) {
                    if (loop.get()) { srcIndex = 0; return; }
                    setEnabled(false);
                    return;
                }
                phase = Phase.AIM_SRC;
                aimTicks = rotateTicks.getInt();
                stuckTicks = 0;
            }
            case AIM_SRC -> {
                if (!lookAt(mc, chests.get(srcIndex))) { phase = Phase.ADVANCE; return; }
                if (aimTicks-- <= 0) phase = Phase.OPEN_SRC;
            }
            case OPEN_SRC -> {
                if (isContainerOpen(mc)) { phase = Phase.LOOT; return; }
                lookAt(mc, chests.get(srcIndex));
                tryOpen(mc, chests.get(srcIndex));
                aimTicks = 12;
                phase = Phase.LOOT;
            }
            case LOOT -> {
                if (!isContainerOpen(mc)) {
                    if (aimTicks-- > 0) {
                        lookAt(mc, chests.get(srcIndex));
                        if (aimTicks == 6) tryOpen(mc, chests.get(srcIndex));
                        return;
                    }
                    phase = Phase.ADVANCE;
                    return;
                }
                if (clickCd > 0) return;
                if (quickMoveOneContainerSlot(mc)) {
                    clickCd = clickDelay.getInt();
                    stuckTicks = 0;
                    return;
                }
                // container empty
                phase = Phase.CLOSE1;
            }
            case CLOSE1 -> {
                closeScreen();
                phase = Phase.AIM_DST;
                aimTicks = rotateTicks.getInt();
                depositFails = 0;
            }
            case AIM_DST -> {
                int dst = nextOpenIndex(srcIndex + 1);
                if (dst < 0) { phase = Phase.ADVANCE; return; }
                if (!lookAt(mc, chests.get(dst))) { phase = Phase.ADVANCE; return; }
                if (aimTicks-- <= 0) phase = Phase.OPEN_DST;
            }
            case OPEN_DST -> {
                int dst = nextOpenIndex(srcIndex + 1);
                if (dst < 0) { phase = Phase.ADVANCE; return; }
                if (isContainerOpen(mc)) { phase = Phase.DEPOSIT; return; }
                lookAt(mc, chests.get(dst));
                tryOpen(mc, chests.get(dst));
                aimTicks = 12;
                phase = Phase.DEPOSIT;
            }
            case DEPOSIT -> {
                int dst = nextOpenIndex(srcIndex + 1);
                if (dst < 0) { phase = Phase.ADVANCE; return; }
                if (!isContainerOpen(mc)) {
                    if (aimTicks-- > 0) {
                        lookAt(mc, chests.get(dst));
                        if (aimTicks == 6) tryOpen(mc, chests.get(dst));
                        return;
                    }
                    phase = Phase.ADVANCE;
                    return;
                }
                if (clickCd > 0) return;
                // If player inv (from loot) still has items to move
                if (playerHasMovableItems(mc)) {
                    boolean moved = quickMoveOnePlayerSlot(mc);
                    clickCd = clickDelay.getInt();
                    if (!moved) {
                        depositFails++;
                        // chest full — can't shift-click in
                        if (depositFails >= 3) {
                            markFull(chests.get(dst));
                            phase = Phase.FULL_BACK;
                            closeScreen();
                            aimTicks = rotateTicks.getInt();
                        }
                    } else {
                        depositFails = 0;
                    }
                    return;
                }
                // all deposited
                phase = Phase.CLOSE2;
            }
            case FULL_BACK -> {
                // put remaining loot back into previous (source) chest
                if (!lookAt(mc, chests.get(srcIndex))) { phase = Phase.ADVANCE; return; }
                if (aimTicks > 0) { aimTicks--; return; }
                if (!isContainerOpen(mc)) {
                    tryOpen(mc, chests.get(srcIndex));
                    aimTicks = 8;
                    return;
                }
                if (clickCd > 0) return;
                if (playerHasMovableItems(mc)) {
                    quickMoveOnePlayerSlot(mc);
                    clickCd = clickDelay.getInt();
                    return;
                }
                closeScreen();
                phase = Phase.ADVANCE;
            }
            case CLOSE2 -> {
                closeScreen();
                phase = Phase.ADVANCE;
            }
            case ADVANCE -> {
                // After dumping into dst, that dst becomes the next "source" (take out and move forward)
                srcIndex = nextOpenIndex(srcIndex + 1);
                if (srcIndex < 0) srcIndex = chests.size(); // done
                waitTicks = Math.max(5, interval.getInt() * 20);
                phase = Phase.WAIT;
            }
        }
    }

    private int nextOpenIndex(int from) {
        for (int i = from; i < chests.size(); i++) {
            if (!fullMarked.contains(chests.get(i))) return i;
        }
        return -1;
    }

    private void markFull(BlockPos pos) {
        if (pos == null) return;
        fullMarked.add(pos.immutable());
        NotificationManager.push("ChestRelay", "full " + pos.toShortString());
    }

    private boolean playerHasMovableItems(Minecraft mc) {
        try {
            if (!(mc.gui.screen() instanceof ContainerScreen screen)) {
                // check actual inv
                var inv = mc.player.getInventory();
                for (int i = 0; i < 36; i++) {
                    if (!inv.getItem(i).isEmpty()) return true;
                }
                return false;
            }
            AbstractContainerMenu menu = screen.getMenu();
            int chestSlots = Math.max(0, menu.slots.size() - 36);
            for (int i = chestSlots; i < menu.slots.size(); i++) {
                Slot slot = menu.slots.get(i);
                if (slot != null && slot.hasItem()) return true;
            }
        } catch (Throwable ignored) {}
        return false;
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
            if (p == null) return;
            Object in = p.input;
            if (in != null) {
                for (var f : in.getClass().getDeclaredFields()) {
                    if (f.getType() == float.class) {
                        f.setAccessible(true);
                        f.setFloat(in, 0f);
                    }
                }
            }
        } catch (Throwable ignored) {}
    }

    private boolean lookAt(Minecraft mc, BlockPos pos) {
        LocalPlayer p = mc.player;
        if (p == null || pos == null) return false;
        Vec3 eye = p.getEyePosition(1f);
        Vec3 target = Vec3.atCenterOf(pos);
        if (eye.distanceTo(target) > range.get().doubleValue() + 0.8) return false;
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
        try {
            BlockHitResult bhr = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
            mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, bhr);
        } catch (Throwable ignored) {}
    }

    private boolean isContainerOpen(Minecraft mc) {
        try { return mc.gui.screen() instanceof ContainerScreen; }
        catch (Throwable t) { return false; }
    }

    private void closeScreen() {
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc == null || mc.player == null) return;
            try { mc.player.closeContainer(); } catch (Throwable ignored) {}
            try { mc.gui.setScreen(null); } catch (Throwable ignored) {}
        } catch (Throwable ignored) {}
    }

    private boolean quickMoveOneContainerSlot(Minecraft mc) {
        try {
            if (!(mc.gui.screen() instanceof ContainerScreen screen)) return false;
            AbstractContainerMenu menu = screen.getMenu();
            int chestSlots = Math.max(0, menu.slots.size() - 36);
            for (int i = 0; i < chestSlots; i++) {
                Slot slot = menu.slots.get(i);
                if (slot != null && slot.hasItem()) return inventoryClick(mc, menu.containerId, i);
            }
        } catch (Throwable ignored) {}
        return false;
    }

    private boolean quickMoveOnePlayerSlot(Minecraft mc) {
        try {
            if (!(mc.gui.screen() instanceof ContainerScreen screen)) return false;
            AbstractContainerMenu menu = screen.getMenu();
            int chestSlots = Math.max(0, menu.slots.size() - 36);
            for (int i = chestSlots; i < menu.slots.size(); i++) {
                Slot slot = menu.slots.get(i);
                if (slot != null && slot.hasItem()) return inventoryClick(mc, menu.containerId, i);
            }
        } catch (Throwable ignored) {}
        return false;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
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

    public static boolean looksLikeChest(Minecraft mc, BlockPos pos) {
        if (mc == null || mc.level == null || pos == null) return false;
        try {
            BlockState st = mc.level.getBlockState(pos);
            String n = st.getBlock().toString().toLowerCase();
            if (n.contains("chest") || n.contains("barrel") || n.contains("shulker")
                    || n.contains("hopper") || n.contains("dispenser") || n.contains("dropper"))
                return true;
            BlockEntity be = mc.level.getBlockEntity(pos);
            return be instanceof RandomizableContainerBlockEntity;
        } catch (Throwable t) {
            return false;
        }
    }

    @Override
    public void onRender2D(GuiGraphicsExtractor g, float partialTick) {
        if (!visuals.get() || chests.isEmpty()) return;
        Minecraft mc = mc();
        if (mc == null || mc.font == null) return;

        // HUD legend
        int lx = 8, ly = 48;
        g.fill(lx - 2, ly - 2, lx + 120, ly + 34, 0xA0080A0E);
        g.text(mc.font, "ChestRelay", lx, ly, 0xFF00E8A0, false);
        g.text(mc.font, chests.size() + " marked", lx, ly + 10, 0xFF9CA3AF, false);
        String ph = phase.name();
        g.text(mc.font, "#" + (srcIndex + 1) + " " + ph, lx, ly + 20, 0xFFE8EAED, false);

        for (int i = 0; i < chests.size(); i++) {
            BlockPos pos = chests.get(i);
            boolean full = fullMarked.contains(pos);
            boolean active = isEnabled() && (i == srcIndex || i == srcIndex + 1);
            int col = full ? 0xFFFF4466 : (active ? 0xFF00E8A0 : 0xFF6B8CFF);

            double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE, maxX = -Double.MAX_VALUE, maxY = -Double.MAX_VALUE;
            int vis = 0;
            for (int c = 0; c < 8; c++) {
                double[] p = new double[2];
                if (!EspProjection.worldToScreen(
                        pos.getX() + ((c & 1) == 0 ? 0.05 : 0.95),
                        pos.getY() + ((c & 2) == 0 ? 0.05 : 0.95),
                        pos.getZ() + ((c & 4) == 0 ? 0.05 : 0.95), p)) continue;
                minX = Math.min(minX, p[0]); minY = Math.min(minY, p[1]);
                maxX = Math.max(maxX, p[0]); maxY = Math.max(maxY, p[1]);
                vis++;
            }
            if (vis < 2) continue;
            EspDraw.box(g, minX, minY, maxX, maxY, col | 0xFF000000, full ? 2 : 1);
            String label = "#" + (i + 1) + (full ? " FULL" : "");
            EspDraw.label(g, mc.font, label, (minX + maxX) * 0.5, minY - 10, col | 0xFF000000);
        }
    }
}
