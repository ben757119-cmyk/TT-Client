package com.ttclient.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.lang.reflect.Method;

/**
 * Hotbar / offhand helpers with real server packets (no ghost swaps).
 * Inventory clicks use reflection so we work whether the enum is ClickType or SlotActionType.
 */
public final class InvUtils {
    private InvUtils() {}

    public static final int OFFHAND_MENU_SLOT = 45;

    public static Minecraft mc() {
        return Minecraft.getInstance();
    }

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

    public static boolean selectHotbar(LocalPlayer p, int slot) {
        if (slot < 0 || slot > 8) return false;
        p.getInventory().setSelectedSlot(slot);
        try {
            p.connection.send(new ServerboundSetCarriedItemPacket(slot));
        } catch (Throwable ignored) {}
        return true;
    }

    public static void setOffhand(LocalPlayer p, ItemStack stack) {
        try {
            p.setItemInHand(InteractionHand.OFF_HAND, stack);
        } catch (Throwable ignored) {}
    }

    public static int toMenuSlot(int invIndex) {
        if (invIndex >= 0 && invIndex < 9) return 36 + invIndex;
        if (invIndex >= 9 && invIndex < 36) return invIndex;
        if (invIndex == 40) return 45;
        return -1;
    }

    public static int findItemSlot(LocalPlayer p, Item item) {
        Inventory inv = p.getInventory();
        for (int i = 0; i < 9; i++) {
            if (inv.getItem(i).is(item)) return i;
        }
        for (int i = 9; i < 36; i++) {
            if (inv.getItem(i).is(item)) return i;
        }
        try {
            if (inv.getItem(40).is(item)) return 40;
        } catch (Throwable ignored) {}
        return -1;
    }

    public static int findEmptyHotbar(LocalPlayer p) {
        Inventory inv = p.getInventory();
        for (int i = 0; i < 9; i++) {
            if (inv.getItem(i).isEmpty()) return i;
        }
        return -1;
    }

    public static boolean hasScreen(Minecraft mc) {
        if (mc == null) return false;
        try {
            return mc.gui.screen() != null;
        } catch (Throwable t) {
            return false;
        }
    }

    public static boolean equipTotem(LocalPlayer p) {
        if (p == null) return false;
        if (p.getOffhandItem().is(Items.TOTEM_OF_UNDYING)) return true;
        int slot = findItemSlot(p, Items.TOTEM_OF_UNDYING);
        if (slot < 0) return false;
        return swapToOffhand(p, slot);
    }

    public static boolean moveToOffhand(LocalPlayer p, Item item) {
        if (p.getOffhandItem().is(item)) return true;
        int slot = findItemSlot(p, item);
        if (slot < 0) return false;
        return swapToOffhand(p, slot);
    }

    public static boolean swapToOffhand(LocalPlayer p, int invIndex) {
        if (p == null || invIndex < 0) return false;
        if (p.getInventory().getItem(invIndex).isEmpty()) return false;

        if (invIndex >= 0 && invIndex < 9) {
            return swapHotbarOffhand(p, invIndex);
        }

        if (clickSwapToOffhand(p, invIndex)) return true;

        int empty = findEmptyHotbar(p);
        if (empty >= 0 && clickSwapSlots(p, invIndex, empty)) {
            return swapHotbarOffhand(p, empty);
        }
        return false;
    }

    public static boolean swapHotbarOffhand(LocalPlayer p, int hotbarSlot) {
        if (hotbarSlot < 0 || hotbarSlot > 8) return false;
        if (!selectHotbar(p, hotbarSlot)) return false;
        return sendSwapOffhandPacket(p);
    }

    public static boolean sendSwapOffhandPacket(LocalPlayer p) {
        try {
            p.connection.send(new ServerboundPlayerActionPacket(
                    ServerboundPlayerActionPacket.Action.SWAP_ITEM_WITH_OFFHAND,
                    BlockPos.ZERO,
                    Direction.DOWN));
            return true;
        } catch (Throwable t) {
            try {
                var ctor = ServerboundPlayerActionPacket.class.getConstructor(
                        ServerboundPlayerActionPacket.Action.class,
                        BlockPos.class,
                        Direction.class,
                        int.class);
                Object pkt = ctor.newInstance(
                        ServerboundPlayerActionPacket.Action.SWAP_ITEM_WITH_OFFHAND,
                        BlockPos.ZERO,
                        Direction.DOWN,
                        0);
                p.connection.send((ServerboundPlayerActionPacket) pkt);
                return true;
            } catch (Throwable t2) {
                return false;
            }
        }
    }

    public static boolean clickSwapToOffhand(LocalPlayer p, int invIndex) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.gameMode == null) return false;
        int menuSlot = toMenuSlot(invIndex);
        if (menuSlot < 0) return false;

        int syncId = inventorySyncId(p);
        if (syncId < 0) return false;

        if (invClick(mc, syncId, menuSlot, 40, "SWAP", p)) return true;
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
        if (from < 0 || toHotbar < 0 || toHotbar > 8) return false;
        int syncId = inventorySyncId(p);
        if (syncId < 0) return false;
        return invClick(mc, syncId, from, toHotbar, "SWAP", p);
    }

    private static int inventorySyncId(LocalPlayer p) {
        try {
            return p.inventoryMenu.containerId;
        } catch (Throwable t) {
            try {
                return p.containerMenu.containerId;
            } catch (Throwable t2) {
                return -1;
            }
        }
    }

    private static boolean invClick(Minecraft mc, int syncId, int slot, int button, String typeName, Player p) {
        String[] enumClasses = {
                "net.minecraft.world.inventory.SlotActionType",
                "net.minecraft.world.inventory.ClickType"
        };
        Object action = null;
        for (String enumClass : enumClasses) {
            try {
                Class<?> ctCls = Class.forName(enumClass);
                @SuppressWarnings({ "unchecked", "rawtypes" })
                Object a = Enum.valueOf((Class<? extends Enum>) ctCls.asSubclass(Enum.class), typeName);
                action = a;
                break;
            } catch (Throwable ignored) {}
        }
        if (action == null) return false;

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
        return false;
    }
}
