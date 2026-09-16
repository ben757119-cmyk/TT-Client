package com.ttclient.modules.misc;

import com.ttclient.modules.Category;
import com.ttclient.modules.Module;
import com.ttclient.settings.NumberSetting;
import net.minecraft.client.Minecraft;

public class AntiAFK extends Module {
    public final NumberSetting interval = addSetting(new NumberSetting("Interval", "Seconds between jumps", 30, 5, 120, 5));
    private int ticks;

    public AntiAFK() {
        super("AntiAFK", "Jump periodically to avoid AFK kick", Category.MISC);
    }

    @Override
    public void onEnable() {
        ticks = 0;
    }

    @Override
    public void onTick() {
        Minecraft mc = mc();
        if (mc == null || mc.player == null) return;
        ticks++;
        if (ticks >= (int) (interval.get() * 20)) {
            ticks = 0;
            if (mc.player.onGround()) {
                mc.player.jumpFromGround();
            }
        }
    }
}
