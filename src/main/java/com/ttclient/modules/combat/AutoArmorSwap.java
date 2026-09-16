package com.ttclient.modules.combat;

import com.ttclient.modules.Category;
import com.ttclient.modules.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

/**
 * Quickly swaps between elytra and chestplate in the hotbar.
 */
public class AutoArmorSwap extends Module {
    private int cooldown;

    public AutoArmorSwap() {
        super("AutoArmorSwap", "Swap elytra and chestplate from hotbar", Category.COMBAT);
    }

    @Override
    public void onTick() {
        Minecraft mc = mc();
        if (mc == null || mc.player == null || mc.gameMode == null || mc.options == null) return;
        if (cooldown > 0) {
            cooldown--;
            return;
        }

        // Activate when jump is held while on ground with elytra in hotbar, or when falling with chestplate
        LocalPlayer p = mc.player;
        ItemStack chest = p.getItemBySlot(EquipmentSlot.CHEST);
        String chestName = chest.getItem().toString().toLowerCase();

        boolean wearingElytra = chestName.contains("elytra");
        int elytraSlot = findHotbar(p, "elytra");
        int plateSlot = findHotbarChestplate(p);

        // If falling far and wearing chestplate, swap to elytra
        if (!wearingElytra && elytraSlot >= 0 && !p.onGround() && p.fallDistance > 1.5f) {
            swapEquip(mc, p, elytraSlot);
            cooldown = 8;
            return;
        }

        // If on ground wearing elytra and have a chestplate, swap back when not jumping
        if (wearingElytra && plateSlot >= 0 && p.onGround() && !mc.options.keyJump.isDown()) {
            swapEquip(mc, p, plateSlot);
            cooldown = 8;
        }
    }

    private void swapEquip(Minecraft mc, LocalPlayer p, int hotbarSlot) {
        int prev = p.getInventory().getSelectedSlot();
        p.getInventory().setSelectedSlot(hotbarSlot);
        mc.gameMode.useItem(p, InteractionHand.MAIN_HAND);
        p.getInventory().setSelectedSlot(prev);
    }

    private static int findHotbar(LocalPlayer p, String part) {
        for (int i = 0; i < 9; i++) {
            ItemStack s = p.getInventory().getItem(i);
            if (!s.isEmpty() && s.getItem().toString().toLowerCase().contains(part)) return i;
        }
        return -1;
    }

    private static int findHotbarChestplate(LocalPlayer p) {
        for (int i = 0; i < 9; i++) {
            ItemStack s = p.getInventory().getItem(i);
            if (s.isEmpty()) continue;
            String n = s.getItem().toString().toLowerCase();
            if (n.contains("chestplate")) return i;
        }
        return -1;
    }
}
