package com.ttclient.modules.misc;

import com.ttclient.modules.Category;
import com.ttclient.modules.Module;
import com.ttclient.settings.NumberSetting;

public class AutoReconnect extends Module {
    public final NumberSetting delay = addSetting(new NumberSetting("Delay", "Reconnect delay (s)", 5, 1, 30, 1));

    public AutoReconnect() {
        super("AutoReconnect", "Automatically reconnect to server", Category.MISC);
    }
}
