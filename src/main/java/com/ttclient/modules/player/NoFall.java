package com.ttclient.modules.player;

import com.ttclient.modules.Category;
import com.ttclient.modules.Module;
import com.ttclient.settings.ModeSetting;

public class NoFall extends Module {
    public final ModeSetting mode = addSetting(new ModeSetting("Mode", "NoFall mode", "Packet", "Packet", "Potion"));

    public NoFall() {
        super("NoFall", "Prevent fall damage", Category.PLAYER);
    }
}
