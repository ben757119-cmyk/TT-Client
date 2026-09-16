package com.ttclient.modules.player;

import com.ttclient.modules.Category;
import com.ttclient.modules.Module;
import net.minecraft.client.Minecraft;

/**
 * Resets right-click delay each tick so blocks/items place faster.
 * Uses reflection-free public field access where available; no-ops safely if mapping differs.
 */
public class FastPlace extends Module {
    public FastPlace() {
        super("FastPlace", "Place blocks faster", Category.PLAYER);
    }

    @Override
    public void onTick() {
        Minecraft mc = mc();
        if (mc == null) return;
        try {
            // 26.2: rightClickDelay is on Minecraft in many mappings
            var field = Minecraft.class.getDeclaredField("rightClickDelay");
            field.setAccessible(true);
            field.setInt(mc, 0);
        } catch (Exception ignored) {
            try {
                var field = Minecraft.class.getDeclaredField("rightClickDelayCounter");
                field.setAccessible(true);
                field.setInt(mc, 0);
            } catch (Exception ignored2) {}
        }
    }
}
