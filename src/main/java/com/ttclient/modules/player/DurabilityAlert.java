package com.ttclient.modules.player;

import com.ttclient.modules.Category;
import com.ttclient.modules.Module;
import com.ttclient.settings.NumberSetting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.HashSet;
import java.util.Set;

public class DurabilityAlert extends Module {
    public final NumberSetting percent = addSetting(new NumberSetting("Percent", "Warn when remaining durability is at or below this %", 10, 1, 50, 1));

    private final Set<Integer> warned = new HashSet<>();

    public DurabilityAlert() {
        super("DurabilityAlert", "Chat warning when armor or held tools are almost broken", Category.PLAYER);
        setEnabled(true);
    }

    @Override
    public void onTick() {
        if (mc.player == null) return;
        check(mc.player.getMainHandItem());
        check(mc.player.getOffhandItem());
        for (ItemStack stack : mc.player.getArmorSlots()) {
            check(stack);
        }
    }

    private void check(ItemStack stack) {
        if (stack.isEmpty() || !stack.isDamageableItem()) return;
        int max = stack.getMaxDamage();
        if (max <= 0) return;
        int rem = max - stack.getDamageValue();
        float pct = rem * 100f / max;
        int id = System.identityHashCode(stack) ^ stack.getItem().hashCode();
        if (pct <= percent.get()) {
            if (warned.add(id) && mc.player != null) {
                mc.player.displayClientMessage(
                        Component.literal("[TT] " + stack.getHoverName().getString() + " at " + (int) pct + "%"),
                        false);
            }
        } else {
            warned.remove(id);
        }
    }
}
