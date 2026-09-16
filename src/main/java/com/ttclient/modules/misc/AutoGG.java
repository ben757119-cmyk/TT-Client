package com.ttclient.modules.misc;

import com.ttclient.modules.Category;
import com.ttclient.modules.Module;
import net.minecraft.client.Minecraft;

public class AutoGG extends Module {
    private boolean wasDead;

    public AutoGG() {
        super("AutoGG", "Say gg when you die", Category.MISC);
    }

    @Override
    public void onTick() {
        Minecraft mc = mc();
        if (mc == null || mc.player == null) return;
        boolean dead = !mc.player.isAlive() || mc.player.getHealth() <= 0;
        if (dead && !wasDead) {
            mc.player.connection.sendChat("gg");
        }
        wasDead = dead;
    }
}
