package com.ttclient.modules.combat;

import com.ttclient.modules.Category;
import com.ttclient.modules.Module;
import com.ttclient.settings.BoolSetting;
import com.ttclient.settings.NumberSetting;

public class AutoCrystal extends Module {
    public final NumberSetting range = addSetting(new NumberSetting("Range", "Crystal range", 4.5, 2.0, 6.0, 0.1));
    public final BoolSetting place = addSetting(new BoolSetting("Place", "Place crystals", true));
    public final BoolSetting breakCrystal = addSetting(new BoolSetting("Break", "Break crystals", true));

    public AutoCrystal() {
        super("AutoCrystal", "Automatically place and break crystals", Category.COMBAT);
    }
}
