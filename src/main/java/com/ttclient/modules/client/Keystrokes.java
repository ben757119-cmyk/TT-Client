package com.ttclient.modules.client;

import com.ttclient.modules.Category;
import com.ttclient.modules.Module;
import com.ttclient.settings.BoolSetting;

public class Keystrokes extends Module {
    public final BoolSetting mouse = addSetting(new BoolSetting("Mouse", "Show mouse buttons", true));

    public Keystrokes() {
        super("Keystrokes", "On-screen WASD / jump / mouse", Category.CLIENT);
        setEnabled(true);
    }
}
