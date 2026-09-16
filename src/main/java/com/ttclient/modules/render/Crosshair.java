package com.ttclient.modules.render;

import com.ttclient.modules.Category;
import com.ttclient.modules.Module;
import com.ttclient.settings.BoolSetting;

public class Crosshair extends Module {
    public final BoolSetting dot = addSetting(new BoolSetting("Dot", "Center dot", true));

    public Crosshair() {
        super("Crosshair", "Custom crosshair overlay", Category.RENDER);
        setEnabled(true);
    }
}
