package com.ttclient.modules.misc;

import com.ttclient.modules.Category;
import com.ttclient.modules.Module;
import com.ttclient.settings.NumberSetting;

public class AntiAFK extends Module {
    public final NumberSetting delay = addSetting(new NumberSetting("Delay", "Jump interval in seconds", 30, 5, 120, 5));
    private int ticks;

    public AntiAFK() {
        super("AntiAFK", "Jump periodically so you are not kicked for AFK", Category.MISC);
    }

    @Override
    public void onEnable() {
        ticks = 0;
    }

    @Override
    public void onTick() {
        if (mc.player == null) return;
        ticks++;
        int interval = delay.getInt() * 20;
        if (ticks >= interval) {
            ticks = 0;
            if (mc.player.onGround()) {
                mc.player.jumpFromGround();
            }
        }
    }
}
