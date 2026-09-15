package com.ttclient.modules.player;

import com.ttclient.modules.Category;
import com.ttclient.modules.Module;
import com.ttclient.settings.NumberSetting;

public class FastPlace extends Module {
    public final NumberSetting delay = addSetting(new NumberSetting("Delay", "Place delay", 0, 0, 4, 1));

    public FastPlace() {
        super("FastPlace", "Place blocks faster", Category.PLAYER);
    }
}
