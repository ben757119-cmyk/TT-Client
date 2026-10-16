package com.ttclient.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/** Hotbar / offhand helpers — server-synced where possible. */
public final class InvUtils {
    private InvUtils() {}

    public static final int OFFHAND_MENU_SLOT = 45;

    public static int findHotbar(LocalPlayer p, Item item) {
        Inventory inv = p.getInventory();
        for (int i = 0; i < 9; i++) {
            if (inv.getItem(i).is(item)) return i;
        }
        return -1;
    }

    public static int findHotbar(LocalPlayer p, Item... items) {
        Inventory inv = p.getInventory();
        for (int i = 0; i < 9; i++) {
            ItemStack s = inv.getItem(i);
            for (Item it : items) if (s.is(it)) return i;
        }
        return -1;
    }

    public static boolean isMace(ItemStack s) {
        if (s == null || s.isEmpty()) return false;
        try {
            return s.is(Items.MACE);
        } catch (Throwable t) {
            return s.getItem().toString().toLowerCase().contains("mace");
        }
    }

    public static int findHotbarMace(LocalPlayer p) {
        Inventory inv = p.getInventory();
        for (int i = 0; i < 9; i++) {
            if (isMace(inv.getItem(i))) return i;
        }
        return -1;
    }

    public static boolean selectHotbar(LocalPlayer p, int slot) {
        if (slot < 0 || slot > 8) return false;
        p.getInventory().setSelectedSlot(slot);
        return true;
    }

    public static void setOffhand(LocalPlayer p, ItemStack stack) {
        try {
            p.setItemInHand(InteractionHand.OFF_HAND, stack);
        } catch (Throwable ignored) {}
    }

    /** Inventory index → player InventoryMenu slot id. */
    public static int toMenuSlot(int invIndex) {
        if (invIndex >= 0 && invIndex < 9) return 36 + invIndex; // hotbar
        if (invIndex >= 9 && invIndex < 36) return invIndex;     // main
        if (invIndex == 40) return 45;                           // offhand
        return -1;
    }

    /** First inventory slot with item (hotbar preferred). */
    public static int findItemSlot(LocalPlayer p, Item item) {
        Inventory inv = p.getInventory();
        for (int i = 0; i < 9; i++) {
            if (inv.getItem(i).is(item)) return i;
        }
        int size = inv.getContainerSize();
        for (int i = 9; i < size && i < 36; i++) {
            if (inv.getItem(i).is(item)) return i;
        }
        // Some mappings put main inv higher — scan rest except armor/offhand
        for (int i = 36; i < size; i++) {
            if (i >= 36 && i <= 40) continue;
            if (inv.getItem(i).is(item)) return i;
        }
        return -1;
    }

    public static int findBestWeapon(LocalPlayer p) {
        Inventory inv = p.getInventory();
        int best = -1;
        float bestScore = -1;
        for (int i = 0; i < 9; i++) {
            ItemStack s = inv.getItem(i);
            if (s.isEmpty()) continue;
            float score = 0;
            String n = s.getItem().toString().toLowerCase();
            if (isMace(s)) score = 100;
            else if (n.contains("netherite_sword")) score = 90;
            else if (n.contains("diamond_sword")) score = 80;
            else if (n.contains("sword")) score = 70;
            else if (n.contains("netherite_axe")) score = 65;
            else if (n.contains("axe")) score = 50;
            else if (n.contains("trident")) score = 60;
            if (score > bestScore) {
                bestScore = score;
                best = i;
            }
        }
        return best;
    }

    public static boolean hasScreen(Minecraft mc) {
        if (mc == null) return false;
        try {
            return mc.gui.screen() != null;
        } catch (Throwable t) {
            try {
                return Minecraft.class.getMethod("getScreen").invoke(mc) != null;
            } catch (Throwable ignored) {
                return false;
            }
        }
    }

    /**
 * Equip item into offhand with real server packets.
     * 1) Hotbar + SWAP_ITEM_WITH_OFFHAND (most reliable)
     * 2) Inventory click SWAP / PICKUP fallback
     */
    public static boolean swapToOffhand(LocalPlayer p, int invIndex) {
        if (p == null || invIndex < 0) return false;
        ItemStack stack = p.getInventory().getItem(invIndex);
        if (stack.isEmpty()) return false;

        // Already offhand
        if (invIndex == 40 || p.getOffhandItem().getItem() == stack.getItem()
                && p.getOffhandItem().getCount() == stack.getCount()) {
            if (!p.getOffhandItem().isEmpty() && invIndex == 40) return true;
        }

        // Path A: item already on hotbar → F-key equivalent packet
        if (invIndex >= 0 && invIndex < 9) {
            if (swapHotbarOffhand(p, invIndex)) return true;
        }

        // Path B: move from main inv via container clicks onto offhand
        if (clickSwapToOffhand(p, invIndex)) return true;

        // Path C: move inv item onto a hotbar slot then F-swap
        if (invIndex >= 9) {
            int empty = findEmptyHotbar(p);
            if (empty >= 0 && clickSwapSlots(p, invIndex, empty)) {
                return swapHotbarOffhand(p, empty);
            }
        }

        return false;
    }

    public static boolean moveToOffhand(LocalPlayer p, Item item) {
        if (p.getOffhandItem().is(item)) return true;
        int slot = findItemSlot(p, item);
        if (slot < 0) return false;
        return swapToOffhand(p, slot);
    }

    /** Dedicated totem equip used by AutoTotem / Destroyer. */
    public static boolean equipTotem(LocalPlayer p) {
        if (p == null) return false;
        if (p.getOffhandItem().is(Items.TOTEM_OF_UNDYING)) return true;
        int slot = findItemSlot(p, Items.TOTEM_OF_UNDYING);
        if (slot < 0) return false;
        return swapToOffhand(p, slot);
    }

    private static int findEmptyHotbar(LocalPlayer p) {
        Inventory inv = p.getInventory();
        for (int i = 0; i < 9; i++) {
            if (inv.getItem(i).isEmpty()) return i;
        }
        return -1;
    }

    /**
 * Select hotbar slot and send SWAP_ITEM_WITH_OFFHAND (same as pressing F).
     */
    public static boolean swapHotbarOffhand(LocalPlayer p, int hotbarSlot) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || hotbarSlot < 0 || hotbarSlot > 8) return false;

        int prev = p.getInventory().getSelectedSlot();
        selectHotbar(p, hotbarSlot);

        // Soft client prediction (matches what vanilla does on F)
        try {
            ItemStack main = p.getMainHandItem().copy();
            ItemStack off = p.getOffhandItem().copy();
            p.setItemInHand(InteractionHand.MAIN_HAND, off);
            p.setItemInHand(InteractionHand.OFF_HAND, main);
        } catch (Throwable ignored) {}

        boolean ok = sendSwapOffhandPacket(p);

        // Restore selected hotbar (weapon) after swap
        selectHotbar(p, prev);
        return ok;
    }

    private static boolean sendSwapOffhandPacket(LocalPlayer p) {
        try {
            Object conn = p.connection;
            if (conn == null) return false;

            Class<?> pktCls = Class.forName("net.minecraft.network.protocol.game.ServerboundPlayerActionPacket");
            Class<?> actionCls = Class.forName("net.minecraft.network.protocol.game.ServerboundPlayerActionPacket$Action");
            @SuppressWarnings({ "unchecked", "rawtypes" })
            Object action = Enum.valueOf((Class<? extends Enum>) actionCls.asSubclass(Enum.class), "SWAP_ITEM_WITH_OFFHAND");

            Object packet = null;
            // Try constructors: (Action, BlockPos, Direction) and (Action, BlockPos, Direction, int)
            for (Constructor<?> c : pktCls.getConstructors()) {
                Class<?>[] t = c.getParameterTypes();
                try {
                    if (t.length == 3) {
                        packet = c.newInstance(action, BlockPos.ZERO, Direction.DOWN);
                        break;
                    }
                    if (t.length == 4 && t[3] == int.class) {
                        packet = c.newInstance(action, BlockPos.ZERO, Direction.DOWN, 0);
                        break;
                    }
                } catch (Throwable ignored) {}
            }
            if (packet == null) return false;

            // connection.send(Packet)
            for (Method m : conn.getClass().getMethods()) {
                if (!m.getName().equals("send") || m.getParameterCount() != 1) continue;
                try {
                    m.invoke(conn, packet);
                    return true;
                } catch (Throwable ignored) {}
            }
        } catch (Throwable ignored) {}
        return false;
    }

    /** Inventory-menu click: SWAP with button 40 = offhand (vanilla). */
    private static boolean clickSwapToOffhand(LocalPlayer p, int invIndex) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.gameMode == null) return false;
        int menuSlot = toMenuSlot(invIndex);
        if (menuSlot < 0) return false;

        int syncId;
        try {
            syncId = p.containerMenu.containerId;
        } catch (Throwable t) {
            return false;
        }

        // Preferred: ClickType.SWAP + button 40 (offhand)
        if (invClick(mc, syncId, menuSlot, 40, "SWAP", p)) return true;

        // Fallback: PICKUP from slot → PICKUP offhand → PICKUP back
        if (invClick(mc, syncId, menuSlot, 0, "PICKUP", p)
                && invClick(mc, syncId, OFFHAND_MENU_SLOT, 0, "PICKUP", p)) {
            invClick(mc, syncId, menuSlot, 0, "PICKUP", p);
            return true;
        }
        return false;
    }

    private static boolean clickSwapSlots(LocalPlayer p, int fromInv, int toHotbar) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.gameMode == null) return false;
        int from = toMenuSlot(fromInv);
        int to = toMenuSlot(toHotbar);
        if (from < 0 || to < 0) return false;
        int syncId;
        try {
            syncId = p.containerMenu.containerId;
        } catch (Throwable t) {
            return false;
        }
        // SWAP with hotbar button 0-8
        if (invClick(mc, syncId, from, toHotbar, "SWAP", p)) return true;
        // PICKUP path
        if (invClick(mc, syncId, from, 0, "PICKUP", p)
                && invClick(mc, syncId, to, 0, "PICKUP", p)) {
            invClick(mc, syncId, from, 0, "PICKUP", p);
            return true;
        }
        return false;
    }

    private static boolean invClick(Minecraft mc, int syncId, int slot, int button, String typeName, Player p) {
        String[] enumClasses = {
                "net.minecraft.world.inventory.ClickType",
                "net.minecraft.world.inventory.SlotActionType"
        };
        for (String enumClass : enumClasses) {
            try {
                Class<?> ctCls = Class.forName(enumClass);
                @SuppressWarnings({ "unchecked", "rawtypes" })
                Object action = Enum.valueOf((Class<? extends Enum>) ctCls.asSubclass(Enum.class), typeName);
                for (Method m : mc.gameMode.getClass().getMethods()) {
                    if (!m.getName().equals("handleInventoryMouseClick")
                            && !m.getName().equals("handleContainerClick"))
                        continue;
                    if (m.getParameterCount() != 5) continue;
                    try {
                        m.invoke(mc.gameMode, syncId, slot, button, action, p);
                        return true;
                    } catch (Throwable ignored) {}
                }
            } catch (Throwable ignored) {}
        }
        return false;
    }

    public static Minecraft mc() {
        return Minecraft.getInstance();
    }
}
