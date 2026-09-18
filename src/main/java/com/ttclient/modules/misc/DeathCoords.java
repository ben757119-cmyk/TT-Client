package com.ttclient.modules.misc;

import com.ttclient.modules.Category;
import com.ttclient.modules.Module;
import net.minecraft.network.chat.Component;

public class DeathCoords extends Module {
    private boolean announced;

    public DeathCoords() {
        super("DeathCoords", "Chat + clipboard your death coordinates", Category.MISC);
        setEnabled(true);
    }

    @Override
    public void onTick() {
        if (mc.player == null) return;
        if (mc.player.isDeadOrDying()) {
            if (!announced) {
                int x = (int) Math.floor(mc.player.getX());
                int y = (int) Math.floor(mc.player.getY());
                int z = (int) Math.floor(mc.player.getZ());
                String text = "Death: " + x + " " + y + " " + z;
                mc.player.displayClientMessage(Component.literal("§b[TT] §f" + text), false);
                mc.keyboardHandler.setClipboard(x + " " + y + " " + z);
                announced = true;
            }
        } else {
            announced = false;
        }
    }
}
