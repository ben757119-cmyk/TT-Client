package com.ttclient.modules.player;

import com.ttclient.modules.Category;
import com.ttclient.modules.Module;
import com.ttclient.settings.BoolSetting;

public class AutoFish extends Module {
    public final BoolSetting openWater = addSetting(new BoolSetting("Open Water", "Only open water", false));

    public AutoFish() {
        super("AutoFish", "Automatically fish", Category.PLAYER);
    }
}
