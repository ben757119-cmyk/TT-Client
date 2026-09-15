package com.ttclient.modules.player;

import com.ttclient.modules.Category;
import com.ttclient.modules.Module;
import com.ttclient.settings.NumberSetting;

public class ChestStealer extends Module {
    public final NumberSetting delay = addSetting(new NumberSetting("Delay", "Steal delay (ticks)", 1, 0, 10, 1));

    public ChestStealer() {
        super("ChestStealer", "Steal items from chests", Category.PLAYER);
    }
}
