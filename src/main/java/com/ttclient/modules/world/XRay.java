package com.ttclient.modules.world;

import com.ttclient.modules.Category;
import com.ttclient.modules.Module;
import com.ttclient.settings.BoolSetting;

public class XRay extends Module {
    public final BoolSetting diamonds = addSetting(new BoolSetting("Diamonds", "Show diamonds", true));
    public final BoolSetting ancientDebris = addSetting(new BoolSetting("Ancient Debris", "Show ancient debris", true));
    public final BoolSetting iron = addSetting(new BoolSetting("Iron", "Show iron", false));
    public final BoolSetting gold = addSetting(new BoolSetting("Gold", "Show gold", false));

    public XRay() {
        super("XRay", "See through blocks to find ores", Category.WORLD);
    }
}
