package com.ttclient.modules.player;

import com.ttclient.modules.Category;
import com.ttclient.modules.Module;
import com.ttclient.settings.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;

public class ChestStealer extends Module {
    public final NumberSetting delay = addSetting(new NumberSetting("Delay", "Ticks between takes", 2, 0, 10, 1));
    private int timer;

    public ChestStealer() {
        super("ChestStealer", "Loot open containers", Category.PLAYER);
    }

    @Override
    public void onTick() {
        Minecraft mc = mc();
        if (mc == null || mc.player == null || mc.gameMode == null) return;
        if (!(mc.gui.screen() instanceof ContainerScreen screen)) return;
        if (timer > 0) { timer--; return; }

        AbstractContainerMenu menu = screen.getMenu();
        int chestSlots = Math.max(0, menu.slots.size() - 36);
        for (int i = 0; i < chestSlots; i++) {
            Slot slot = menu.slots.get(i);
            if (slot != null && slot.hasItem()) {
                try {
                    mc.gameMode.handleInventoryMouseClick(
                            menu.containerId,
                            i,
                            0,
                            net.minecraft.world.inventory.ClickType.QUICK_MOVE,
                            mc.player
                    );
                } catch (Exception ignored) {
                    return;
                }
                timer = delay.getInt();
                return;
            }
        }
    }
}
