package com.ttclient.modules.player;

import com.ttclient.modules.Category;
import com.ttclient.modules.Module;
import com.ttclient.settings.NumberSetting;

public class AutoEat extends Module {
    public final NumberSetting hunger = addSetting(new NumberSetting("Hunger", "Eat below this hunger", 14, 1, 19, 1));

    public AutoEat() {
        super("AutoEat", "Automatically eat food", Category.PLAYER);
    }
}
