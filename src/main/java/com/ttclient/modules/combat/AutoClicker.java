package com.ttclient.modules.combat;

import com.ttclient.modules.Category;
import com.ttclient.modules.Module;
import com.ttclient.settings.NumberSetting;
import com.ttclient.settings.BoolSetting;

public class AutoClicker extends Module {
    public final NumberSetting cps = addSetting(new NumberSetting("CPS", "Clicks per second", 12, 1, 20, 1));
    public final BoolSetting onlyWeapon = addSetting(new BoolSetting("Only Weapon", "Only when holding weapon", true));

    public AutoClicker() {
        super("AutoClicker", "Automatically click", Category.COMBAT);
    }
}
