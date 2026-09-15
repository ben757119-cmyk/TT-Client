package com.ttclient.modules.misc;

import com.ttclient.modules.Category;
import com.ttclient.modules.Module;
import com.ttclient.settings.NumberSetting;

public class AutoRespawn extends Module {
    public final NumberSetting delay = addSetting(new NumberSetting("Delay", "Respawn delay (s)", 0.5, 0.0, 5.0, 0.5));

    public AutoRespawn() {
        super("AutoRespawn", "Automatically respawn", Category.MISC);
    }
}
