package com.ttclient.modules.player;

import com.ttclient.client.NotificationManager;
import com.ttclient.modules.Category;
import com.ttclient.modules.Module;
import com.ttclient.settings.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

public class DurabilityAlert extends Module {
    public final NumberSetting percent = addSetting(new NumberSetting("Percent", "Warn below this %", 15, 5, 50, 5));
    private int cooldown;

    public DurabilityAlert() {
        super("DurabilityAlert", "Warn when gear is low", Category.PLAYER);
    }

    @Override
    public void onTick() {
        Minecraft mc = mc();
        if (mc == null || mc.player == null) return;
        if (cooldown > 0) { cooldown--; return; }

        EquipmentSlot[] slots = {
                EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND,
                EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
        };
        for (EquipmentSlot slot : slots) {
            ItemStack stack = mc.player.getItemBySlot(slot);
            if (stack.isEmpty() || !stack.isDamageableItem()) continue;
            int max = stack.getMaxDamage();
            if (max <= 0) continue;
            int left = max - stack.getDamageValue();
            double pct = (left * 100.0) / max;
            if (pct <= percent.get()) {
                NotificationManager.push("Durability", stack.getHoverName().getString() + " low");
                cooldown = 100;
                return;
            }
        }
    }
}
