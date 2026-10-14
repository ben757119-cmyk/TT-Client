package com.ttclient.modules.misc;

import com.ttclient.client.NotificationManager;
import com.ttclient.modules.Category;
import com.ttclient.modules.Module;
import net.minecraft.client.Minecraft;

public class DeathCoords extends Module {
    private boolean wasDead;

    public DeathCoords() {
        super("DeathCoords", "Remember death coordinates", Category.MISC);
        setEnabled(true);
    }

    @Override
    public void onTick() {
        Minecraft mc = mc();
        if (mc == null || mc.player == null) return;
        boolean dead = !mc.player.isAlive() || mc.player.getHealth() <= 0;
        if (dead && !wasDead) {
            String text = String.format("Death: %.0f %.0f %.0f",
                    mc.player.getX(), mc.player.getY(), mc.player.getZ());
            NotificationManager.push("DeathCoords", text);
            try {
                mc.keyboardHandler.setClipboard(text);
            } catch (Throwable ignored) {}
        }
        wasDead = dead;
    }
}
