package com.ttclient.modules.misc;

import com.ttclient.modules.Category;
import com.ttclient.modules.Module;
import com.ttclient.settings.NumberSetting;
import net.minecraft.client.Minecraft;

public class Spammer extends Module {
    public final NumberSetting delay = addSetting(new NumberSetting("Delay", "Seconds between messages", 5, 1, 30, 1));
    private int timer;

    public Spammer() {
        super("Spammer", "Send a chat message on an interval", Category.MISC);
    }

    @Override
    public void onTick() {
        Minecraft mc = mc();
        if (mc == null || mc.player == null) return;
        if (timer++ < delay.getInt() * 20) return;
        timer = 0;
        mc.player.connection.sendChat("TT Client");
    }
}
