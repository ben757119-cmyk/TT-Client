package com.ttclient.modules.player;

import com.ttclient.modules.Category;
import com.ttclient.modules.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

public class AutoArmor extends Module {
    private int timer;

    public AutoArmor() {
        super("AutoArmor", "Equip best armor from hotbar/inventory", Category.PLAYER);
    }

    @Override
    public void onTick() {
        Minecraft mc = mc();
        if (mc == null || mc.player == null || mc.gameMode == null) return;
        if (timer > 0) { timer--; return; }
        LocalPlayer p = mc.player;
        // Simple: if a hotbar slot has armor and equipment slot empty, use it
        for (int i = 0; i < 9; i++) {
            ItemStack stack = p.getInventory().getItem(i);
            if (stack.isEmpty()) continue;
            EquipmentSlot slot = null;
            try {
                // Prefer modern equippable component path via reflection-safe checks
                String name = stack.getItem().toString().toLowerCase();
                if (name.contains("helmet") || name.contains("skull")) slot = EquipmentSlot.HEAD;
                else if (name.contains("chestplate") || name.contains("elytra")) slot = EquipmentSlot.CHEST;
                else if (name.contains("leggings")) slot = EquipmentSlot.LEGS;
                else if (name.contains("boots")) slot = EquipmentSlot.FEET;
            } catch (Exception ignored) {}
            if (slot == null) continue;
            if (!p.getItemBySlot(slot).isEmpty()) continue;
            p.getInventory().setSelectedSlot(i);
            // right-click to equip wearable from hotbar when possible
            mc.gameMode.useItem(p, net.minecraft.world.InteractionHand.MAIN_HAND);
            timer = 5;
            return;
        }
    }
}
