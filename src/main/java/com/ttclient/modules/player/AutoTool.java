package com.ttclient.modules.player;

import com.ttclient.modules.Category;
import com.ttclient.modules.Module;
import com.ttclient.settings.BoolSetting;

public class AutoTool extends Module {
    public final BoolSetting switchBack = addSetting(new BoolSetting("Switch Back", "Return to previous slot", true));

    public AutoTool() {
        super("AutoTool", "Automatically switch to best tool", Category.PLAYER);
    }
}
