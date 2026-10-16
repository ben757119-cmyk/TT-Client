package com.ttclient.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Hotbar / offhand helpers for combat modules. */
public final class InvUtils {
    private InvUtils() {}

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
            String n = s.getItem().toString().toLowerCase();
            return n.contains("mace");
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

    /** Inventory index (0-40) → InventoryMenu slot id. */
    public static int toMenuSlot(int invIndex) {
        if (invIndex >= 0 && invIndex < 9) return 36 + invIndex; // hotbar
        if (invIndex >= 9 && invIndex < 36) return invIndex;     // main
        if (invIndex >= 36 && invIndex < 40) return 5 + (invIndex - 36); // armor rough
        if (invIndex == 40) return 45; // offhand
        return -1;
    }

    public static final int OFFHAND_MENU_SLOT = 45;

    /**
 * Server-synced swap of inv slot into offhand via ClickType.SWAP (button 40).
     * Does NOT client-side ghost items.
     */
    public static boolean swapToOffhand(LocalPlayer p, int invIndex) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.gameMode == null || p == null) return false;
        int menuSlot = toMenuSlot(invIndex);
        if (menuSlot < 0) return false;
        try {
            int syncId = p.containerMenu.containerId;
            mc.gameMode.handleInventoryMouseClick(syncId, menuSlot, 40, ClickType.SWAP, p);
            return true;
        } catch (Throwable t) {
            try {
                // Fallback: pickup → offhand → put back
                int syncId = p.containerMenu.containerId;
                mc.gameMode.handleInventoryMouseClick(syncId, menuSlot, 0, ClickType.PICKUP, p);
                mc.gameMode.handleInventoryMouseClick(syncId, OFFHAND_MENU_SLOT, 0, ClickType.PICKUP, p);
                mc.gameMode.handleInventoryMouseClick(syncId, menuSlot, 0, ClickType.PICKUP, p);
                return true;
            } catch (Throwable ignored) {
                return false;
            }
        }
    }

    /** Find first inventory slot containing item (0 = hotbar start). */
    public static int findItemSlot(LocalPlayer p, Item item) {
        Inventory inv = p.getInventory();
        // Prefer hotbar first
        for (int i = 0; i < 9; i++) {
            if (inv.getItem(i).is(item)) return i;
        }
        for (int i = 9; i < inv.getContainerSize(); i++) {
            if (inv.getItem(i).is(item)) return i;
        }
        return -1;
    }

    public static boolean moveToOffhand(LocalPlayer p, Item item) {
        if (p.getOffhandItem().is(item)) return true;
        int slot = findItemSlot(p, item);
        if (slot < 0) return false;
        return swapToOffhand(p, slot);
    }

    /** Prefer sword/axe/mace in hotbar. */
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

    public static Minecraft mc() {
        return Minecraft.getInstance();
    }
}
