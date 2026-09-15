package com.ttclient.modules.combat;

import com.ttclient.modules.Category;
import com.ttclient.modules.Module;
import com.ttclient.settings.NumberSetting;
import com.ttclient.settings.BoolSetting;

public class AimAssist extends Module {
    public final NumberSetting strength = addSetting(new NumberSetting("Strength", "Aim strength", 0.4, 0.05, 1.0, 0.05));
    public final NumberSetting range = addSetting(new NumberSetting("Range", "Assist range", 4.5, 2.0, 6.0, 0.1));
    public final BoolSetting playersOnly = addSetting(new BoolSetting("Players Only", "Only aim at players", true));

    public AimAssist() {
        super("AimAssist", "Gently assist aiming at targets", Category.COMBAT);
    }
}
