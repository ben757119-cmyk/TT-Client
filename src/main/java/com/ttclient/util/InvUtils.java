package com.ttclient.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
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
        if (invIndex >= 0 && invIndex < 9) return 36 + invIndex;
        if (invIndex >= 9 && invIndex < 36) return invIndex;
        if (invIndex == 40) return 45;
        return -1;
    }

    public static final int OFFHAND_MENU_SLOT = 45;

    /**
 * Server-synced swap into offhand.
     * Uses reflection so we work across ClickType / SlotActionType naming.
     */
    public static boolean swapToOffhand(LocalPlayer p, int invIndex) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.gameMode == null || p == null) return false;
        int menuSlot = toMenuSlot(invIndex);
        if (menuSlot < 0) return false;

        int syncId;
        try {
            syncId = p.containerMenu.containerId;
        } catch (Throwable t) {
            return false;
        }

        // Try ClickType then SlotActionType (loader/mapping differences)
        if (invokeInvClick(mc, syncId, menuSlot, 40, "net.minecraft.world.inventory.ClickType", "SWAP", p))
            return true;
        if (invokeInvClick(mc, syncId, menuSlot, 40, "net.minecraft.world.inventory.SlotActionType", "SWAP", p))
            return true;

        // Fallback: PICKUP path
        if (invokeInvClick(mc, syncId, menuSlot, 0, "net.minecraft.world.inventory.ClickType", "PICKUP", p)
                && invokeInvClick(mc, syncId, OFFHAND_MENU_SLOT, 0, "net.minecraft.world.inventory.ClickType", "PICKUP", p)) {
            invokeInvClick(mc, syncId, menuSlot, 0, "net.minecraft.world.inventory.ClickType", "PICKUP", p);
            return true;
        }
        if (invokeInvClick(mc, syncId, menuSlot, 0, "net.minecraft.world.inventory.SlotActionType", "PICKUP", p)
                && invokeInvClick(mc, syncId, OFFHAND_MENU_SLOT, 0, "net.minecraft.world.inventory.SlotActionType", "PICKUP", p)) {
            invokeInvClick(mc, syncId, menuSlot, 0, "net.minecraft.world.inventory.SlotActionType", "PICKUP", p);
            return true;
        }

        // Last resort: select hotbar only (user can press F)
        if (invIndex >= 0 && invIndex < 9) selectHotbar(p, invIndex);
        return false;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static boolean invokeInvClick(Minecraft mc, int syncId, int slot, int button,
                                          String enumClass, String enumName, Player p) {
        try {
            Class<?> ctCls = Class.forName(enumClass);
            Object action = Enum.valueOf((Class<? extends Enum>) ctCls.asSubclass(Enum.class), enumName);
            for (var m : mc.gameMode.getClass().getMethods()) {
                if (!m.getName().equals("handleInventoryMouseClick") && !m.getName().equals("handleContainerClick"))
                    continue;
                if (m.getParameterCount() != 5) continue;
                Class<?>[] pts = m.getParameterTypes();
                if (pts[0] != int.class && pts[0] != Integer.TYPE) continue;
                m.invoke(mc.gameMode, syncId, slot, button, action, p);
                return true;
            }
        } catch (Throwable ignored) {}
        return false;
    }

    public static int findItemSlot(LocalPlayer p, Item item) {
        Inventory inv = p.getInventory();
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

    /** NeoForge 26.2: screen is on gui, not Minecraft.screen */
    public static boolean hasScreen(Minecraft mc) {
        if (mc == null) return false;
        try {
            return mc.gui.screen() != null;
        } catch (Throwable t) {
            try {
                Object s = Minecraft.class.getMethod("getScreen").invoke(mc);
                return s != null;
            } catch (Throwable ignored) {
                return false;
            }
        }
    }

    public static Minecraft mc() {
        return Minecraft.getInstance();
    }
}
