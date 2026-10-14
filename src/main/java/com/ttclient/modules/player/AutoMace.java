package com.ttclient.modules.player;

import com.ttclient.client.NotificationManager;
import com.ttclient.modules.Category;
import com.ttclient.modules.Module;
import com.ttclient.settings.BoolSetting;
import com.ttclient.settings.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * Helps obtain and equip maces from ominous vault runs.
 *
 * Important: vault loot is decided by the server. This module cannot force
 * a mace drop on multiplayer. It will:
 *  - auto-use an Ominous Trial Key on an ominous vault you're looking at
 *  - move any mace that appears in your inventory into a hotbar slot
 *  - select that hotbar slot
 */
public class AutoMace extends Module {
    public final BoolSetting autoKey = addSetting(new BoolSetting(
            "AutoKey", "Use ominous trial key on ominous vaults", true));
    public final BoolSetting autoHotbar = addSetting(new BoolSetting(
            "AutoHotbar", "Move mace into hotbar when you get one", true));
    public final NumberSetting slot = addSetting(new NumberSetting(
            "Slot", "Preferred hotbar slot (1-9)", 1, 1, 9, 1));
    public final BoolSetting notify = addSetting(new BoolSetting(
            "Notify", "Chat/toast when a mace is secured", true));

    private int keyCooldown;
    private int swapCooldown;
    private boolean hadMace;

    public AutoMace() {
        super("AutoMace",
                "Auto-key ominous vaults and hotbar any mace you receive",
                Category.PLAYER);
    }

    @Override
    public void onEnable() {
        keyCooldown = 0;
        swapCooldown = 0;
        Minecraft mc = mc();
        hadMace = mc != null && mc.player != null && findMaceSlot(mc.player) >= 0;
    }

    @Override
    public void onTick() {
        Minecraft mc = mc();
        if (mc == null || mc.player == null || mc.gameMode == null || mc.level == null) return;
        LocalPlayer player = mc.player;

        if (keyCooldown > 0) keyCooldown--;
        if (swapCooldown > 0) swapCooldown--;

        if (autoKey.get() && keyCooldown <= 0) {
            tryUseKeyOnVault(mc, player);
        }

        if (autoHotbar.get() && swapCooldown <= 0) {
            int maceInv = findMaceSlot(player);
            if (maceInv >= 0) {
                if (!hadMace) {
                    hadMace = true;
                    if (notify.get()) {
                        NotificationManager.push("AutoMace", "Mace secured");
                        player.displayClientMessage(
                                Component.literal("§a[TT] §fAutoMace: §eMace in inventory"), false);
                    }
                }
                ensureMaceInHotbar(mc, player, maceInv);
            } else {
                hadMace = false;
            }
        }
    }

    private void tryUseKeyOnVault(Minecraft mc, LocalPlayer player) {
        if (!(mc.hitResult instanceof BlockHitResult bhr)) return;
        if (mc.hitResult.getType() != HitResult.Type.BLOCK) return;

        BlockState state = mc.level.getBlockState(bhr.getBlockPos());
        String id = blockPath(state);
        if (!id.equals("ominous_vault") && !id.contains("ominous_vault")) return;

        int keySlot = findItemSlot(player, "ominous_trial_key");
        if (keySlot < 0) return;

        int prev = player.getInventory().getSelectedSlot();
        if (keySlot < 9) {
            player.getInventory().setSelectedSlot(keySlot);
        } else {
            // Prefer swapping key into preferred hotbar briefly
            int prefer = slot.getInt() - 1;
            swapSlots(mc, player, keySlot, prefer);
            player.getInventory().setSelectedSlot(prefer);
        }

        mc.gameMode.useItemOn(player, InteractionHand.MAIN_HAND, bhr);
        player.getInventory().setSelectedSlot(prev);
        keyCooldown = 15;
    }

    private void ensureMaceInHotbar(Minecraft mc, LocalPlayer player, int maceSlot) {
        int prefer = slot.getInt() - 1;
        Inventory inv = player.getInventory();

        // Already on preferred hotbar slot
        if (maceSlot == prefer) {
            inv.setSelectedSlot(prefer);
            return;
        }

        // Mace already in hotbar — just select it
        if (maceSlot >= 0 && maceSlot < 9) {
            inv.setSelectedSlot(maceSlot);
            return;
        }

        // Move from main inventory into preferred hotbar slot
        swapSlots(mc, player, maceSlot, prefer);
        inv.setSelectedSlot(prefer);
        swapCooldown = 8;
    }

    private void swapSlots(Minecraft mc, LocalPlayer player, int from, int toHotbar) {
        try {
            // InventoryMenu: hotbar 0-8 -> container slots 36-44, main 9-35 -> 9-35
            int fromContainer = toContainerSlot(from);
            int toContainer = 36 + toHotbar;
            int menuId = player.inventoryMenu.containerId;
            mc.gameMode.handleInventoryMouseClick(menuId, fromContainer, 0, ClickType.PICKUP, player);
            mc.gameMode.handleInventoryMouseClick(menuId, toContainer, 0, ClickType.PICKUP, player);
            mc.gameMode.handleInventoryMouseClick(menuId, fromContainer, 0, ClickType.PICKUP, player);
        } catch (Throwable ignored) {
            // Fallback: if mace somehow in hotbar range only
        }
    }

    private static int toContainerSlot(int invSlot) {
        if (invSlot < 9) return 36 + invSlot;
        return invSlot; // 9-35 map 1:1 in InventoryMenu
    }

    private static int findMaceSlot(LocalPlayer player) {
        return findItemSlot(player, "mace");
    }

    private static int findItemSlot(LocalPlayer player, String pathPart) {
        Inventory inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (stack.isEmpty()) continue;
            if (itemPath(stack).contains(pathPart)) return i;
        }
        return -1;
    }

    private static String itemPath(ItemStack stack) {
        try {
            ResourceLocation rl = BuiltInRegistries.ITEM.getKey(stack.getItem());
            return rl != null ? rl.getPath().toLowerCase() : stack.getItem().toString().toLowerCase();
        } catch (Throwable t) {
            return stack.getItem().toString().toLowerCase();
        }
    }

    private static String blockPath(BlockState state) {
        try {
            ResourceLocation rl = BuiltInRegistries.BLOCK.getKey(state.getBlock());
            return rl != null ? rl.getPath().toLowerCase() : "";
        } catch (Throwable t) {
            return state.getBlock().toString().toLowerCase();
        }
    }
}
