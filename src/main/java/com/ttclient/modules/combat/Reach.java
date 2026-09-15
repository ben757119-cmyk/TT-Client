package com.ttclient.modules.combat;

import com.ttclient.modules.Category;
import com.ttclient.modules.Module;
import com.ttclient.settings.NumberSetting;

public class Reach extends Module {
    public final NumberSetting reach = addSetting(new NumberSetting("Reach", "Attack reach", 3.2, 3.0, 6.0, 0.1));

    public Reach() {
        super("Reach", "Extend attack reach", Category.COMBAT);
    }
}
