package com.ttclient.modules.misc;

import com.ttclient.modules.Category;
import com.ttclient.modules.Module;
import net.minecraft.client.Minecraft;

public class Announcer extends Module {
    private int blocksBroken;
    private int timer;

    public Announcer() {
        super("Announcer", "Announce milestones in chat occasionally", Category.MISC);
    }

    @Override
    public void onTick() {
        Minecraft mc = mc();
        if (mc == null || mc.player == null || mc.options == null) return;
        if (mc.options.keyAttack.isDown()) {
            blocksBroken++;
        }
        if (timer++ < 200) return;
        timer = 0;
        if (blocksBroken >= 50) {
            mc.player.connection.sendChat("Broke " + blocksBroken + " blocks with TT Client");
            blocksBroken = 0;
        }
    }
}
