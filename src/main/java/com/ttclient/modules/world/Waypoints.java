package com.ttclient.modules.world;

import com.ttclient.modules.Category;
import com.ttclient.modules.Module;
import com.ttclient.settings.BoolSetting;

public class Waypoints extends Module {
    public final BoolSetting death = addSetting(new BoolSetting("Death Point", "Save death position", true));

    public Waypoints() {
        super("Waypoints", "Save and show waypoints", Category.WORLD);
    }
}
