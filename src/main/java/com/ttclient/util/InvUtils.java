package com.ttclient.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
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

    /** Best-effort offhand set (client prediction; servers may resync). */
    public static void setOffhand(LocalPlayer p, ItemStack stack) {
        try {
            p.setItemInHand(InteractionHand.OFF_HAND, stack);
        } catch (Throwable ignored) {}
    }

    public static boolean moveToOffhand(LocalPlayer p, Item item) {
        if (p.getOffhandItem().is(item)) return true;
        Inventory inv = p.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (!stack.is(item)) continue;
            ItemStack copy = stack.copy();
            setOffhand(p, copy);
            inv.setItem(i, ItemStack.EMPTY);
            return true;
        }
        return false;
    }

    public static Minecraft mc() {
        return Minecraft.getInstance();
    }
}
