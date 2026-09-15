package com.ttclient.modules.combat;

import com.ttclient.modules.Category;
import com.ttclient.modules.Module;
import com.ttclient.settings.BoolSetting;

public class Surround extends Module {
    public final BoolSetting center = addSetting(new BoolSetting("Center", "Center on block", true));
    public final BoolSetting rotate = addSetting(new BoolSetting("Rotate", "Rotate when placing", true));

    public Surround() {
        super("Surround", "Place obsidian around yourself", Category.COMBAT);
    }
}
