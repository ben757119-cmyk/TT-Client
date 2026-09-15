package com.ttclient.modules.misc;

import com.ttclient.modules.Category;
import com.ttclient.modules.Module;
import com.ttclient.settings.NumberSetting;

public class AntiAFK extends Module {
    public final NumberSetting delay = addSetting(new NumberSetting("Delay", "Action delay (s)", 30, 5, 120, 5));

    public AntiAFK() {
        super("AntiAFK", "Prevent being kicked for AFK", Category.MISC);
    }
}
