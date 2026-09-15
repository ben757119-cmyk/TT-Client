package com.ttclient.modules.world;

import com.ttclient.modules.Category;
import com.ttclient.modules.Module;
import com.ttclient.settings.BoolSetting;
import com.ttclient.settings.NumberSetting;

public class Scaffold extends Module {
    public final BoolSetting tower = addSetting(new BoolSetting("Tower", "Fast tower", true));
    public final NumberSetting delay = addSetting(new NumberSetting("Delay", "Place delay", 0, 0, 5, 1));

    public Scaffold() {
        super("Scaffold", "Automatically place blocks under you", Category.WORLD);
    }
}
