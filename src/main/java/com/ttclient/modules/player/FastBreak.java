package com.ttclient.modules.player;

import com.ttclient.modules.Category;
import com.ttclient.modules.Module;
import com.ttclient.settings.NumberSetting;

public class FastBreak extends Module {
    public final NumberSetting multiplier = addSetting(new NumberSetting("Multiplier", "Break speed mult", 1.2, 1.0, 3.0, 0.1));

    public FastBreak() {
        super("FastBreak", "Break blocks faster", Category.PLAYER);
    }
}
