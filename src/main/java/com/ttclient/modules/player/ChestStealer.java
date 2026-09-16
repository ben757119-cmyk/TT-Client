package com.ttclient.modules.player;

import com.ttclient.modules.Category;
import com.ttclient.modules.Module;
import com.ttclient.settings.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;

/**
 * Container loot helper. Full shift-click path depends on 26.2 menu click enum location;
 * for now detects non-empty chest slots and reports via onTick pacing.
 */
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
                // Prefer pickup-all via mouse click API if available at runtime
                try {
                    var method = mc.gameMode.getClass().getMethod(
                            "handleInventoryMouseClick",
                            int.class, int.class, int.class,
                            Class.forName("net.minecraft.world.inventory.ClickType"),
                            net.minecraft.world.entity.player.Player.class
                    );
                    Object quickMove = Enum.valueOf(
                            (Class<? extends Enum>) Class.forName("net.minecraft.world.inventory.ClickType"),
                            "QUICK_MOVE"
                    );
                    method.invoke(mc.gameMode, menu.containerId, i, 0, quickMove, mc.player);
                } catch (Exception e) {
                    // Mapping moved — leave item in place rather than crash
                    return;
                }
                timer = delay.getInt();
                return;
            }
        }
    }
}
