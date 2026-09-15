package com.ttclient.modules.movement;

import com.ttclient.modules.Category;
import com.ttclient.modules.Module;
import com.ttclient.settings.ModeSetting;

public class Jesus extends Module {
    public final ModeSetting mode = addSetting(new ModeSetting("Mode", "Jesus mode", "Solid", "Solid", "Bob", "Trampoline"));

    public Jesus() {
        super("Jesus", "Walk on water and lava", Category.MOVEMENT);
    }
}
