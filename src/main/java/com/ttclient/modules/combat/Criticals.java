package com.ttclient.modules.combat;

import com.ttclient.modules.Category;
import com.ttclient.modules.Module;
import com.ttclient.settings.ModeSetting;

public class Criticals extends Module {
    public final ModeSetting mode = addSetting(new ModeSetting("Mode", "Criticals mode", "Packet", "Packet", "Jump", "MiniJump"));

    public Criticals() {
        super("Criticals", "Always land critical hits", Category.COMBAT);
    }
}
